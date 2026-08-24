package com.lifelink.blood_donation.Entities;

import com.lifelink.blood_donation.Entities.Enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @ManyToOne
    @JoinColumn(name = "blood_request_id", nullable = false)
    private BloodRequest bloodRequest;

    // Nullable: kept for traceability, but a request could theoretically
    // generate a notification without a live assignment in edge cases.
    @ManyToOne
    @JoinColumn(name = "request_assign_id")
    private RequestAssign requestAssign;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String message;

    @Builder.Default
    @Column(nullable = false)
    private boolean isRead = false;
}
