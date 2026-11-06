package com.lifelink.blood_donation.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * GOAL: deserialises the model service's /predict reply.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MlPredictionResponse {
    @JsonProperty("model_version")
    private String modelVersion;

    private List<Double> probabilities;
}
