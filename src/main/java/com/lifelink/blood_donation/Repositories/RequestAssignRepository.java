package com.lifelink.blood_donation.Repositories;

import com.lifelink.blood_donation.Entities.Enums.AssignStatus;
import com.lifelink.blood_donation.Entities.RequestAssign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RequestAssignRepository extends JpaRepository<RequestAssign, Long> {

    // Finds the current ACTIVE assignment for a request — used to enforce "one active assignment at a time"
    Optional<RequestAssign> findByBloodRequestIdAndStatus(Long bloodRequestId, AssignStatus status);

    // Donor's "my assignments" view, most recent first
    List<RequestAssign> findByDonorIdOrderByCreatedAtDesc(Long donorId);
}