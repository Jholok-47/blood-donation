package com.lifelink.blood_donation.Services;

import com.lifelink.blood_donation.DTO.ProfileUpdateRequest;
import com.lifelink.blood_donation.Entities.Enums.Role;
import com.lifelink.blood_donation.Entities.User;
import com.lifelink.blood_donation.Exceptions.InvalidOperationException;
import com.lifelink.blood_donation.Exceptions.ResourceNotFoundException;
import com.lifelink.blood_donation.Repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;

    public User getCurrentUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional
    public User updateProfile(Long userId, ProfileUpdateRequest dto) {
        User user = getCurrentUser(userId);

        user.setFullName(dto.getFullName());
        user.setPhone(dto.getPhone());
        user.setDistrict(dto.getDistrict());
        user.setLatitude(dto.getLatitude());
        user.setLongitude(dto.getLongitude());

        // Admins don't reach this method (no profile form for them in this module),
        // but guard anyway since bloodGroup must stay null for ADMIN.
        if (user.getRole() != Role.ADMIN) {
            user.setBloodGroup(dto.getBloodGroup());
        }

        return userRepository.save(user);
    }

    @Transactional
    public void toggleAvailability(Long userId) {
        User user = getCurrentUser(userId);

        if (user.getRole() != Role.DONOR) {
            throw new InvalidOperationException("Only donors have an availability status");
        }

        user.setAvailable(!user.isAvailable());
        userRepository.save(user);
    }

    public List<User> getUnverifiedDonors() {
        return userRepository.findByRoleAndVerifiedFalse(Role.DONOR);
    }

    @Transactional
    public void verifyDonor(Long donorId) {
        User donor = getCurrentUser(donorId);

        if (donor.getRole() != Role.DONOR) {
            throw new InvalidOperationException("Only donor accounts can be verified");
        }

        donor.setVerified(true);
        userRepository.save(donor);
    }
}
