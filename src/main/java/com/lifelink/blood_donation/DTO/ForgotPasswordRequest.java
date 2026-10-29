package com.lifelink.blood_donation.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Step 1 of Module 14 — the user only supplies the email they registered with.
 * AuthService resolves the User, guards against GOOGLE-only accounts, and
 * triggers OtpService.generateAndSendOtp(email, OtpPurpose.PASSWORD_RESET).
 */
@Getter
@Setter
public class ForgotPasswordRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    private String email;
}
