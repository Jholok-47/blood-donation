package com.lifelink.blood_donation.DTO;

import com.lifelink.blood_donation.Entities.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RankedDonorDto {
    private User donor;
    private DonorScoreBreakdown score;
}