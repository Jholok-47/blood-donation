package com.lifelink.blood_donation.Utils;

import com.lifelink.blood_donation.Entities.BloodRequest;
import com.lifelink.blood_donation.Entities.Enums.RequestStatus;
import com.lifelink.blood_donation.Entities.Enums.UrgencyLevel;
import org.springframework.data.jpa.domain.Specification;

public class BloodRequestSpecifications {

    private BloodRequestSpecifications() {}

    public static Specification<BloodRequest> hasStatus(RequestStatus status) {
        return (root, query, cb) -> status == null
                ? cb.conjunction()
                : cb.equal(root.get("status"), status);
    }

    public static Specification<BloodRequest> hasUrgency(UrgencyLevel urgency) {
        return (root, query, cb) -> urgency == null
                ? cb.conjunction()
                : cb.equal(root.get("urgency"), urgency);
    }
}
