package com.lifelink.blood_donation.Utils;

import com.lifelink.blood_donation.DTO.DonorScoreBreakdown;
import com.lifelink.blood_donation.Entities.User;

/**
 * REPLACES the old DonorScoring.
 * Goal: pure static rule-based score (still NOT the ML model - that is Module 16).
 *   total = 0.40 proximity + 0.35 reliability + 0.25 rating
 * The old 0.25 "recency" component is gone because it needed last_donation_date; donor rating
 * (Module 15 reviews) takes its slot, so the other two weights are unchanged.
 *
 * If your old proximity code used different fallback numbers, copy them into proximity().
 */
public final class DonorScoring {

    public static final double W_PROXIMITY = 0.40;
    public static final double W_RELIABILITY = 0.35;
    public static final double W_RATING = 0.25;

    private static final double FULL_SCORE_KM = 5.0;
    private static final double ZERO_SCORE_KM = 100.0;
    private static final double SAME_DISTRICT_FALLBACK = 50.0;

    private static final double RELIABILITY_PRIOR = 2.5;   // first-pass tuning (unchanged from Module 9)

    private static final double RATING_PRIOR_MEAN = 4.0;   // new donors start at 4.0 stars...
    private static final double RATING_PRIOR_WEIGHT = 3.0; // ...worth three imaginary reviews

    private DonorScoring() {
    }

    /**
     * @param completed   donations this donor completed
     * @param letDown     assignments the donor withdrew from or went silent on (DECLINED + UNRESPONSIVE)
     * @param avgRating   average review rating, null when no reviews
     * @param reviewCount number of reviews
     */
    public static DonorScoreBreakdown score(User donor, User patient,
                                            long completed, long letDown,
                                            Double avgRating, long reviewCount) {
        double proximity = proximity(donor, patient);
        double reliability = reliability(completed, letDown);
        double rating = rating(avgRating, reviewCount);
        double total = W_PROXIMITY * proximity + W_RELIABILITY * reliability + W_RATING * rating;
        return DonorScoreBreakdown.builder()
                .proximityScore(proximity)
                .reliabilityScore(reliability)
                .ratingScore(rating)
                .totalScore(total)
                .build();
    }

    static double proximity(User donor, User patient) {
        Double dLat = donor.getLatitude();
        Double dLng = donor.getLongitude();
        Double pLat = patient.getLatitude();
        Double pLng = patient.getLongitude();
        if (dLat != null && dLng != null && pLat != null && pLng != null) {
            double km = DistanceCalculator.haversineDistanceKm(dLat, dLng, pLat, pLng);
            if (km <= FULL_SCORE_KM) return 100.0;
            if (km >= ZERO_SCORE_KM) return 0.0;
            return 100.0 * (ZERO_SCORE_KM - km) / (ZERO_SCORE_KM - FULL_SCORE_KM);
        }
        String a = donor.getDistrict();
        String b = patient.getDistrict();
        return (a != null && b != null && a.equalsIgnoreCase(b)) ? SAME_DISTRICT_FALLBACK : 0.0;
    }

    static double reliability(long completed, long letDown) {
        double v = ((completed + RELIABILITY_PRIOR) / (completed + letDown + 2 * RELIABILITY_PRIOR)) * 100.0;
        return clamp(v);
    }

    static double rating(Double avgRating, long reviewCount) {
        double avg = (avgRating == null || reviewCount == 0) ? 0.0 : avgRating;
        double smoothed = (avg * reviewCount + RATING_PRIOR_MEAN * RATING_PRIOR_WEIGHT)
                / (reviewCount + RATING_PRIOR_WEIGHT);
        return clamp(smoothed / 5.0 * 100.0);
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(100.0, v));
    }
}
