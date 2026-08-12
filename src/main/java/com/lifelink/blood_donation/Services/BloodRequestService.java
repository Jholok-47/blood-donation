package com.lifelink.blood_donation.Services;

import com.lifelink.blood_donation.DTO.BloodRequestCreateDto;
import com.lifelink.blood_donation.Entities.BloodRequest;
import com.lifelink.blood_donation.Entities.Enums.RequestStatus;
import com.lifelink.blood_donation.Entities.User;
import com.lifelink.blood_donation.Exceptions.InvalidOperationException;
import com.lifelink.blood_donation.Exceptions.ResourceNotFoundException;
import com.lifelink.blood_donation.Repositories.BloodRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BloodRequestService {

    private final BloodRequestRepository bloodRequestRepository;

    public BloodRequest createRequest(User patient, BloodRequestCreateDto dto) {
        BloodRequest request = BloodRequest.builder()
                .patient(patient)
                .bloodGroup(dto.getBloodGroup())
                .quantity(dto.getQuantity())
                .urgency(dto.getUrgency())
                .district(dto.getDistrict())
                .description(dto.getDescription())
                .status(RequestStatus.PENDING)
                .build();
        return bloodRequestRepository.save(request);
    }

    public List<BloodRequest> getMyRequests(Long patientId) {
        return bloodRequestRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    public List<BloodRequest> getAllRequests() {
        return bloodRequestRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<BloodRequest> getPendingRequests() {
        return bloodRequestRepository.findByStatusOrderByCreatedAtAsc(RequestStatus.PENDING);
    }

    public BloodRequest getRequestById(Long id) {
        return bloodRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found with id: " + id));
    }

    public void approveRequest(Long requestId) {
        BloodRequest request = getRequestById(requestId);
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new InvalidOperationException("Only PENDING requests can be approved. Current status: " + request.getStatus());
        }
        request.setStatus(RequestStatus.APPROVED);
        bloodRequestRepository.save(request);
    }

    public void rejectRequest(Long requestId) {
        BloodRequest request = getRequestById(requestId);
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new InvalidOperationException("Only PENDING requests can be rejected. Current status: " + request.getStatus());
        }
        request.setStatus(RequestStatus.REJECTED);
        bloodRequestRepository.save(request);
    }
}
