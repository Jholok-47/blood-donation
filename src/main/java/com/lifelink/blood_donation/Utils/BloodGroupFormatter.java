package com.lifelink.blood_donation.Utils;

import com.lifelink.blood_donation.Entities.Enums.BloodGroup;

/** Goal: pure static helper turning A_POSITIVE into "A+" for emails and notification text. */
public final class BloodGroupFormatter {

    private BloodGroupFormatter() {
    }

    public static String display(BloodGroup group) {
        if (group == null) {
            return "-";
        }
        return group.name().replace("_POSITIVE", "+").replace("_NEGATIVE", "-");
    }
}