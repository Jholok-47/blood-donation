package com.lifelink.blood_donation.DTO;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
public class LocalRegisterRequest {

    @NotBlank
    private String fullName; // doubles as the "Username" field from the requirement

    @NotBlank @Email
    private String email;

    @NotBlank @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank
    private String confirmPassword;
}
