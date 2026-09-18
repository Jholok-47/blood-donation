package com.lifelink.blood_donation.DTO;

import com.lifelink.blood_donation.Entities.Enums.BloodGroup;
import com.lifelink.blood_donation.Entities.Enums.Role;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Getter @Setter
public class ProfileCompletionDto {

    @NotNull(message = "Please select a role")
    private Role role; // PATIENT or DONOR only — enforce in service, not just template

    @NotNull(message = "Please select your blood group")
    private BloodGroup bloodGroup;

    @NotBlank
    private String address;

    @NotBlank
    private String phone;

    @NotBlank
    private String district;

    @NotNull(message = "Please pick your location on the map")
    private Double latitude;

    @NotNull(message = "Please pick your location on the map")
    private Double longitude;

    private LocalDate lastDonationDate; // optional, donor-only
}
