package com.lifelink.blood_donation.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DonorRatingSummaryDto {
    private double averageRating; // 0.0 when there are no reviews, else one decimal
    private long reviewCount;
}