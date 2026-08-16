package com.lifelink.blood_donation.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DonorScoreBreakdown {
    private double proximityScore;
    private double reliabilityScore;
    private double recencyScore;
    private double totalScore;
}
