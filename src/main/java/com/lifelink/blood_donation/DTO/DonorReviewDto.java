package com.lifelink.blood_donation.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class DonorReviewDto {
    private Integer rating;
    private String comment;
    private LocalDateTime reviewedAt;
}
