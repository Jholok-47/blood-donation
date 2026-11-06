package com.lifelink.blood_donation.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * GOAL: one candidate's feature row sent to the Python model service.
 * Field names (JSON) must match ml-service/app/features.py exactly.*/


@Getter
@Builder
@AllArgsConstructor
public class MlCandidateFeatures {
    @JsonProperty("distance_km")         private final double distanceKm;
    @JsonProperty("coords_known")        private final int coordsKnown;
    @JsonProperty("completed_count")     private final long completedCount;
    @JsonProperty("let_down_count")      private final long letDownCount;
    @JsonProperty("avg_rating")          private final double avgRating;
    @JsonProperty("review_count")        private final long reviewCount;
    @JsonProperty("urgency")             private final int urgency;
    @JsonProperty("quantity")            private final int quantity;
    @JsonProperty("response_delay_min")  private final double responseDelayMin;
    @JsonProperty("exact_match")         private final int exactMatch;
    @JsonProperty("hour_of_day")         private final int hourOfDay;
    @JsonProperty("is_weekend")          private final int isWeekend;
}
