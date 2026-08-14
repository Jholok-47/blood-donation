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

    /**
     * Admin-triggered: marks the active assignment as COMPLETED, writes a DonationHistory
     * record, moves the request to FULFILLED, and updates the donor's lastDonationDate
     * so isEligibleToDonate() starts enforcing the 3-month rule.
     */
    @Transactional
    public void completeDonation(Long bloodRequestId) {
        BloodRequest request = bloodRequestRepository.findById(bloodRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found"));

        if (request.getStatus() != RequestStatus.ASSIGNED) {
            throw new InvalidOperationException("Only ASSIGNED requests can be marked as completed");
        }

        RequestAssign activeAssign = requestAssignRepository
                .findByBloodRequestIdAndStatus(bloodRequestId, AssignStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("No active assignment found for this request"));

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        activeAssign.setStatus(AssignStatus.COMPLETED);
        activeAssign.setCompletedAt(now);
        requestAssignRepository.save(activeAssign);

        User donor = activeAssign.getDonor();

        DonationHistory history = DonationHistory.builder()
                .donor(donor)
                .patient(request.getPatient())
                .bloodRequest(request)
                .requestAssign(activeAssign)
                .donationDate(today)
                .quantity(request.getQuantity())
                .build();
        donationHistoryRepository.save(history);

        request.setStatus(RequestStatus.FULFILLED);
        bloodRequestRepository.save(request);

        donor.setLastDonationDate(today);
        userRepository.save(donor);
    }

    public List<DonationHistory> getMyDonationHistory(Long donorId) {
        return donationHistoryRepository.findByDonorIdOrderByDonationDateDesc(donorId);
    }
}
