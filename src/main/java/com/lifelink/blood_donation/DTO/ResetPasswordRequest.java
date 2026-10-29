package com.lifelink.blood_donation.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Step 2 of Module 14 — submitted from reset-password.html after the user
 * receives their PASSWORD_RESET OTP. email is carried as a hidden field
 * from step 1 rather than re-typed. Cross-field check (newPassword vs.
 * confirmPassword) is done in AuthService.resetPassword(), same pattern
 * as the password-mismatch check in registerLocal().
 */
@Getter
@Setter
public class ResetPasswordRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    private String email;

    @NotBlank(message = "Enter the code sent to your email")
    private String otpCode;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String newPassword;

    @NotBlank(message = "Please confirm your new password")
    private String confirmPassword;
}