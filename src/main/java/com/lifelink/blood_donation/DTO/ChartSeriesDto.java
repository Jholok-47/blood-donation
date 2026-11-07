package com.lifelink.blood_donation.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/** Parallel label/value lists for a Chart.js series. */
@Getter
@AllArgsConstructor
public class ChartSeriesDto {
    private final List<String> labels;
    private final List<Long> values;
}
