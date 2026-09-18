package com.lifelink.blood_donation.DTO;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
public class OtpVerifyRequest {
    @NotBlank @Email
    private String email;

    @NotBlank @Size(min = 6, max = 6)
    private String otpCode;
}
