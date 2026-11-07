package com.lifelink.blood_donation.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** Per blood group: units requested vs units donated in the period vs donors currently able to respond. */
@Getter
@Builder
@AllArgsConstructor
public class SupplyDemandRowDto {
    private final String bloodGroup;
    private final long requestedUnits;
    private final long donatedUnits;
    private final long availableDonors;
    private final boolean shortage;   // demand exists and available donors < requested units
}
