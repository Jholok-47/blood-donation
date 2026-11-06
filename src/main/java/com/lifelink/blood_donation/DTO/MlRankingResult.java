package com.lifelink.blood_donation.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * GOAL: output of MlRankingService - the (re)ordered donor list plus the AI scores keyed by donor id.
 * When the model is unavailable: ranked == original order, scores is empty, modelAvailable == false.*/


@Getter
@AllArgsConstructor
public class MlRankingResult {
    private final List<RankedDonorDto> ranked;
    private final Map<Long, MlDonorScoreDto> scores;
    private final boolean modelAvailable;
    private final String modelVersion;
}
