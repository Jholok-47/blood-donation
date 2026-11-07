package com.lifelink.blood_donation.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** Headline numbers for the admin Control Center. Computed on read. */
@Getter
@Builder
@AllArgsConstructor
public class AdminOverviewDto {
    // workload
    private final long pendingRequests;
    private final long openRequests;          // APPROVED: still needs donors
    private final long assignedRequests;      // ASSIGNED: waiting for donations
    private final long activeAssignments;
    private final long unconfirmedAssignments;
    // users
    private final long verifiedDonors;
    private final long availableDonors;       // verified + available + not suspended
    private final long unverifiedDonors;
    private final long patients;
    private final long suspendedUsers;
    // last 30 days
    private final long requests30d;
    private final long fulfilled30d;
    private final double fulfilmentRate30d;   // 0-100
    private final long donations30d;
    private final long donationsPrev30d;
    // health
    private final long criticalIssues;
    private final long warningIssues;
    private final boolean mlEnabled;
    private final boolean mlReachable;
    private final String mlModelVersion;
}
