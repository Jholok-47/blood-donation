package com.lifelink.blood_donation.Utils;

import com.lifelink.blood_donation.Entities.User;
import com.lifelink.blood_donation.Entities.Enums.BloodGroup;
import com.lifelink.blood_donation.Entities.Enums.Role;
import org.springframework.data.jpa.domain.Specification;

public class DonorSpecifications {

    private DonorSpecifications() {}

    public static Specification<User> isDonor() {
        return (root, query, cb) -> cb.equal(root.get("role"), Role.DONOR);
    }

    public static Specification<User> isVerified() {
        return (root, query, cb) -> cb.isTrue(root.get("verified"));
    }

    public static Specification<User> isAvailable() {
        return (root, query, cb) -> cb.isTrue(root.get("available"));
    }

    public static Specification<User> hasBloodGroup(BloodGroup bloodGroup) {
        return (root, query, cb) -> bloodGroup == null
                ? cb.conjunction()
                : cb.equal(root.get("bloodGroup"), bloodGroup);
    }

    public static Specification<User> hasDistrict(String district) {
        return (root, query, cb) -> (district == null || district.isBlank())
                ? cb.conjunction()
                : cb.equal(cb.lower(root.get("district")), district.trim().toLowerCase());
    }
}
