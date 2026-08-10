package com.lifelink.blood_donation.Repositories;

import com.lifelink.blood_donation.Entities.RequestAssign;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestAssignRepository extends JpaRepository<RequestAssign, Long> {
}