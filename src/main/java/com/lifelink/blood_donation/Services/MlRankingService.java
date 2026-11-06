package com.lifelink.blood_donation.Services;

import com.lifelink.blood_donation.DTO.MlCandidateFeatures;
import com.lifelink.blood_donation.DTO.MlDonorScoreDto;
import com.lifelink.blood_donation.DTO.MlPredictionResponse;
import com.lifelink.blood_donation.DTO.MlRankingResult;
import com.lifelink.blood_donation.DTO.RankedDonorDto;
import com.lifelink.blood_donation.Entities.BloodRequest;
import com.lifelink.blood_donation.Entities.Enums.AssignEndReason;
import com.lifelink.blood_donation.Entities.Enums.AssignStatus;
import com.lifelink.blood_donation.Entities.Enums.UrgencyLevel;
import com.lifelink.blood_donation.Entities.RequestInterest;
import com.lifelink.blood_donation.Entities.User;
import com.lifelink.blood_donation.Repositories.RequestAssignRepository;
import com.lifelink.blood_donation.Repositories.RequestInterestRepository;
import com.lifelink.blood_donation.Repositories.ReviewRepository;
import com.lifelink.blood_donation.Utils.DistanceCalculator;
import com.lifelink.blood_donation.Utils.ScoreBlender;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * GOAL: AI layer on top of the rule-based ranking. Takes the already rule-ranked donors for a request,
 * builds each donor's feature row from real data, asks the model service for completion probabilities,
 * blends them with the rule score (ScoreBlender) and re-sorts. Computed on read, never persisted.
 * Falls back to the untouched rule-based order if the model is disabled/unreachable.*/


@Service
@RequiredArgsConstructor
public class MlRankingService {

    private static final double MISSING_DISTANCE_KM = 50.0;   // must match the training data
    private static final double NO_REVIEW_RATING = 4.0;       // must match the training data
    private static final long MAX_DELAY_MINUTES = 1440;

    private final MlRankingClient client;
    private final RequestAssignRepository requestAssignRepository;
    private final RequestInterestRepository requestInterestRepository;
    private final ReviewRepository reviewRepository;

    @Value("${lifelink.ml.weight:0.40}")
    private double mlWeight;

    @Transactional(readOnly = true)
    public MlRankingResult rank(BloodRequest bloodRequest, List<RankedDonorDto> ruleRanked) {
        if (ruleRanked.isEmpty() || !client.isEnabled()) {
            return fallback(ruleRanked);
        }

        LocalDateTime now = LocalDateTime.now();
        List<MlCandidateFeatures> features = new ArrayList<>();
        for (RankedDonorDto r : ruleRanked) {
            features.add(buildFeatures(bloodRequest, r, now));
        }

        Optional<MlPredictionResponse> prediction = client.predict(features);
        if (prediction.isEmpty()) {
            return fallback(ruleRanked);
        }

        List<Double> probabilities = prediction.get().getProbabilities();
        Map<Long, MlDonorScoreDto> scores = new HashMap<>();
        for (int i = 0; i < ruleRanked.size(); i++) {
            RankedDonorDto r = ruleRanked.get(i);
            double rule = r.getScore().getTotalScore();
            double p = probabilities.get(i);
            scores.put(r.getDonor().getId(),
                    new MlDonorScoreDto(p, rule, ScoreBlender.blend(rule, p, mlWeight)));
        }

        List<RankedDonorDto> sorted = new ArrayList<>(ruleRanked);
        sorted.sort(Comparator
                .comparingDouble((RankedDonorDto r) -> scores.get(r.getDonor().getId()).getFinalScore())
                .reversed()
                .thenComparing(RankedDonorDto::getRespondedAt, Comparator.nullsLast(Comparator.naturalOrder())));

        return new MlRankingResult(sorted, scores, true, prediction.get().getModelVersion());
    }

    private MlRankingResult fallback(List<RankedDonorDto> ruleRanked) {
        return new MlRankingResult(ruleRanked, Map.of(), false, null);
    }

    private MlCandidateFeatures buildFeatures(BloodRequest req, RankedDonorDto ranked, LocalDateTime now) {
        User donor = ranked.getDonor();
        User patient = req.getPatient();
        Long donorId = donor.getId();

        long completed = requestAssignRepository.countByDonorIdAndStatus(donorId, AssignStatus.COMPLETED);
        long letDown = requestAssignRepository.countByDonorIdAndStatus(donorId, AssignStatus.DECLINED)
                + requestAssignRepository.countByDonorIdAndEndReason(donorId, AssignEndReason.UNRESPONSIVE);

        Double avg = reviewRepository.findAverageRatingByDonorId(donorId);
        long reviewCount = reviewRepository.countByDonorId(donorId);

        boolean coordsKnown = donor.getLatitude() != null && donor.getLongitude() != null
                && patient.getLatitude() != null && patient.getLongitude() != null;
        double distance = coordsKnown
                ? DistanceCalculator.haversineDistanceKm(donor.getLatitude(), donor.getLongitude(),
                        patient.getLatitude(), patient.getLongitude())
                : MISSING_DISTANCE_KM;

        long delayMinutes = 0;
        Optional<RequestInterest> interest =
                requestInterestRepository.findByBloodRequestIdAndDonorId(req.getId(), donorId);
        if (interest.isPresent() && interest.get().getRespondedAt() != null
                && interest.get().getCreatedAt() != null) {
            delayMinutes = Math.max(0, Math.min(MAX_DELAY_MINUTES,
                    Duration.between(interest.get().getCreatedAt(), interest.get().getRespondedAt()).toMinutes()));
        }

        DayOfWeek day = now.getDayOfWeek();
        return MlCandidateFeatures.builder()
                .distanceKm(distance)
                .coordsKnown(coordsKnown ? 1 : 0)
                .completedCount(completed)
                .letDownCount(letDown)
                .avgRating(avg != null && reviewCount > 0 ? avg : NO_REVIEW_RATING)
                .reviewCount(reviewCount)
                .urgency(urgencyIndex(req.getUrgency()))
                .quantity(req.getQuantity())
                .responseDelayMin(delayMinutes)
                .exactMatch(donor.getBloodGroup() == req.getBloodGroup() ? 1 : 0)
                .hourOfDay(now.getHour())
                .isWeekend(day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY ? 1 : 0)
                .build();
    }

    private int urgencyIndex(UrgencyLevel level) {
        return switch (level) {
            case LOW -> 0;
            case MEDIUM -> 1;
            case HIGH -> 2;
            case CRITICAL -> 3;
        };
    }
}
