package com.lifelink.blood_donation.Services;

import com.lifelink.blood_donation.Entities.BloodRequest;
import com.lifelink.blood_donation.Entities.Enums.AssignStatus;
import com.lifelink.blood_donation.Entities.Enums.InterestStatus;
import com.lifelink.blood_donation.Entities.Enums.RequestStatus;
import com.lifelink.blood_donation.Repositories.BloodRequestRepository;
import com.lifelink.blood_donation.Repositories.RequestAssignRepository;
import com.lifelink.blood_donation.Repositories.RequestInterestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Goal: small read-only counters for the dashboard "needs attention" cards.
 * Kept separate so Module 17 (reports) can grow here without bloating other services.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final BloodRequestRepository bloodRequestRepository;
    private final RequestInterestRepository interestRepository;
    private final RequestAssignRepository requestAssignRepository;

    /** Requests waiting for the admin to approve/reject. */
    public long pendingRequestCount() {
        return bloodRequestRepository.findByStatusOrderByCreatedAtAsc(RequestStatus.PENDING).size();
    }

    /** Approved requests that already have at least one donor who said "I can donate". */
    public long awaitingSelectionCount() {
        List<Long> ids = bloodRequestRepository.findByStatusOrderByCreatedAtAsc(RequestStatus.APPROVED).stream()
                .map(BloodRequest::getId).toList();
        if (ids.isEmpty()) {
            return 0;
        }
        return interestRepository.findByBloodRequestIdInAndStatus(ids, InterestStatus.INTERESTED).stream()
                .map(i -> i.getBloodRequest().getId())
                .distinct()
                .count();
    }

    /** Assigned donors who did not confirm within the urgency window (escalated). */
    public long unconfirmedAssignmentCount() {
        return requestAssignRepository.countByStatusAndRespondedAtIsNullAndEscalatedAtIsNotNull(AssignStatus.ACTIVE);
    }

    /** Donor side: open calls not answered yet. */
    public long openRequestsForDonor(Long donorId) {
        return interestRepository.countAwaitingResponse(donorId);
    }
}

