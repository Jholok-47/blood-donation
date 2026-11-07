package com.lifelink.blood_donation.DTO;

import com.lifelink.blood_donation.Entities.Enums.IssueSeverity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** One detected problem or inconsistency shown on the Issues page. Computed on read, never persisted. */
@Getter
@Builder
@AllArgsConstructor
public class SystemIssueDto {
    private final IssueSeverity severity;
    private final String category;   // "Data integrity", "Request flow", "Assignments", "Donors", "Accounts", "System"
    private final String title;
    private final String detail;     // human-readable, includes a few affected ids
    private final long count;
    private final String link;       // where the admin can act (nullable)
}
