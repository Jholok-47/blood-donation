package com.lifelink.blood_donation.Entities;

import com.lifelink.blood_donation.Entities.Enums.OtpPurpose;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "otp_verifications")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class OtpVerification extends BaseEntity {

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String otpCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OtpPurpose purpose;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used = false;
}
