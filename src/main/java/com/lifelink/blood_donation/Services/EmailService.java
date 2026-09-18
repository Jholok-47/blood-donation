package com.lifelink.blood_donation.Services;

import com.lifelink.blood_donation.Entities.Enums.OtpPurpose;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendOtpEmail(String to, String code, OtpPurpose purpose) {
        String subject = purpose == OtpPurpose.REGISTRATION
                ? "LifeLink — Verify your email"
                : "LifeLink — Password reset code";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText("Your LifeLink verification code is: " + code
                + "\nThis code expires in a few minutes.");
        mailSender.send(message);
    }

    // Module 13 will add sendAssignmentEmail(...) / sendAcceptanceEmail(...) here,
    // called from RequestAssignService's existing three notification trigger points.
}
