package com.lifelink.blood_donation.Services;

import com.lifelink.blood_donation.DTO.RegisterRequest;
import com.lifelink.blood_donation.Entities.Enums.Role;
import com.lifelink.blood_donation.Entities.User;
import com.lifelink.blood_donation.Exceptions.EmailAlreadyExistsException;
import com.lifelink.blood_donation.Repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void register(RegisterRequest req) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException("An account with this email already exists");
        }
        if (req.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("Self-registration as ADMIN is not permitted");
        }
        if (req.getRole() == Role.DONOR && req.getBloodGroup() == null) {
            throw new IllegalArgumentException("Blood group is required for donor registration");
        }

        User user = User.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .phone(req.getPhone())
                .role(req.getRole())
                .bloodGroup(req.getBloodGroup())
                .district(req.getDistrict())
                .available(req.getRole() == Role.DONOR)
                .verified(false) // admin verification handled in Module 3
                .build();

        userRepository.save(user);
    }
}
