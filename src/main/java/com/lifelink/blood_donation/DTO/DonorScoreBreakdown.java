package com.lifelink.blood_donation.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * REPLACES the old DonorScoreBreakdown.
 * Goal: the three rule-based score components (0-100) plus the weighted total. Computed on read only.
 * recencyScore is gone (it depended on last_donation_date); ratingScore replaces it.
 */
@Getter
@Builder
@AllArgsConstructor
public class DonorScoreBreakdown {
    private double proximityScore;
    private double reliabilityScore;
    private double ratingScore;
    private double totalScore;
}
