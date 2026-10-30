package com.lifelink.blood_donation.Services;

import com.lifelink.blood_donation.DTO.*;
import com.lifelink.blood_donation.Entities.DonationHistory;
import com.lifelink.blood_donation.Entities.Review;
import com.lifelink.blood_donation.Exceptions.InvalidOperationException;
import com.lifelink.blood_donation.Exceptions.ResourceNotFoundException;
import com.lifelink.blood_donation.Repositories.DonationHistoryRepository;
import com.lifelink.blood_donation.Repositories.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final DonationHistoryRepository donationHistoryRepository;

    @Value("${lifelink.review.window-days:30}")
    private int reviewWindowDays;

    // ---------- Patient: submit ----------

    @Transactional
    public Review submitReview(Long patientId, ReviewCreateDto dto) {
        DonationHistory history = findOwnedDonation(patientId, dto.getDonationHistoryId());

        if (reviewRepository.existsByDonationHistoryId(history.getId())) {
            throw new InvalidOperationException("You have already reviewed this donation.");
        }
        if (!isWithinReviewWindow(history)) {
            throw new InvalidOperationException("The review period for this donation has ended.");
        }

        Review review = Review.builder()
                .donationHistory(history)
                .patient(history.getPatient())
                .donor(history.getDonor())
                .rating(dto.getRating())
                .comment(normalizeComment(dto.getComment()))
                .build();
        try {
            // saveAndFlush so a unique-constraint race surfaces here, not at commit
            return reviewRepository.saveAndFlush(review);
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate review blocked by DB constraint for donation {}", history.getId());
            throw new InvalidOperationException("You have already reviewed this donation.");
        }
    }

    // ---------- Patient: read ----------

    @Transactional(readOnly = true)
    public ReviewableDonationDto getReviewableDonation(Long patientId, Long donationHistoryId) {
        DonationHistory history = findOwnedDonation(patientId, donationHistoryId);
        Review review = reviewRepository.findByDonationHistoryId(donationHistoryId).orElse(null);
        return toDto(history, review);
    }

    /** Donations still awaiting a review and inside the window, newest first (drives the pop-up). */
    @Transactional(readOnly = true)
    public List<ReviewableDonationDto> getPendingReviews(Long patientId) {
        return loadStates(patientId).stream()
                .filter(ReviewableDonationDto::isCanReview)
                .toList();
    }

    /** bloodRequestId -> review state, for the "Review" column on My Requests. */
    @Transactional(readOnly = true)
    public Map<Long, ReviewableDonationDto> getReviewStateByRequestId(Long patientId) {
        return loadStates(patientId).stream()
                .collect(Collectors.toMap(
                        ReviewableDonationDto::getBloodRequestId,
                        Function.identity(),
                        (a, b) -> a));
    }

    // ---------- Donor: read ----------

    @Transactional(readOnly = true)
    public DonorRatingSummaryDto getDonorRatingSummary(Long donorId) {
        long count = reviewRepository.countByDonorId(donorId);
        if (count == 0) {
            return new DonorRatingSummaryDto(0.0, 0);
        }
        Double avg = reviewRepository.findAverageRatingByDonorId(donorId);
        double rounded = avg == null ? 0.0 : Math.round(avg * 10.0) / 10.0;
        return new DonorRatingSummaryDto(rounded, count);
    }

    /** donationHistoryId -> received review (anonymous), for the donor's donation history page. */
    @Transactional(readOnly = true)
    public Map<Long, DonorReviewDto> getReviewsByDonationIdForDonor(Long donorId) {
        return reviewRepository.findByDonorIdOrderByCreatedAtDesc(donorId).stream()
                .collect(Collectors.toMap(
                        r -> r.getDonationHistory().getId(),
                        r -> DonorReviewDto.builder()
                                .rating(r.getRating())
                                .comment(r.getComment())
                                .reviewedAt(r.getCreatedAt())
                                .build(),
                        (a, b) -> a,
                        LinkedHashMap::new));
    }

    // ---------- Helpers ----------

    private List<ReviewableDonationDto> loadStates(Long patientId) {
        List<DonationHistory> histories =
                donationHistoryRepository.findByPatientIdOrderByDonationDateDesc(patientId);
        if (histories.isEmpty()) {
            return List.of();
        }
        List<Long> ids = histories.stream().map(DonationHistory::getId).toList();
        Map<Long, Review> reviewsByHistoryId = reviewRepository.findByDonationHistoryIdIn(ids).stream()
                .collect(Collectors.toMap(r -> r.getDonationHistory().getId(), Function.identity(), (a, b) -> a));
        return histories.stream()
                .map(h -> toDto(h, reviewsByHistoryId.get(h.getId())))
                .toList();
    }

    private DonationHistory findOwnedDonation(Long patientId, Long donationHistoryId) {
        DonationHistory history = donationHistoryRepository.findById(donationHistoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation record not found"));
        // Same message for "not yours" and "doesn't exist": don't leak which IDs exist
        if (!history.getPatient().getId().equals(patientId)) {
            throw new ResourceNotFoundException("Donation record not found");
        }
        return history;
    }

    private boolean isWithinReviewWindow(DonationHistory history) {
        return !history.getDonationDate().plusDays(reviewWindowDays).isBefore(LocalDate.now());
    }

    private ReviewableDonationDto toDto(DonationHistory h, Review review) {
        boolean reviewed = review != null;
        return ReviewableDonationDto.builder()
                .donationHistoryId(h.getId())
                .bloodRequestId(h.getBloodRequest().getId())
                .donorName(h.getDonor().getFullName())
                .bloodGroup(h.getBloodRequest().getBloodGroup())
                .donationDate(h.getDonationDate())
                .reviewed(reviewed)
                .rating(reviewed ? review.getRating() : null)
                .comment(reviewed ? review.getComment() : null)
                .canReview(!reviewed && isWithinReviewWindow(h))
                .build();
    }

    private String normalizeComment(String comment) {
        if (comment == null) return null;
        String trimmed = comment.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}