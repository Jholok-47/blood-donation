package com.lifelink.blood_donation.Utils;

import com.lifelink.blood_donation.Entities.Enums.Role;
import com.lifelink.blood_donation.Entities.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/** Optional-filter Specification for the admin user list (name/email/phone text search + status filters). */
public final class UserAdminSpecifications {

    private UserAdminSpecifications() {
    }

    public static Specification<User> matches(String q, Role role, Boolean verified, Boolean active,
                                              boolean missingLocation, boolean incompleteProfile) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();

            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase()
                        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                p.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), like, '\\'),
                        cb.like(cb.lower(root.get("email")), like, '\\'),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("phone"), "")), like, '\\')));
            }
            if (role != null) p.add(cb.equal(root.get("role"), role));
            if (verified != null) p.add(cb.equal(root.get("verified"), verified));
            if (active != null) p.add(cb.equal(root.get("active"), active));
            if (missingLocation) {
                p.add(cb.equal(root.get("role"), Role.DONOR));
                p.add(cb.or(cb.isNull(root.get("latitude")), cb.isNull(root.get("longitude"))));
            }
            if (incompleteProfile) p.add(cb.isFalse(root.get("profileCompleted")));

            return cb.and(p.toArray(new Predicate[0]));
        };
    }
}
