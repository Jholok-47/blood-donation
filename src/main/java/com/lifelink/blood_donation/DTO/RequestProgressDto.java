package com.lifelink.blood_donation.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Goal: how far one blood request is toward its unit target. Not persisted, computed on read.
 * required  = BloodRequest.quantity (units needed, one donor gives one unit)
 * active    = donors currently assigned and not yet donated
 * completed = donors who donated
 */
@Getter
@AllArgsConstructor
public class RequestProgressDto {
    private final int required;
    private final int active;
    private final int completed;

    /** Units already covered: donated + assigned. */
    public int getAssigned() {
        return active + completed;
    }

    /** Units still needing a donor. */
    public int getRemaining() {
        return Math.max(0, required - getAssigned());
    }

    public boolean isFullyAssigned() {
        return getRemaining() == 0;
    }
}

