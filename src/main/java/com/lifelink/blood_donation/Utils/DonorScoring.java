package com.lifelink.blood_donation.Utils;

import com.lifelink.blood_donation.Entities.BloodRequest;
import com.lifelink.blood_donation.Entities.User;
import com.lifelink.blood_donation.DTO.DonorScoreBreakdown;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class DonorScoring {

    private DonorScoring() {
        // pure static utility, no instances
    }

    // Weights sum to 1.0 — this is the single place the Module 8 formula is defined.
    private static final double PROXIMITY_WEIGHT = 0.40;
    private static final double RELIABILITY_WEIGHT = 0.35;
    private static final double RECENCY_WEIGHT = 0.25;

    // 90 days = the 3-month eligibility threshold already enforced by
    // User.isEligibleToDonate(); a donor never even reaches ranking unless
    // they've cleared it. 180 days is treated as "fully rested".
    private static final long RECENCY_MIN_DAYS = 90;
    private static final long RECENCY_MAX_DAYS = 180;

    private static final double RELIABILITY_BASELINE = 50.0;
    private static final double COMPLETED_DONATION_BONUS = 10.0;
    private static final double DECLINE_PENALTY = 20.0;

    public static DonorScoreBreakdown score(User donor, BloodRequest request,
                                            long completedDonationCount,
                                            long declineCount) {
        double proximity = calculateProximityScore(donor, request);
        double reliability = calculateReliabilityScore(completedDonationCount, declineCount);
        double recency = calculateRecencyScore(donor);

        double total = (proximity * PROXIMITY_WEIGHT)
                + (reliability * RELIABILITY_WEIGHT)
                + (recency * RECENCY_WEIGHT);

        return DonorScoreBreakdown.builder()
                .proximityScore(round(proximity))
                .reliabilityScore(round(reliability))
                .recencyScore(round(recency))
                .totalScore(round(total))
                .build();
    }

    // Placeholder: district-equality proximity check. lat/long exist on User
    // but real distance calculation is deferred to Module 10 (Google Maps
    // integration) — see Project State §9. Revisit once coordinates are
    // reliably populated for all users.
    private static double calculateProximityScore(User donor, BloodRequest request) {
        if (donor.getDistrict() != null && donor.getDistrict().equalsIgnoreCase(request.getDistrict())) {
            return 100.0;
        }
        return 40.0;
    }

    private static double calculateReliabilityScore(long completedDonationCount, long declineCount) {
        double score = RELIABILITY_BASELINE
                + (completedDonationCount * COMPLETED_DONATION_BONUS)
                - (declineCount * DECLINE_PENALTY);
        return clamp(score, 0.0, 100.0);
    }

    // Donors reaching this method already passed isEligibleToDonate(), so
    // lastDonationDate is either null (never donated) or >= RECENCY_MIN_DAYS
    // in the past. Score scales linearly across the rest window.
    private static double calculateRecencyScore(User donor) {
        if (donor.getLastDonationDate() == null) {
            return 100.0;
        }
        long daysSince = ChronoUnit.DAYS.between(donor.getLastDonationDate(), LocalDate.now());
        if (daysSince >= RECENCY_MAX_DAYS) {
            return 100.0;
        }
        if (daysSince <= RECENCY_MIN_DAYS) {
            return 0.0;
        }
        double fraction = (double) (daysSince - RECENCY_MIN_DAYS) / (RECENCY_MAX_DAYS - RECENCY_MIN_DAYS);
        return fraction * 100.0;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
