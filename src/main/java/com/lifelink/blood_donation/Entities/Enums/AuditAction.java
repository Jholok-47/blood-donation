package com.lifelink.blood_donation.Entities.Enums;

/**
 * Kinds of sensitive admin actions that are written to the audit trail.
 * All values are declared up front on purpose: Hibernate puts a CHECK constraint on enum
 * columns and ddl-auto=update will not extend it later (see handoff §5 "Database/schema decisions").
 */
public enum AuditAction {
    USER_SUSPENDED,
    USER_REACTIVATED,
    DONOR_VERIFIED,
    DONOR_DELETED,
    REQUEST_APPROVED,
    REQUEST_REJECTED,
    REQUEST_CLOSED_EARLY,
    ASSIGNMENT_CANCELLED,
    ASSIGNMENT_RELEASED,
    REPORT_EXPORTED
}
