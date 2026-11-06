package com.lifelink.blood_donation.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * GOAL: per-donor AI result shown on the Select-donors page. Computed on read, never persisted.
 * probability = model's chance (0..1) that this donor completes; finalScore = blended 0..100 rank score.*/


@Getter
@AllArgsConstructor
public class MlDonorScoreDto {
    private final double probability;
    private final double ruleScore;
    private final double finalScore;
}
