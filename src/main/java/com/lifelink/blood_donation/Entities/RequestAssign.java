package com.lifelink.blood_donation.Entities;

import com.lifelink.blood_donation.Entities.Enums.AssignStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "request_assigns")
public class RequestAssign extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blood_request_id", nullable = false)
    private BloodRequest bloodRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donor_id", nullable = false)
    private User donor;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private AssignStatus status = AssignStatus.ACTIVE;

    private LocalDateTime assignedAt;

    private LocalDateTime respondedAt;  // when donor accepted/declined

    private LocalDateTime completedAt; // when donation actually happened

    // NOTE: "only one ACTIVE RequestAssign per BloodRequest at a time" is
    // enforced in the service layer (Module 5), not by a DB constraint,
    // since a request legitimately accumulates multiple historical rows
    // (declined/cancelled) over its lifetime.
}
