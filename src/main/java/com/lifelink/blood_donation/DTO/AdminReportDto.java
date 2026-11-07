package com.lifelink.blood_donation.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/** All figures of the Reports page for one date range. Computed on read, never persisted. */
@Getter
@Builder
@AllArgsConstructor
public class AdminReportDto {
    private final LocalDate from;
    private final LocalDate to;

    // request outcomes (requests created in range)
    private final long requestsCreated;
    private final long requestsFulfilled;
    private final long requestsClosedEarly;     // subset of fulfilled: fewer donations than units
    private final long requestsRejected;
    private final long requestsCancelled;
    private final long requestsStillOpen;       // PENDING + APPROVED + ASSIGNED
    private final double fulfilmentRate;        // fulfilled / (created - rejected), 0-100
    private final long unitsRequested;          // excludes rejected
    private final Double avgHoursToFulfil;      // null when nothing fulfilled

    // donations (donation date in range)
    private final long donations;
    private final ChartSeriesDto donationsOverTime;
    private final ChartSeriesDto donationsByBloodGroup;
    private final ChartSeriesDto donationsByDistrict;
    private final boolean dailyBuckets;
    private final List<SupplyDemandRowDto> supplyDemand;

    // assignment outcomes (assigned in range)
    private final long assignCompleted;
    private final long assignActive;
    private final long assignWithdrew;
    private final long assignUnresponsive;
    private final long assignAdminCancelled;
    private final double assignCompletionRate;  // completed / (completed + withdrew + unresponsive), 0-100

    // donor responsiveness (open-call rows created in range)
    private final long callsSent;
    private final long callsAccepted;
    private final long callsDeclined;
    private final long callsNoAnswer;
    private final double callResponseRate;      // answered / sent, 0-100
    private final Double avgResponseMinutes;
    private final Double medianResponseMinutes;
}
