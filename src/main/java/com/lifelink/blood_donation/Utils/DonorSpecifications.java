package com.lifelink.blood_donation.Utils;

import com.lifelink.blood_donation.Entities.Enums.AssignStatus;
import com.lifelink.blood_donation.Entities.RequestAssign;
import com.lifelink.blood_donation.Entities.User;
import com.lifelink.blood_donation.Entities.Enums.BloodGroup;
import com.lifelink.blood_donation.Entities.Enums.Role;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
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

    // Custom specification to filter donors with no active assignments
    public static Specification<User> hasNoActiveAssignment() {
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<RequestAssign> raRoot = subquery.from(RequestAssign.class);
            subquery.select(raRoot.get("donor").get("id"))
                    .where(cb.equal(raRoot.get("status"), AssignStatus.ACTIVE));
            return cb.not(root.get("id").in(subquery));
        };
    }
}
