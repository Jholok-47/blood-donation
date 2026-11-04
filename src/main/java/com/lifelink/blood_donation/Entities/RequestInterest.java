package com.lifelink.blood_donation.Entities;

import com.lifelink.blood_donation.Entities.Enums.InterestStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Goal: one row per (blood request, donor) that was sent the open call.
 * It is both the audit trail of "who was notified" and the admin's candidate pool
 * (rows with status INTERESTED). eligibilityAttested stores the donor's yes/no
 * self-declaration given when accepting (NOT a date).
 */
@Entity
@Table(name = "request_interests",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_request_interest_request_donor",
                columnNames = {"blood_request_id", "donor_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestInterest extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "blood_request_id", nullable = false)
    private BloodRequest bloodRequest;

    @ManyToOne(optional = false)
    @JoinColumn(name = "donor_id", nullable = false)
    private User donor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InterestStatus status;

    /** When the donor last answered (null while still NOTIFIED). */
    private LocalDateTime respondedAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean eligibilityAttested = false;
}
