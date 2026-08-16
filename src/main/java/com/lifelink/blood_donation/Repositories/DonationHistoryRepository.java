package com.lifelink.blood_donation.Repositories;

import com.lifelink.blood_donation.Entities.DonationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DonationHistoryRepository extends JpaRepository<DonationHistory, Long> {

    // Powers the donor's "My Donations" history page
    List<DonationHistory> findByDonorIdOrderByDonationDateDesc(Long donorId);

    // Needed to count a donor's past completed donations for the reliability factor.
    long countByDonorId(Long donorId);
}
