package com.lifelink.blood_donation.DTO;

import com.lifelink.blood_donation.Entities.Enums.BloodGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
public class ReviewableDonationDto {
    private Long donationHistoryId;
    private Long bloodRequestId;
    private String donorName;
    private BloodGroup bloodGroup;
    private LocalDate donationDate;
    private boolean reviewed;     // a review already exists
    private Integer rating;       // null unless reviewed
    private String comment;       // null unless reviewed
    private boolean canReview;    // not reviewed AND inside the review window
}
