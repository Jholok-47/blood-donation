package com.lifelink.blood_donation.Entities;

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
@Table(name = "donation_histories")
public class DonationHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donor_id", nullable = false)
    private User donor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    // Link back to the request that this donation fulfilled
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blood_request_id", nullable = false)
    private BloodRequest bloodRequest;

    // Link to the specific assignment that led to this donation
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_assign_id", nullable = false, unique = true)
    private RequestAssign requestAssign;

    @Column(nullable = false)
    private LocalDate donationDate;

    private Integer quantity; // units donated
}
