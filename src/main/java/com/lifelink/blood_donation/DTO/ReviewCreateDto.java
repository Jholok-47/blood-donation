package com.lifelink.blood_donation.DTO;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewCreateDto {

    @NotNull(message = "Invalid donation record")
    private Long donationHistoryId;

    @NotNull(message = "Please select a star rating")
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private Integer rating;

    @Size(max = 1000, message = "Review must be at most 1000 characters")
    private String comment;
}