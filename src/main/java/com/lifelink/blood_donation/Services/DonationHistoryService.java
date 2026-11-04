package com.lifelink.blood_donation.Services;

import com.lifelink.blood_donation.Entities.BloodRequest;
import com.lifelink.blood_donation.Entities.DonationHistory;
import com.lifelink.blood_donation.Entities.RequestAssign;
import com.lifelink.blood_donation.Entities.User;
import com.lifelink.blood_donation.Entities.Enums.AssignStatus;
import com.lifelink.blood_donation.Entities.Enums.RequestStatus;
import com.lifelink.blood_donation.Exceptions.InvalidOperationException;
import com.lifelink.blood_donation.Exceptions.ResourceNotFoundException;
import com.lifelink.blood_donation.Repositories.BloodRequestRepository;
import com.lifelink.blood_donation.Repositories.DonationHistoryRepository;
import com.lifelink.blood_donation.Repositories.RequestAssignRepository;
import com.lifelink.blood_donation.Repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DonationHistoryService {

    private final BloodRequestRepository bloodRequestRepository;
    private final RequestAssignRepository requestAssignRepository;
    private final DonationHistoryRepository donationHistoryRepository;
    private final UserRepository userRepository;
    private final RequestProgressService progressService;

    @Transactional
    public DonationHistory completeDonation(Long assignmentId) {
        RequestAssign assignment = requestAssignRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
        if (assignment.getStatus() != AssignStatus.ACTIVE) {
            throw new InvalidOperationException("Only an active assignment can be marked as donated.");
        }
        BloodRequest bloodRequest = assignment.getBloodRequest();
        LocalDateTime now = LocalDateTime.now();

        assignment.setStatus(AssignStatus.COMPLETED);
        assignment.setCompletedAt(now);
        if (assignment.getRespondedAt() == null) {
            assignment.setRespondedAt(now);                 // stops the "not confirmed" flag
        }
        requestAssignRepository.save(assignment);

        DonationHistory history = donationHistoryRepository.save(DonationHistory.builder()
                .donor(assignment.getDonor())
                .patient(bloodRequest.getPatient())
                .bloodRequest(bloodRequest)
                .requestAssign(assignment)
                .donationDate(LocalDate.now())
                .quantity(1)                                // one donor = one unit (VERIFY quantity type)
                .build());

        progressService.refreshStatus(bloodRequest);         // FULFILLED when the last unit is donated
        return history;
    }

    public List<DonationHistory> getMyDonationHistory(Long donorId) {
        return donationHistoryRepository.findByDonorIdOrderByDonationDateDesc(donorId);
    }
}
