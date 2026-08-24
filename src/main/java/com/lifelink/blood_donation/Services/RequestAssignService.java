package com.lifelink.blood_donation.Services;

import com.lifelink.blood_donation.DTO.DonorScoreBreakdown;
import com.lifelink.blood_donation.DTO.RankedDonorDto;
import com.lifelink.blood_donation.Entities.BloodRequest;
import com.lifelink.blood_donation.Entities.Enums.*;
import com.lifelink.blood_donation.Entities.RequestAssign;
import com.lifelink.blood_donation.Entities.User;
import com.lifelink.blood_donation.Exceptions.InvalidOperationException;
import com.lifelink.blood_donation.Exceptions.ResourceNotFoundException;
import com.lifelink.blood_donation.Repositories.BloodRequestRepository;
import com.lifelink.blood_donation.Repositories.DonationHistoryRepository;
import com.lifelink.blood_donation.Repositories.RequestAssignRepository;
import com.lifelink.blood_donation.Repositories.UserRepository;
import com.lifelink.blood_donation.Utils.BloodCompatibility;
import com.lifelink.blood_donation.Utils.DonorScoring;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RequestAssignService {

    private final DonationHistoryRepository donationHistoryRepository;
    private final RequestAssignRepository requestAssignRepository;
    private final BloodRequestRepository bloodRequestRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    // Verified, available donors whose blood group can legally donate to only one request's blood group.
    public List<User> getCompatibleDonors(Long bloodRequestId) {
        BloodRequest request = bloodRequestRepository.findById(bloodRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found"));

        List<BloodGroup> compatibleGroups = BloodCompatibility.getCompatibleDonorGroups(request.getBloodGroup());

        List<User> candidates = userRepository
                .findByRoleAndBloodGroupInAndVerifiedTrueAndAvailableTrue(Role.DONOR, compatibleGroups);

        // Donors already committed to another active assignment are not compatible right now
        Set<Long> busyDonorIds = requestAssignRepository.findByStatus(AssignStatus.ACTIVE).stream()
                .map(ra -> ra.getDonor().getId())
                .collect(Collectors.toSet());

        return candidates.stream()
                .filter(User::isEligibleToDonate)
                .filter(donor -> !busyDonorIds.contains(donor.getId()))
                .toList();
    }

    // First-time assignment: request must be APPROVED, no existing ACTIVE assignment. Moves request to ASSIGNED.
    @Transactional
    public void assignDonor(Long requestId, Long donorId) {
        BloodRequest request = bloodRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found"));

        if (request.getStatus() != RequestStatus.APPROVED) {
            throw new InvalidOperationException("Only APPROVED requests can be assigned a donor");
        }

        requestAssignRepository.findByBloodRequestIdAndStatus(requestId, AssignStatus.ACTIVE)
                .ifPresent(a -> { throw new InvalidOperationException("This request already has an active assignment"); });

        User donor = userRepository.findById(donorId)
                .orElseThrow(() -> new ResourceNotFoundException("Donor not found"));

        if (donor.getRole() != Role.DONOR) {
            throw new InvalidOperationException("Selected user is not a donor");
        }

        RequestAssign assign = RequestAssign.builder()
                .bloodRequest(request)
                .donor(donor)
                .status(AssignStatus.ACTIVE)
                .assignedAt(LocalDateTime.now())
                .build();
        requestAssignRepository.save(assign);

        notificationService.notify(
                donor, request, assign, NotificationType.ASSIGNMENT,
                "You've been assigned to a " + request.getBloodGroup() +
                        " request in " + request.getDistrict() + ". Please accept or decline."
        );

        request.setStatus(RequestStatus.ASSIGNED);
        bloodRequestRepository.save(request);
    }

    // Reassignment: cancels the current ACTIVE assignment, creates a new one for a different donor.
    // Request stays ASSIGNED throughout — this is admin picking someone else, not a rejection of the request.
    @Transactional
    public void reassignDonor(Long requestId, Long newDonorId) {
        BloodRequest request = bloodRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found"));

        RequestAssign activeAssign = requestAssignRepository.findByBloodRequestIdAndStatus(requestId, AssignStatus.ACTIVE)
                .orElseThrow(() -> new InvalidOperationException("No active assignment to reassign"));

        activeAssign.setStatus(AssignStatus.CANCELLED);
        activeAssign.setRespondedAt(LocalDateTime.now());
        requestAssignRepository.save(activeAssign);

        User newDonor = userRepository.findById(newDonorId)
                .orElseThrow(() -> new ResourceNotFoundException("Donor not found"));

        RequestAssign newAssign = RequestAssign.builder()
                .bloodRequest(request)
                .donor(newDonor)
                .status(AssignStatus.ACTIVE)
                .assignedAt(LocalDateTime.now())
                .build();
        requestAssignRepository.save(newAssign);

        notificationService.notify(
                newDonor, request, newAssign, NotificationType.CASCADE_REASSIGNED,
                "You've been assigned to a " + request.getBloodGroup() +
                        " request in " + request.getDistrict() + " (reassigned by admin)."
        );
    }

    // Donor accepts — just records the response timestamp, stays ACTIVE until donation is completed (Module 6)
    @Transactional
    public void acceptAssignment(Long assignId, Long donorId) {
        RequestAssign assign = getOwnedActiveAssignment(assignId, donorId);
        assign.setRespondedAt(LocalDateTime.now());
        requestAssignRepository.save(assign);
    }

    // Donor declines — assignment becomes DECLINED, request reverts to APPROVED so admin can pick someone else
    @Transactional
    public void declineAssignment(Long assignId, Long donorId) {
        RequestAssign assign = getOwnedActiveAssignment(assignId, donorId);
        assign.setStatus(AssignStatus.DECLINED);
        assign.setRespondedAt(LocalDateTime.now());
        requestAssignRepository.save(assign);

        cascadeToNextDonor(assign.getBloodRequest());
    }

    // Admin can cancel an assignment.
    @Transactional
    public void cancelAssignment(Long requestId) {
        BloodRequest request = bloodRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found"));

        RequestAssign activeAssign = requestAssignRepository.findByBloodRequestIdAndStatus(requestId, AssignStatus.ACTIVE)
                .orElseThrow(() -> new InvalidOperationException("No active assignment to cancel"));

        activeAssign.setStatus(AssignStatus.CANCELLED);
        activeAssign.setRespondedAt(LocalDateTime.now());
        requestAssignRepository.save(activeAssign);

        request.setStatus(RequestStatus.APPROVED);
        bloodRequestRepository.save(request);
    }

    public List<RequestAssign> getMyAssignments(Long donorId) {
        return requestAssignRepository.findByDonorIdOrderByCreatedAtDesc(donorId);
    }

    private RequestAssign getOwnedActiveAssignment(Long assignId, Long donorId) {
        RequestAssign assign = requestAssignRepository.findById(assignId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));

        if (!assign.getDonor().getId().equals(donorId)) {
            throw new InvalidOperationException("This assignment does not belong to you");
        }
        if (assign.getStatus() != AssignStatus.ACTIVE) {
            throw new InvalidOperationException("Only ACTIVE assignments can be responded to");
        }
        return assign;
    }

    // Module 8: AI scoring of compatible donors. Returns a list of RankedDonorDto, sorted by total score descending.
    public List<RankedDonorDto> getRankedCompatibleDonors(Long bloodRequestId) {
        BloodRequest request = bloodRequestRepository.findById(bloodRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found"));

        List<User> compatibleDonors = getCompatibleDonors(bloodRequestId); // unchanged base filter

        return compatibleDonors.stream()
                .map(donor -> {
                    long completed = donationHistoryRepository.countByDonorId(donor.getId());
                    long declined = requestAssignRepository.countByDonorIdAndStatus(donor.getId(), AssignStatus.DECLINED);
                    DonorScoreBreakdown score = DonorScoring.score(donor, request, completed, declined);
                    return new RankedDonorDto(donor, score);
                })
                .sorted(Comparator.comparingDouble((RankedDonorDto d) -> d.getScore().getTotalScore()).reversed())
                .toList();
    }

    // Module 9: Notification methods
    private void cascadeToNextDonor(BloodRequest bloodRequest) {
        boolean alreadyHasActive = requestAssignRepository
                .findByBloodRequestIdAndStatus(bloodRequest.getId(), AssignStatus.ACTIVE)
                .isPresent();
        if (alreadyHasActive) {
            return;
        }

        // Exclude donors who already declined OR were cancelled-out (e.g. via timeout)
        // on THIS request, so they aren't re-offered it in the same cascade chain.
        List<RequestAssign> exhausted = requestAssignRepository
                .findAllByBloodRequestIdAndStatusIn(
                        bloodRequest.getId(),
                        List.of(AssignStatus.DECLINED, AssignStatus.CANCELLED)
                );
        Set<Long> excludedDonorIds = exhausted.stream()
                .map(ra -> ra.getDonor().getId())
                .collect(Collectors.toSet());

        List<RankedDonorDto> candidates = getRankedCompatibleDonors(bloodRequest.getId()).stream()
                .filter(rd -> !excludedDonorIds.contains(rd.getDonor().getId()))
                .toList();

        if (candidates.isEmpty()) {
            bloodRequest.setStatus(RequestStatus.APPROVED);
            bloodRequestRepository.save(bloodRequest);
            return;
        }

        User nextDonor = candidates.get(0).getDonor();
        RequestAssign newAssign = RequestAssign.builder()
                .bloodRequest(bloodRequest)
                .donor(nextDonor)
                .status(AssignStatus.ACTIVE)
                .assignedAt(LocalDateTime.now())
                .build();
        requestAssignRepository.save(newAssign);

        bloodRequest.setStatus(RequestStatus.ASSIGNED);
        bloodRequestRepository.save(bloodRequest);

        notificationService.notify(
                nextDonor, bloodRequest, newAssign, NotificationType.CASCADE_REASSIGNED,
                "You've been auto-assigned to a " + bloodRequest.getBloodGroup() +
                        " request in " + bloodRequest.getDistrict() + " after the previous donor didn't respond."
        );
    }

    @Transactional
    public void cascadeStaleAssignments(int windowMinutes) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(windowMinutes);
        List<RequestAssign> stale = requestAssignRepository
                .findByStatusAndRespondedAtIsNullAndAssignedAtBefore(AssignStatus.ACTIVE, threshold);

        for (RequestAssign requestAssign : stale) {
            requestAssign.setStatus(AssignStatus.CANCELLED);
            requestAssign.setRespondedAt(LocalDateTime.now());
            requestAssignRepository.save(requestAssign);
            cascadeToNextDonor(requestAssign.getBloodRequest());
        }
    }

    // Module 9 follow-up: donor name to show in the admin requests table.
// Only ACTIVE or COMPLETED rows are "the" donor for a request — declined/cancelled
// rows are history, not the current occupant of this column.
    public Map<Long, String> getCurrentDonorNamesByRequestId(List<Long> requestIds) {
        if (requestIds.isEmpty()) {
            return Map.of();
        }
        List<RequestAssign> relevant = requestAssignRepository.findByBloodRequestIdInAndStatusIn(
                requestIds, List.of(AssignStatus.ACTIVE, AssignStatus.COMPLETED)
        );
        return relevant.stream()
                .collect(Collectors.toMap(
                        ra -> ra.getBloodRequest().getId(),
                        ra -> ra.getDonor().getFullName()
                ));
    }
}
