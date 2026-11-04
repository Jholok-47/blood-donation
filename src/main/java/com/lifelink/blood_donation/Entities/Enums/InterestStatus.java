package com.lifelink.blood_donation.Entities.Enums;

/**
 * Goal: lifecycle of one donor's response to one broadcast blood request.
 * NOTIFIED   - donor was sent the open call, has not answered yet
 * INTERESTED - donor said "I can donate" (shows up in the admin's selection list)
 * DECLINED   - donor said "not available" (or withdrew before being selected)
 * WITHDRAWN  - donor was selected and then pulled out / went silent; never considered again for this request
 */
public enum InterestStatus {
    NOTIFIED, INTERESTED, DECLINED, WITHDRAWN
}