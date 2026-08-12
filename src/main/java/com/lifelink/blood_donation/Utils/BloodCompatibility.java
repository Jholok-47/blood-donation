package com.lifelink.blood_donation.Utils;

import com.lifelink.blood_donation.Entities.Enums.BloodGroup;

import java.util.List;
import java.util.Map;

public class BloodCompatibility {

    // ⚠️ Verify these constant names match your actual BloodGroup enum before compiling.
    private static final Map<BloodGroup, List<BloodGroup>> RECIPIENT_TO_DONORS = Map.of(
            BloodGroup.O_NEGATIVE,  List.of(BloodGroup.O_NEGATIVE),
            BloodGroup.O_POSITIVE,  List.of(BloodGroup.O_NEGATIVE, BloodGroup.O_POSITIVE),
            BloodGroup.A_NEGATIVE,  List.of(BloodGroup.O_NEGATIVE, BloodGroup.A_NEGATIVE),
            BloodGroup.A_POSITIVE,  List.of(BloodGroup.O_NEGATIVE, BloodGroup.O_POSITIVE, BloodGroup.A_NEGATIVE, BloodGroup.A_POSITIVE),
            BloodGroup.B_NEGATIVE,  List.of(BloodGroup.O_NEGATIVE, BloodGroup.B_NEGATIVE),
            BloodGroup.B_POSITIVE,  List.of(BloodGroup.O_NEGATIVE, BloodGroup.O_POSITIVE, BloodGroup.B_NEGATIVE, BloodGroup.B_POSITIVE),
            BloodGroup.AB_NEGATIVE, List.of(BloodGroup.O_NEGATIVE, BloodGroup.A_NEGATIVE, BloodGroup.B_NEGATIVE, BloodGroup.AB_NEGATIVE),
            BloodGroup.AB_POSITIVE, List.of(BloodGroup.values()) // universal recipient — accepts every group
    );

    private BloodCompatibility() {}

    public static List<BloodGroup> getCompatibleDonorGroups(BloodGroup recipientGroup) {
        return RECIPIENT_TO_DONORS.get(recipientGroup);
    }
}
