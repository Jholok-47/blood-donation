package com.lifelink.blood_donation.Services;

import com.lifelink.blood_donation.Entities.BloodRequest;
import com.lifelink.blood_donation.Entities.RequestAssign;
import com.lifelink.blood_donation.Entities.User;
import com.lifelink.blood_donation.Entities.Enums.AssignStatus;
import com.lifelink.blood_donation.Entities.Enums.BloodGroup;
import com.lifelink.blood_donation.Entities.Enums.RequestStatus;
import com.lifelink.blood_donation.Entities.Enums.Role;
import com.lifelink.blood_donation.Exceptions.InvalidOperationException;
import com.lifelink.blood_donation.Exceptions.ResourceNotFoundException;
import com.lifelink.blood_donation.Repositories.BloodRequestRepository;
import com.lifelink.blood_donation.Repositories.RequestAssignRepository;
import com.lifelink.blood_donation.Repositories.UserRepository;
import com.lifelink.blood_donation.Utils.BloodCompatibility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RequestAssignService {

    private final RequestAssignRepository requestAssignRepository;
    private final BloodRequestRepository bloodRequestRepository;
    private final UserRepository userRepository;

    // Verified, available donors whose blood group can legally donate to this request's needed blood group
    public List<User> getCompatibleDonors(Long bloodRequestId) {
        BloodRequest request = bloodRequestRepository.findById(bloodRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found"));

        List<BloodGroup> compatibleGroups = BloodCompatibility.getCompatibleDonorGroups(request.getBloodGroup());

        List<User> candidates = userRepository
                .findByRoleAndBloodGroupInAndVerifiedTrueAndAvailableTrue(Role.DONOR, compatibleGroups);

        return candidates.stream()
                .filter(User::isEligibleToDonate)
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

        BloodRequest request = assign.getBloodRequest();
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
}
