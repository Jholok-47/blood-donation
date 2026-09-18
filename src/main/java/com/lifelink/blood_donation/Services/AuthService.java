package com.lifelink.blood_donation.Services;

import com.lifelink.blood_donation.DTO.LocalRegisterRequest;
import com.lifelink.blood_donation.DTO.OtpVerifyRequest;
import com.lifelink.blood_donation.Entities.Enums.AuthProvider;
import com.lifelink.blood_donation.Entities.Enums.OtpPurpose;
import com.lifelink.blood_donation.Entities.User;
import com.lifelink.blood_donation.Exceptions.EmailAlreadyExistsException;
import com.lifelink.blood_donation.Exceptions.InvalidOperationException;
import com.lifelink.blood_donation.Repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    @Transactional
    public User registerLocal(LocalRegisterRequest dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new InvalidOperationException("Passwords do not match");
        }
        userRepository.findByEmail(dto.getEmail()).ifPresent(u -> {
            throw new EmailAlreadyExistsException("An account with this email already exists");
        });

        User user = User.builder()
                .fullName(dto.getFullName())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .authProvider(AuthProvider.LOCAL)
                .profileCompleted(false)
                .emailVerified(false)
                .role(null)
                .build();
        user = userRepository.save(user);

        otpService.generateAndSendOtp(dto.getEmail(), OtpPurpose.REGISTRATION);
        return user;
    }

    @Transactional
    public void verifyRegistrationOtp(OtpVerifyRequest dto) {
        otpService.verifyOtp(dto.getEmail(), dto.getOtpCode(), OtpPurpose.REGISTRATION);

        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new InvalidOperationException("No account found for this email"));
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    public void resendOtp(String email) {
        userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidOperationException("No account found for this email"));
        otpService.generateAndSendOtp(email, OtpPurpose.REGISTRATION);
    }
}