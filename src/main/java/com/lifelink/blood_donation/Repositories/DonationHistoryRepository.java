package com.lifelink.blood_donation.Repositories;

import com.lifelink.blood_donation.Entities.DonationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DonationHistoryRepository extends JpaRepository<DonationHistory, Long> {
}
