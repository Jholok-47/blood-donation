package com.lifelink.blood_donation.Services;

import com.lifelink.blood_donation.DTO.DonorRatingSummaryDto;
import com.lifelink.blood_donation.DTO.UserDetailDto;
import com.lifelink.blood_donation.Entities.BloodRequest;
import com.lifelink.blood_donation.Entities.Enums.*;
import com.lifelink.blood_donation.Entities.RequestAssign;
import com.lifelink.blood_donation.Entities.RequestInterest;
import com.lifelink.blood_donation.Entities.User;
import com.lifelink.blood_donation.Exceptions.InvalidOperationException;
import com.lifelink.blood_donation.Exceptions.ResourceNotFoundException;
import com.lifelink.blood_donation.Repositories.*;
import com.lifelink.blood_donation.Utils.UserAdminSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Admin user list/search, activity detail, and account suspension (with audit trail and safety guards). */
@Service
@RequiredArgsConstructor
public class UserManagementService {

    private static final Set<RequestStatus> OPEN_STATUSES =
            Set.of(RequestStatus.PENDING, RequestStatus.APPROVED, RequestStatus.ASSIGNED);

    private final UserRepository userRepository;
    private final RequestAssignRepository requestAssignRepository;
    private final DonationHistoryRepository donationHistoryRepository;
    private final BloodRequestRepository bloodRequestRepository;
    private final RequestInterestRepository requestInterestRepository;
    private final ReviewService reviewService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<User> search(String q, Role role, Boolean verified, Boolean active,
                             boolean missingLocation, boolean incompleteProfile, int page, int size) {
        return userRepository.findAll(
                UserAdminSpecifications.matches(q, role, verified, active, missingLocation, incompleteProfile),
                PageRequest.of(Math.max(page, 0), size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Transactional(readOnly = true)
    public UserDetailDto getDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        UserDetailDto.UserDetailDtoBuilder b = UserDetailDto.builder().user(user);
        Map<RequestStatus, Long> byStatus = new EnumMap<>(RequestStatus.class);
        b.requestsByStatus(byStatus).recentAssignments(List.of()).recentRequests(List.of());

        if (user.getRole() == Role.DONOR) {
            Long id = user.getId();
            List<RequestAssign> assignments = requestAssignRepository.findByDonorIdOrderByCreatedAtDesc(id);
            DonorRatingSummaryDto rating = reviewService.getDonorRatingSummary(id);
            List<RequestInterest> calls = requestInterestRepository.findByDonorIdOrderByCreatedAtDesc(id);

            b.assignmentsActive(requestAssignRepository.countByDonorIdAndStatus(id, AssignStatus.ACTIVE))
                    .assignmentsCompleted(requestAssignRepository.countByDonorIdAndStatus(id, AssignStatus.COMPLETED))
                    .assignmentsWithdrew(requestAssignRepository.countByDonorIdAndEndReason(id, AssignEndReason.DONOR_WITHDREW))
                    .assignmentsUnresponsive(requestAssignRepository.countByDonorIdAndEndReason(id, AssignEndReason.UNRESPONSIVE))
                    .assignmentsAdminCancelled(requestAssignRepository.countByDonorIdAndEndReason(id, AssignEndReason.ADMIN_CANCELLED))
                    .donations(donationHistoryRepository.countByDonorId(id))
                    .averageRating(rating.getAverageRating())
                    .reviewCount(rating.getReviewCount())
                    .callsReceived(calls.size())
                    .callsAccepted(calls.stream().filter(c -> c.getStatus() == InterestStatus.INTERESTED
                            || c.getStatus() == InterestStatus.WITHDRAWN).count())
                    .callsDeclined(calls.stream().filter(c -> c.getStatus() == InterestStatus.DECLINED).count())
                    .callsNoAnswer(calls.stream().filter(c -> c.getStatus() == InterestStatus.NOTIFIED).count())
                    .recentAssignments(assignments.stream().limit(10).toList());
        }

        if (user.getRole() == Role.PATIENT) {
            List<BloodRequest> requests = bloodRequestRepository.findByPatientIdOrderByCreatedAtDesc(user.getId());
            for (BloodRequest r : requests) byStatus.merge(r.getStatus(), 1L, Long::sum);
            b.requestsTotal(requests.size()).requestsByStatus(byStatus)
                    .recentRequests(requests.stream().limit(10).toList());
        }
        return b.build();
    }

    @Transactional
    public void suspend(User admin, Long userId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new InvalidOperationException("A reason is required to suspend an account.");
        }
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        if (target.getId().equals(admin.getId())) {
            throw new InvalidOperationException("You cannot suspend your own account.");
        }
        if (target.getRole() == Role.ADMIN) {
            throw new InvalidOperationException("Admin accounts cannot be suspended here.");
        }
        if (!target.isActive()) {
            throw new InvalidOperationException("This account is already suspended.");
        }
        if (target.getRole() == Role.DONOR) {
            if (requestAssignRepository.countByDonorIdAndStatus(target.getId(), AssignStatus.ACTIVE) > 0) {
                throw new InvalidOperationException(
                        "This donor has an active assignment. Cancel or release it first, then suspend.");
            }
            target.setAvailable(false); // also removes the donor from every open call / assignment pool
        }
        if (target.getRole() == Role.PATIENT) {
            boolean hasOpen = bloodRequestRepository.findByPatientIdOrderByCreatedAtDesc(target.getId()).stream()
                    .anyMatch(r -> OPEN_STATUSES.contains(r.getStatus()));
            if (hasOpen) {
                throw new InvalidOperationException(
                        "This patient has an open request. Close or reject it first, then suspend.");
            }
        }
        target.setActive(false);
        userRepository.save(target);
        auditService.logAction(admin, AuditAction.USER_SUSPENDED, "USER", target.getId(),
                "Suspended " + target.getFullName() + " (" + target.getRole() + "). Reason: " + reason.trim());
    }

    @Transactional
    public void reactivate(User admin, Long userId) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        if (target.isActive()) {
            throw new InvalidOperationException("This account is not suspended.");
        }
        target.setActive(true);
        if (target.getRole() == Role.DONOR) {
            target.setAvailable(true); // suspension switched availability off; the toggle endpoint is unreliable (HTTP 400 bug)
        }
        userRepository.save(target);
        auditService.logAction(admin, AuditAction.USER_REACTIVATED, "USER", target.getId(),
                "Reactivated " + target.getFullName() + " (" + target.getRole() + ").");
    }
}
