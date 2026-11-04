package com.lifelink.blood_donation.Services;

import com.lifelink.blood_donation.DTO.RequestProgressDto;
import com.lifelink.blood_donation.Entities.BloodRequest;
import com.lifelink.blood_donation.Entities.Enums.AssignStatus;
import com.lifelink.blood_donation.Entities.Enums.RequestStatus;
import com.lifelink.blood_donation.Entities.RequestAssign;
import com.lifelink.blood_donation.Repositories.BloodRequestRepository;
import com.lifelink.blood_donation.Repositories.RequestAssignRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Goal: single owner of "how many units are covered" and of the request status that follows from it.
 *   APPROVED  = still needs donors (assigned + donated < quantity)
 *   ASSIGNED  = every unit has a donor, some not yet donated
 *   FULFILLED = every unit donated (or admin closed it early)
 * Used by RequestAssignService, DonationHistoryService and the list pages, so the rule lives in one place.
 * No new enum value is needed: partial progress is derived from RequestAssign rows.
 */
@Service
@RequiredArgsConstructor
public class RequestProgressService {

    private final RequestAssignRepository requestAssignRepository;
    private final BloodRequestRepository bloodRequestRepository;

    @Transactional(readOnly = true)
    public RequestProgressDto getProgress(BloodRequest bloodRequest) {
        int active = (int) requestAssignRepository
                .countByBloodRequestIdAndStatus(bloodRequest.getId(), AssignStatus.ACTIVE);
        int completed = (int) requestAssignRepository
                .countByBloodRequestIdAndStatus(bloodRequest.getId(), AssignStatus.COMPLETED);
        return new RequestProgressDto(bloodRequest.getQuantity(), active, completed);
    }

    /** Batched for list pages: one query for all requests on the page. */
    @Transactional(readOnly = true)
    public Map<Long, RequestProgressDto> getProgressByRequestId(Collection<BloodRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = requests.stream().map(BloodRequest::getId).toList();
        Map<Long, int[]> counts = new HashMap<>();   // [active, completed]
        for (RequestAssign a : requestAssignRepository.findByBloodRequestIdInAndStatusIn(
                ids, List.of(AssignStatus.ACTIVE, AssignStatus.COMPLETED))) {
            int[] c = counts.computeIfAbsent(a.getBloodRequest().getId(), k -> new int[2]);
            if (a.getStatus() == AssignStatus.ACTIVE) c[0]++; else c[1]++;
        }
        Map<Long, RequestProgressDto> result = new HashMap<>();
        for (BloodRequest r : requests) {
            int[] c = counts.getOrDefault(r.getId(), new int[2]);
            result.put(r.getId(), new RequestProgressDto(r.getQuantity(), c[0], c[1]));
        }
        return result;
    }

    /**
     * Recomputes APPROVED / ASSIGNED / FULFILLED from the assignments. Requests that are
     * PENDING, REJECTED, CANCELLED or already closed are never touched.
     */
    @Transactional
    public void refreshStatus(BloodRequest bloodRequest) {
        RequestStatus current = bloodRequest.getStatus();
        if (current != RequestStatus.APPROVED && current != RequestStatus.ASSIGNED) {
            return;
        }
        RequestProgressDto p = getProgress(bloodRequest);
        RequestStatus next;
        if (p.getCompleted() >= p.getRequired()) {
            next = RequestStatus.FULFILLED;
        } else if (p.getAssigned() >= p.getRequired()) {
            next = RequestStatus.ASSIGNED;
        } else {
            next = RequestStatus.APPROVED;
        }
        if (next != current) {
            bloodRequest.setStatus(next);
            bloodRequestRepository.save(bloodRequest);
        }
    }
}
