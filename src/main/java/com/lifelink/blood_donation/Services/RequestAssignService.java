package com.lifelink.blood_donation.Services;

import com.lifelink.blood_donation.DTO.DonorMapMarkerDto;
import com.lifelink.blood_donation.DTO.DonorScoreBreakdown;
import com.lifelink.blood_donation.DTO.RankedDonorDto;
import com.lifelink.blood_donation.DTO.RequestProgressDto;
import com.lifelink.blood_donation.Entities.BloodRequest;
import com.lifelink.blood_donation.Entities.Enums.*;
import com.lifelink.blood_donation.Entities.RequestAssign;
import com.lifelink.blood_donation.Entities.RequestInterest;
import com.lifelink.blood_donation.Entities.User;
import com.lifelink.blood_donation.Exceptions.InvalidOperationException;
import com.lifelink.blood_donation.Exceptions.ResourceNotFoundException;
import com.lifelink.blood_donation.Repositories.*;
import com.lifelink.blood_donation.Utils.BloodCompatibility;
import com.lifelink.blood_donation.Utils.BloodGroupFormatter;
import com.lifelink.blood_donation.Utils.DonorScoring;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
    private final EmailService emailService;
    private final RequestInterestRepository interestRepository;
    private final DonorInterestService donorInterestService;
    private final RequestProgressService progressService;
    private final ReviewRepository reviewRepository;

    @Value("${lifelink.assignment.ack-window.critical-minutes:15}") private int ackCriticalMinutes;
    @Value("${lifelink.assignment.ack-window.high-minutes:30}")     private int ackHighMinutes;
    @Value("${lifelink.assignment.ack-window.medium-minutes:60}")   private int ackMediumMinutes;
    @Value("${lifelink.assignment.ack-window.low-minutes:180}")     private int ackLowMinutes;

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

    // used to feed the assign-donor page's map.
    public List<DonorMapMarkerDto> toDonorMapMarkers(List<RankedDonorDto> rankedDonors) {
        return rankedDonors.stream()
                .map(rd -> DonorMapMarkerDto.builder()
                        .donorId(rd.getDonor().getId())
                        .fullName(rd.getDonor().getFullName())
                        .bloodGroup(rd.getDonor().getBloodGroup().name())
                        .district(rd.getDonor().getDistrict())
                        .phone(rd.getDonor().getPhone())
                        .latitude(rd.getDonor().getLatitude())
                        .longitude(rd.getDonor().getLongitude())
                        .build())
                .toList();
    }


