package com.lifelink.blood_donation.Services;

import com.lifelink.blood_donation.Entities.Enums.OtpPurpose;
import com.lifelink.blood_donation.Entities.OtpVerification;
import com.lifelink.blood_donation.Exceptions.InvalidOperationException;
import com.lifelink.blood_donation.Repositories.OtpVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpVerificationRepository otpVerificationRepository;
    private final EmailService emailService;

    @Value("${lifelink.otp.expiry-minutes}")
    private int expiryMinutes;

    @Value("${lifelink.otp.length}")
    private int otpLength;

    public void generateAndSendOtp(String email, OtpPurpose purpose) {
        // Invalidate any prior live OTP for this (email, purpose) before issuing a new one —
        // keeps the Optional-returning lookup below safe.
        otpVerificationRepository.findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(email, purpose)
                .ifPresent(prev -> { prev.setUsed(true); otpVerificationRepository.save(prev); });

        String code = generateNumericCode();

        OtpVerification otp = OtpVerification.builder()
                .email(email)
                .otpCode(code)
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusMinutes(expiryMinutes))
                .used(false)
                .build();
        otpVerificationRepository.save(otp);

        emailService.sendOtpEmail(email, code, purpose);
    }

    public void verifyOtp(String email, String code, OtpPurpose purpose) {
        OtpVerification otp = otpVerificationRepository
                .findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(email, purpose)
                .orElseThrow(() -> new InvalidOperationException("No OTP requested for this email"));

        if (otp.isUsed() || otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidOperationException("OTP expired — please request a new one");
        }
        if (!otp.getOtpCode().equals(code)) {
            throw new InvalidOperationException("Incorrect OTP code");
        }

        otp.setUsed(true);
        otpVerificationRepository.save(otp);
    }

    private String generateNumericCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < otpLength; i++) sb.append(random.nextInt(10));
        return sb.toString();
    }
}