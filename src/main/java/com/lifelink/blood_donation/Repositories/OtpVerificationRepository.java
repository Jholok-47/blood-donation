package com.lifelink.blood_donation.Repositories;

import com.lifelink.blood_donation.Entities.Enums.OtpPurpose;
import com.lifelink.blood_donation.Entities.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {
    // Only ever one "live" OTP matters per (email, purpose) at lookup time —
    // Optional is safe here because OtpService always invalidates prior codes before issuing a new one.
    Optional<OtpVerification> findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(String email, OtpPurpose purpose);
}
