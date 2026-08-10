package com.lifelink.blood_donation.Repositories;

import com.lifelink.blood_donation.Entities.BloodRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BloodRequestRepository extends JpaRepository<BloodRequest, Long> {
}