// ------------------------------------------------------------ ranking (donors who accepted only)
@Transactional(readOnly = true)
public List<RankedDonorDto> getRankedInterestedDonors(Long requestId) {
    BloodRequest bloodRequest = bloodRequestRepository.findById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException("Blood request not found"));
    User patient = bloodRequest.getPatient();

    return donorInterestService.getUsablePool(requestId).stream()
            .map(i -> {
                User donor = i.getDonor();
                long completed = requestAssignRepository.countByDonorIdAndStatus(donor.getId(), AssignStatus.COMPLETED);
                long letDown = requestAssignRepository.countByDonorIdAndStatus(donor.getId(), AssignStatus.DECLINED)
                        + requestAssignRepository.countByDonorIdAndEndReason(donor.getId(), AssignEndReason.UNRESPONSIVE);
                Double avg = reviewRepository.findAverageRatingByDonorId(donor.getId());
                long reviews = reviewRepository.countByDonorId(donor.getId());
                return RankedDonorDto.builder()
                        .donor(donor)
                        .score(DonorScoring.score(donor, patient, completed, letDown, avg, reviews))
                        .respondedAt(i.getRespondedAt())
                        .build();
            })
            // best score first; tie -> whoever accepted first
            .sorted(Comparator.comparingDouble((RankedDonorDto r) -> r.getScore().getTotalScore()).reversed()
                    .thenComparing(RankedDonorDto::getRespondedAt, Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();
}

/** ACTIVE + COMPLETED assignments of one request (one row per unit), oldest first. */
@Transactional(readOnly = true)
public List<RequestAssign> findAssignmentsForRequest(Long requestId) {
    return requestAssignRepository
            .findAllByBloodRequestIdAndStatusIn(requestId, List.of(AssignStatus.ACTIVE, AssignStatus.COMPLETED))
            .stream()
            .sorted(Comparator.comparing(RequestAssign::getAssignedAt, Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();
}

/** For the /admin/requests list: ACTIVE assignments grouped by request (batched, no per-row queries). */
@Transactional(readOnly = true)
public Map<Long, List<RequestAssign>> getActiveAssignmentsByRequestId(Collection<Long> requestIds) {
    if (requestIds == null || requestIds.isEmpty()) return Map.of();
    return requestAssignRepository.findByBloodRequestIdInAndStatusIn(requestIds, List.of(AssignStatus.ACTIVE)).stream()
            .collect(Collectors.groupingBy(a -> a.getBloodRequest().getId()));
}

public int ackWindowMinutes(UrgencyLevel urgency) {
    if (urgency == null) return ackMediumMinutes;
    return switch (urgency) {
        case CRITICAL -> ackCriticalMinutes;
        case HIGH -> ackHighMinutes;
        case MEDIUM -> ackMediumMinutes;
        case LOW -> ackLowMinutes;
    };
}

// ------------------------------------------------------------ admin selects one OR several donors who already accepted
/** All-or-nothing: if any donor fails validation the whole batch rolls back. @return number of donors assigned */
@Transactional
public int assignDonors(Long requestId, Collection<Long> donorIds) {
    if (donorIds == null || donorIds.isEmpty()) {
        throw new InvalidOperationException("Select at least one donor.");
    }
    Set<Long> ids = new LinkedHashSet<>(donorIds);

    BloodRequest bloodRequest = bloodRequestRepository.findById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException("Blood request not found"));
    if (bloodRequest.getStatus() != RequestStatus.APPROVED) {
        throw new InvalidOperationException("This request is not open for assignment.");
    }
    RequestProgressDto progress = progressService.getProgress(bloodRequest);
    if (ids.size() > progress.getRemaining()) {
        throw new InvalidOperationException("You selected " + ids.size() + " donor(s) but only "
                + progress.getRemaining() + " unit(s) are still needed.");
    }

    Set<Long> alreadyOnRequest = requestAssignRepository
            .findAllByBloodRequestIdAndStatusIn(requestId, List.of(AssignStatus.ACTIVE, AssignStatus.COMPLETED)).stream()
            .map(a -> a.getDonor().getId()).collect(Collectors.toSet());

    List<RequestAssign> created = new ArrayList<>();
    for (Long donorId : ids) {
        RequestInterest interest = interestRepository.findByBloodRequestIdAndDonorId(requestId, donorId)
                .filter(i -> i.getStatus() == InterestStatus.INTERESTED)
                .orElseThrow(() -> new InvalidOperationException("A selected donor has not accepted this request."));
        User donor = interest.getDonor();
        if (!donor.isVerified() || !donor.isAvailable()) {
            throw new InvalidOperationException(donor.getFullName() + " is no longer available.");
        }
        if (alreadyOnRequest.contains(donorId)) {
            throw new InvalidOperationException(donor.getFullName() + " is already assigned to this request.");
        }
        if (requestAssignRepository.countByDonorIdAndStatus(donorId, AssignStatus.ACTIVE) > 0) {
            throw new InvalidOperationException(donor.getFullName() + " is already assigned to another request.");
        }
        created.add(requestAssignRepository.save(RequestAssign.builder()
                .bloodRequest(bloodRequest)
                .donor(donor)
                .status(AssignStatus.ACTIVE)
                .assignedAt(LocalDateTime.now())
                .build()));
    }
    progressService.refreshStatus(bloodRequest);   // ASSIGNED once every unit has a donor

    String group = BloodGroupFormatter.display(bloodRequest.getBloodGroup());
    User patient = bloodRequest.getPatient();
    List<String> donorLines = new ArrayList<>();
    for (RequestAssign a : created) {
        User donor = a.getDonor();
        // in-app + email paired at the same call site (convention). NO second "accept" step: the donor already accepted.
        notificationService.notifySelected(a);
        emailService.sendSelectionEmail(donor.getEmail(), donor.getFullName(), group, bloodRequest.getDistrict(),
                patient.getFullName(), patient.getPhone(), String.valueOf(ackWindowMinutes(bloodRequest.getUrgency())));
        donorLines.add(donor.getFullName() + (donor.getPhone() == null ? "" : " - " + donor.getPhone()));
    }
    // ONE email to the patient for the whole batch
    emailService.sendDonorsAssignedEmail(patient.getEmail(), patient.getFullName(), group,
            String.valueOf(bloodRequest.getQuantity()), bloodRequest.getDistrict(), donorLines,
            progress.getAssigned() + created.size());
    return created.size();
}

// ------------------------------------------------------------ donor side
/** Liveness check, NOT an acceptance. */
@Transactional
public void confirmAssignment(Long assignmentId, Long donorId) {
    RequestAssign a = loadOwnedActive(assignmentId, donorId);
    if (a.getRespondedAt() == null) {
        a.setRespondedAt(LocalDateTime.now());
        requestAssignRepository.save(a);
    }
}

/** Donor pulls out after selection: penalised, that unit reopens, admin picks another donor. */
@Transactional
public void withdrawFromAssignment(Long assignmentId, Long donorId) {
    RequestAssign a = loadOwnedActive(assignmentId, donorId);
    endAssignment(a, AssignStatus.DECLINED, AssignEndReason.DONOR_WITHDREW, true);
}

// ------------------------------------------------------------ admin side
@Transactional
public void adminConfirmByPhone(Long assignmentId) {
    RequestAssign a = loadActive(assignmentId);
    if (a.getRespondedAt() == null) {
        a.setRespondedAt(LocalDateTime.now());
        requestAssignRepository.save(a);
    }
}

/** Admin gave up on a silent donor: penalised, dropped from this request's pool, that unit reopens. */
@Transactional
public void markUnresponsive(Long assignmentId) {
    endAssignment(loadActive(assignmentId), AssignStatus.CANCELLED, AssignEndReason.UNRESPONSIVE, true);
}

/** Admin's own decision (wrong pick, patient changed mind...): no penalty, donor stays in the pool. */
@Transactional
public void cancelAssignment(Long assignmentId) {
    endAssignment(loadActive(assignmentId), AssignStatus.CANCELLED, AssignEndReason.ADMIN_CANCELLED, false);
}

/**
 * Partly donated and the admin cannot / need not find more donors: release anyone still waiting (no penalty)
 * and mark the request FULFILLED with the units donated so far.
 */
@Transactional
public void closeRequestEarly(Long requestId) {
    BloodRequest bloodRequest = bloodRequestRepository.findById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException("Blood request not found"));
    if (bloodRequest.getStatus() != RequestStatus.APPROVED && bloodRequest.getStatus() != RequestStatus.ASSIGNED) {
        throw new InvalidOperationException("This request is already closed.");
    }
    if (progressService.getProgress(bloodRequest).getCompleted() < 1) {
        throw new InvalidOperationException("Nothing has been donated yet. Cancel the assignments instead.");
    }
    LocalDateTime now = LocalDateTime.now();
    for (RequestAssign a : requestAssignRepository
            .findAllByBloodRequestIdAndStatusIn(requestId, List.of(AssignStatus.ACTIVE))) {
        a.setStatus(AssignStatus.CANCELLED);
        a.setEndReason(AssignEndReason.ADMIN_CANCELLED);
        a.setRespondedAt(now);
        requestAssignRepository.save(a);
    }
    bloodRequest.setStatus(RequestStatus.FULFILLED);
    bloodRequestRepository.save(bloodRequest);
}

@Transactional(readOnly = true)
public Long getRequestIdOfAssignment(Long assignmentId) {
    return requestAssignRepository.findById(assignmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"))
            .getBloodRequest().getId();
}

// ------------------------------------------------------------ scheduler entry point
/** Flags (never reassigns) ACTIVE assignments whose donor has not confirmed within the urgency window. */
@Transactional
public int escalateStaleAssignments() {
    LocalDateTime now = LocalDateTime.now();
    int shortest = Math.min(Math.min(ackCriticalMinutes, ackHighMinutes), Math.min(ackMediumMinutes, ackLowMinutes));
    List<RequestAssign> candidates = requestAssignRepository
            .findByStatusAndRespondedAtIsNullAndAssignedAtBefore(AssignStatus.ACTIVE, now.minusMinutes(shortest));
    int flagged = 0;
    for (RequestAssign a : candidates) {
        if (a.getEscalatedAt() != null) continue;
        long window = ackWindowMinutes(a.getBloodRequest().getUrgency());
        if (a.getAssignedAt().plusMinutes(window).isAfter(now)) continue;
        a.setEscalatedAt(now);
        requestAssignRepository.save(a);
        flagged++;
    }
    return flagged;
}

// ------------------------------------------------------------ private helpers
private RequestAssign loadActive(Long assignmentId) {
    RequestAssign a = requestAssignRepository.findById(assignmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
    if (a.getStatus() != AssignStatus.ACTIVE) {
        throw new InvalidOperationException("This assignment is no longer active.");
    }
    return a;
}

private RequestAssign loadOwnedActive(Long assignmentId, Long donorId) {
    RequestAssign a = requestAssignRepository.findById(assignmentId)
            .filter(x -> x.getDonor().getId().equals(donorId))
            .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
    if (a.getStatus() != AssignStatus.ACTIVE) {
        throw new InvalidOperationException("This assignment is no longer active.");
    }
    return a;
}

private void endAssignment(RequestAssign a, AssignStatus status, AssignEndReason reason, boolean dropFromPool) {
    a.setStatus(status);
    a.setEndReason(reason);
    a.setRespondedAt(LocalDateTime.now());
    requestAssignRepository.save(a);

    BloodRequest bloodRequest = a.getBloodRequest();
    progressService.refreshStatus(bloodRequest);       // that unit is open again -> APPROVED

    if (dropFromPool) {
        interestRepository.findByBloodRequestIdAndDonorId(bloodRequest.getId(), a.getDonor().getId())
                .ifPresent(i -> { i.setStatus(InterestStatus.WITHDRAWN); interestRepository.save(i); });
    }
}
}
