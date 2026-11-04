package com.lifelink.blood_donation.Entities.Enums;

/**
 * Goal: records WHY an assignment ended, so the three old "CANCELLED" causes are distinguishable.
 * DONOR_WITHDREW  - donor pressed "I can't make it"  (reliability penalty)
 * UNRESPONSIVE    - admin released a donor who never confirmed (reliability penalty)
 * ADMIN_CANCELLED - admin cancelled for their own reasons (NO penalty, same precedent as Module 8)
 */
public enum AssignEndReason {
    DONOR_WITHDREW, UNRESPONSIVE, ADMIN_CANCELLED
}

