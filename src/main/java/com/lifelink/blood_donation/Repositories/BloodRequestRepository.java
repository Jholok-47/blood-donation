package com.lifelink.blood_donation.Repositories;

import com.lifelink.blood_donation.Entities.BloodRequest;
import com.lifelink.blood_donation.Entities.Enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BloodRequestRepository extends JpaRepository<BloodRequest, Long> {

    List<BloodRequest> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    List<BloodRequest> findByStatusOrderByCreatedAtAsc(RequestStatus status);

    List<BloodRequest> findAllByOrderByCreatedAtDesc();
}
