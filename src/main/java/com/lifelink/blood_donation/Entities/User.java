package com.lifelink.blood_donation.Entities;

import com.lifelink.blood_donation.Entities.Enums.BloodGroup;
import com.lifelink.blood_donation.Entities.Enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password; // BCrypt-hashed, handled in Module 2

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // Nullable because ADMIN doesn't have a blood group
    @Enumerated(EnumType.STRING)
    private BloodGroup bloodGroup;

    private String district;

    private Double latitude;
    private Double longitude;

    // Relevant for DONOR only: toggled by the donor themselves
    @Builder.Default
    private boolean available = true;

    // Admin verifies donor profile before it's publicly visible (func req #3)
    @Builder.Default
    private boolean verified = false;

    // Denormalized convenience field, updated when a DonationHistory row
    // is created for this donor. Used for the 3-month eligibility check.
    private LocalDate lastDonationDate;

    // Convenience method for Module 6's eligibility logic
    @Transient
    public boolean isEligibleToDonate() {
        if (lastDonationDate == null) return true;
        return lastDonationDate.plusMonths(3).isBefore(LocalDate.now())
                || lastDonationDate.plusMonths(3).isEqual(LocalDate.now());
    }
}
