/*
package com.lifelink.blood_donation.DTO;

import com.lifelink.blood_donation.Entities.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RankedDonorDto {
    private User donor;
    private DonorScoreBreakdown score;
}*/

package com.lifelink.blood_donation.DTO;

import com.lifelink.blood_donation.Entities.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * REPLACES the old RankedDonorDto.
 * Goal: one row of the admin's selection table = an interested donor + score + when they accepted.
 */
@Getter
@Builder
@AllArgsConstructor
public class RankedDonorDto {
    private User donor;
    private DonorScoreBreakdown score;
    private LocalDateTime respondedAt;
}