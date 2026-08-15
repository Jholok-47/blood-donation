package com.lifelink.blood_donation.Repositories;

import com.lifelink.blood_donation.Entities.Enums.BloodGroup;
import com.lifelink.blood_donation.Entities.Enums.Role;
import com.lifelink.blood_donation.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);

    List<User> findByRoleAndVerifiedFalse(Role role);

    // Candidate donors for matching: role DONOR, verified, available, blood group in the compatible list
    List<User> findByRoleAndBloodGroupInAndVerifiedTrueAndAvailableTrue(Role role, List<BloodGroup> bloodGroups);
}
