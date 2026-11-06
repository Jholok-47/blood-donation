package com.lifelink.blood_donation.Utils;

/**
 * GOAL: pure static blend of the rule-based score (0..100, unchanged 0.40/0.35/0.25 weights)
 * with the model's completion probability (0..1).
 * final = (1 - mlWeight) * ruleScore + mlWeight * probability * 100*/


public final class ScoreBlender {

    private ScoreBlender() {
    }

    public static double blend(double ruleScore, double probability, double mlWeight) {
        double w = Math.max(0.0, Math.min(1.0, mlWeight));
        double p = Math.max(0.0, Math.min(1.0, probability));
        return (1.0 - w) * ruleScore + w * p * 100.0;
    }
}
