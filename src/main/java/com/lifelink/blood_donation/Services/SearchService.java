package com.lifelink.blood_donation.Services;

import com.lifelink.blood_donation.Entities.BloodRequest;
import com.lifelink.blood_donation.Entities.User;
import com.lifelink.blood_donation.Entities.Enums.BloodGroup;
import com.lifelink.blood_donation.Entities.Enums.RequestStatus;
import com.lifelink.blood_donation.Entities.Enums.UrgencyLevel;
import com.lifelink.blood_donation.Repositories.BloodRequestRepository;
import com.lifelink.blood_donation.Repositories.UserRepository;
import com.lifelink.blood_donation.Utils.BloodRequestSpecifications;
import com.lifelink.blood_donation.Utils.DonorSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final UserRepository userRepository;
    private final BloodRequestRepository bloodRequestRepository;

    public List<User> searchDonors(BloodGroup bloodGroup, String district) {
        var spec = DonorSpecifications.isDonor()
                .and(DonorSpecifications.isVerified())
                .and(DonorSpecifications.isAvailable())
                .and(DonorSpecifications.hasBloodGroup(bloodGroup))
                .and(DonorSpecifications.hasDistrict(district))
                .and(DonorSpecifications.hasNoActiveAssignment()); // NEW: excludes donors already committed elsewhere

        // isEligibleToDonate() is @Transient — same in-memory filter pattern as
        // RequestAssignService.getCompatibleDonors() (Module 6 precedent).
        return userRepository.findAll(spec).stream()
                .filter(User::isEligibleToDonate)
                .toList();
    }

    public List<BloodRequest> filterRequests(RequestStatus status, UrgencyLevel urgency) {
        var spec = BloodRequestSpecifications.hasStatus(status)
                .and(BloodRequestSpecifications.hasUrgency(urgency));

        return bloodRequestRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
