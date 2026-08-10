package com.lifelink.blood_donation.Entities.Enums;

public enum RequestStatus {
    PENDING,     // just submitted by patient
    APPROVED,    // admin approved, awaiting donor assignment
    REJECTED,    // admin rejected
    ASSIGNED,    // donor currently assigned (active RequestAssign exists)
    FULFILLED,   // donation completed
    CANCELLED    // patient/admin cancelled
}
