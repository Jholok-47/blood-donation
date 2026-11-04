package com.lifelink.blood_donation.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** Goal: counts shown on the assign-donor page ("12 notified, 3 accepted..."). Not persisted. */
@Getter
@Builder
@AllArgsConstructor
public class InterestSummaryDto {
    private long notified;              // everyone who received the call
    private long waiting;               // notified, no answer yet
    private long interested;            // said "I can donate"
    private long declined;
    private long interestedButUnusable; // accepted, but now busy / switched availability off
}
