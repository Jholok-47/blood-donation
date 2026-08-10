package com.lifelink.blood_donation.Entities.Enums;

public enum AssignStatus {
    ACTIVE,     // currently assigned, awaiting donor response/donation
    DECLINED,   // donor declined or didn't respond in time
    CANCELLED,  // admin manually removed this assignment
    COMPLETED   // donation happened, DonationHistory created
}
