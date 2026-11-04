package com.lifelink.blood_donation.DTO;

import com.lifelink.blood_donation.Entities.Enums.BloodGroup;
import com.lifelink.blood_donation.Entities.Enums.UrgencyLevel;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BloodRequestCreateDto {

    @NotNull(message = "Blood group is required")
    private BloodGroup bloodGroup;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1 unit")
    @Max(value = 10, message = "Quantity must be at most 10 units")
    private Integer quantity;

    @NotNull(message = "Urgency level is required")
    private UrgencyLevel urgency;

    @NotBlank(message = "District is required")
    private String district;

    @Size(max = 500, message = "Description must be under 500 characters")
    private String description;
}
