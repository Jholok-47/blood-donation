package com.lifelink.blood_donation.DTO;

import com.lifelink.blood_donation.Entities.Enums.BloodGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileUpdateRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9+\\-\\s]{7,15}$", message = "Enter a valid phone number")
    private String phone;

    // Required for PATIENT and DONOR; admins don't use this form at all.
    @NotNull(message = "Blood group is required")
    private BloodGroup bloodGroup;

    @NotBlank(message = "District is required")
    private String district;

    // Optional — only populated once map integration (Module 10) is wired up.
    private Double latitude;
    private Double longitude;
}
