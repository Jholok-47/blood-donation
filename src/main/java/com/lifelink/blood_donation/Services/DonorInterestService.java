package com.lifelink.blood_donation.Services;

import com.lifelink.blood_donation.DTO.InterestSummaryDto;
import com.lifelink.blood_donation.Entities.BloodRequest;
import com.lifelink.blood_donation.Entities.Enums.*;
import com.lifelink.blood_donation.Entities.RequestAssign;
import com.lifelink.blood_donation.Entities.RequestInterest;
import com.lifelink.blood_donation.Entities.User;
import com.lifelink.blood_donation.Exceptions.InvalidOperationException;
import com.lifelink.blood_donation.Exceptions.ResourceNotFoundException;
import com.lifelink.blood_donation.Repositories.BloodRequestRepository;
import com.lifelink.blood_donation.Repositories.RequestAssignRepository;
import com.lifelink.blood_donation.Repositories.RequestInterestRepository;
import com.lifelink.blood_donation.Repositories.UserRepository;
import com.lifelink.blood_donation.Utils.BloodCompatibility;
import com.lifelink.blood_donation.Utils.BloodGroupFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Goal: the "open call" half of the new flow.
 *  - broadcastForRequest: after admin approval, notify every eligible donor (in-app + email), once each
 *  - respond: donor says "I can donate" (with eligibility attestation) or "not available"
 *  - getUsablePool: the admin's candidate list = INTERESTED donors who are still verified, available and free
 * Multi-unit: a request needing N units keeps serving donors until N are covered.
 * Eligibility = verified + availability toggle ON + compatible blood group + no ACTIVE assignment.
 * There is deliberately NO donation-date check anywhere.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DonorInterestService {

    private final RequestInterestRepository interestRepository;
    private final BloodRequestRepository bloodRequestRepository;
    private final UserRepository userRepository;
    private final RequestAssignRepository requestAssignRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    // ---------------------------------------------------------------- broadcast

    @Transactional(readOnly = true)
    public List<User> findEligibleDonors(BloodRequest bloodRequest) {
        List<BloodGroup> groups = BloodCompatibility.getCompatibleDonorGroups(bloodRequest.getBloodGroup());
        Set<Long> busy = activeDonorIds();
        return userRepository
                .findByRoleAndBloodGroupInAndVerifiedTrueAndAvailableTrue(Role.DONOR, groups)
                .stream()
                .filter(d -> !busy.contains(d.getId()))
                .toList();
    }

    /**
     * Idempotent: donors who already have a row for this request are skipped, so the admin's
     * "Notify donors" button can be pressed again to reach donors who became eligible later.
     * @return number of donors newly notified
     */
    @Transactional
    public int broadcastForRequest(Long requestId) {
        BloodRequest bloodRequest = bloodRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found"));
        if (bloodRequest.getStatus() != RequestStatus.APPROVED) {
            throw new InvalidOperationException("Only approved requests can be sent to donors.");
        }

        Set<Long> alreadyContacted = interestRepository.findByBloodRequestId(requestId).stream()
                .map(i -> i.getDonor().getId())
                .collect(Collectors.toSet());

        int notified = 0;
        for (User donor : findEligibleDonors(bloodRequest)) {
            if (alreadyContacted.contains(donor.getId())) {
                continue;
            }
            interestRepository.save(RequestInterest.builder()
                    .bloodRequest(bloodRequest)
                    .donor(donor)
                    .status(InterestStatus.NOTIFIED)
                    .build());
            // in-app + email paired at the same call site (project convention)
            notificationService.notifyBroadcast(donor, bloodRequest);
            emailService.sendBroadcastEmail(
                    donor.getEmail(),
                    donor.getFullName(),
                    BloodGroupFormatter.display(bloodRequest.getBloodGroup()),
                    String.valueOf(bloodRequest.getQuantity()),
                    String.valueOf(bloodRequest.getUrgency()),
                    bloodRequest.getDistrict(),
                    bloodRequest.getDescription());
            notified++;
        }
        log.info("Request {} broadcast to {} new donor(s)", requestId, notified);
        return notified;
    }

    // ---------------------------------------------------------------- donor response

    @Transactional
    public void respond(Long donorId, Long requestId, boolean interested, boolean attested) {
        RequestInterest interest = interestRepository.findByBloodRequestIdAndDonorId(requestId, donorId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        if (interest.getBloodRequest().getStatus() != RequestStatus.APPROVED) {
            throw new InvalidOperationException("This request is no longer open for responses.");
        }
        if (interest.getStatus() == InterestStatus.WITHDRAWN) {
            throw new InvalidOperationException("You can no longer respond to this request.");
        }
        // a request can now be partly assigned (still APPROVED): a selected/donated donor must use "My assignments"
        boolean alreadySelected = requestAssignRepository
                .findAllByBloodRequestIdAndStatusIn(requestId, List.of(AssignStatus.ACTIVE, AssignStatus.COMPLETED))
                .stream().anyMatch(x -> x.getDonor().getId().equals(donorId));
        if (alreadySelected) {
            throw new InvalidOperationException("You are already selected for this request. See My assignments.");
        }

        if (interested) {
            User donor = interest.getDonor();
            if (!donor.isAvailable()) {
                throw new InvalidOperationException("Switch your availability on in your profile before accepting.");
            }
            if (!attested) {
                throw new InvalidOperationException("Please tick the eligibility statement to accept this request.");
            }
            interest.setStatus(InterestStatus.INTERESTED);
            interest.setEligibilityAttested(true);
        } else {
            interest.setStatus(InterestStatus.DECLINED);
            interest.setEligibilityAttested(false);
        }
        interest.setRespondedAt(LocalDateTime.now());
        interestRepository.save(interest);
    }

    // ---------------------------------------------------------------- reads

    /** Admin candidate pool: accepted AND still usable right now (re-checked on every read). */
    @Transactional(readOnly = true)
    public List<RequestInterest> getUsablePool(Long requestId) {
        Set<Long> busy = new HashSet<>(activeDonorIds());
        // multi-unit requests: a donor who already donated for this request must not be offered again
        busy.addAll(requestAssignRepository
                .findAllByBloodRequestIdAndStatusIn(requestId, List.of(AssignStatus.COMPLETED)).stream()
                .map(x -> x.getDonor().getId()).collect(Collectors.toSet()));
        return interestRepository.findByBloodRequestIdAndStatus(requestId, InterestStatus.INTERESTED).stream()
                .filter(i -> {
                    User d = i.getDonor();
                    return d.isVerified() && d.isAvailable() && !busy.contains(d.getId());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public InterestSummaryDto getSummary(Long requestId) {
        List<RequestInterest> all = interestRepository.findByBloodRequestId(requestId);
        long interested = all.stream().filter(i -> i.getStatus() == InterestStatus.INTERESTED).count();
        long declined = all.stream().filter(i -> i.getStatus() == InterestStatus.DECLINED).count();
        long waiting = all.stream().filter(i -> i.getStatus() == InterestStatus.NOTIFIED).count();
        long usable = getUsablePool(requestId).size();
        return InterestSummaryDto.builder()
                .notified(all.size())
                .waiting(waiting)
                .interested(interested)
                .declined(declined)
                .interestedButUnusable(Math.max(0, interested - usable))
                .build();
    }

    @Transactional(readOnly = true)
    public List<RequestInterest> getMyInterests(Long donorId) {
        return interestRepository.findByDonorIdOrderByCreatedAtDesc(donorId);
    }

    /** Batched count of INTERESTED donors per request for the /admin/requests list. */
    @Transactional(readOnly = true)
    public Map<Long, Long> getInterestedCountByRequestId(Collection<Long> requestIds) {
        if (requestIds == null || requestIds.isEmpty()) {
            return Map.of();
        }
        return interestRepository.findByBloodRequestIdInAndStatus(requestIds, InterestStatus.INTERESTED).stream()
                .collect(Collectors.groupingBy(i -> i.getBloodRequest().getId(), Collectors.counting()));
    }

    private Set<Long> activeDonorIds() {
        return requestAssignRepository.findByStatus(AssignStatus.ACTIVE).stream()
                .map(RequestAssign::getDonor)
                .map(User::getId)
                .collect(Collectors.toSet());
    }
}
