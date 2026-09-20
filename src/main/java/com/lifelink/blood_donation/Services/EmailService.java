package com.lifelink.blood_donation.Services;

import com.lifelink.blood_donation.Entities.BloodRequest;
import com.lifelink.blood_donation.Entities.Enums.OtpPurpose;
import com.lifelink.blood_donation.Entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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

    public void sendAssignmentEmail(User donor, BloodRequest request) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(donor.getEmail());
            helper.setSubject("LifeLink – New Blood Donation Assignment");
            helper.setText(buildAssignmentEmailBody(donor, request), true);
            mailSender.send(message);
        } catch (MessagingException ex) {
            log.error("Failed to send assignment email to donor id={}", donor.getId(), ex);
        }
    }

    public void sendAcceptanceEmail(User patient, User donor, BloodRequest request) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(patient.getEmail());
            helper.setSubject("LifeLink – A Donor Has Accepted Your Blood Request");
            helper.setText(buildAcceptanceEmailBody(patient, donor, request), true);
            mailSender.send(message);
        } catch (MessagingException ex) {
            log.error("Failed to send acceptance email to patient id={}", patient.getId(), ex);
        }
    }

    private String buildAssignmentEmailBody(User donor, BloodRequest request) {
        return """
        <div style="font-family:Arial,sans-serif;font-size:14px;color:#222;line-height:1.6;max-width:600px;">
          <p>Dear %s,</p>

          <p>You have been assigned to a blood donation request through the
          <strong>LifeLink Smart Blood Donation Platform</strong>. Your prompt
          response will help save a life.</p>

          <table style="border-collapse:collapse;margin:16px 0;">
            <tr><td style="padding:4px 12px 4px 0;color:#555;">Blood Group Required</td>
                <td style="padding:4px 0;"><strong>%s</strong></td></tr>
            <tr><td style="padding:4px 12px 4px 0;color:#555;">Quantity</td>
                <td style="padding:4px 0;">%d unit(s)</td></tr>
            <tr><td style="padding:4px 12px 4px 0;color:#555;">Urgency</td>
                <td style="padding:4px 0;">%s</td></tr>
            <tr><td style="padding:4px 12px 4px 0;color:#555;">District</td>
                <td style="padding:4px 0;">%s</td></tr>
            <tr><td style="padding:4px 12px 4px 0;color:#555;">Notes</td>
                <td style="padding:4px 0;">%s</td></tr>
          </table>

          <p>Please log in to your LifeLink dashboard to <strong>accept or decline</strong>
          this assignment at your earliest convenience. If you are unable to donate
          at this time, kindly decline so the request can be reassigned to another
          eligible donor without delay.</p>

          <p>Thank you for being part of the LifeLink donor community.</p>

          <p>Sincerely,<br/>The LifeLink Team</p>

          <hr style="border:none;border-top:1px solid #ddd;margin:20px 0;"/>
          <p style="font-size:11px;color:#999;">This is an automated notification
          from LifeLink. Please do not reply directly to this email.</p>
        </div>
        """.formatted(
                donor.getFullName(),
                request.getBloodGroup().name().replace("_", " "),
                request.getQuantity(),
                request.getUrgency().name(),
                request.getDistrict(),
                request.getDescription() != null ? request.getDescription() : "—"
        );
    }

    private String buildAcceptanceEmailBody(User patient, User donor, BloodRequest request) {
        return """
        <div style="font-family:Arial,sans-serif;font-size:14px;color:#222;line-height:1.6;max-width:600px;">
          <p>Dear %s,</p>

          <p>We are pleased to inform you that a donor has <strong>accepted</strong>
          your blood donation request submitted through LifeLink.</p>

          <table style="border-collapse:collapse;margin:16px 0;">
            <tr><td style="padding:4px 12px 4px 0;color:#555;">Blood Group</td>
                <td style="padding:4px 0;"><strong>%s</strong></td></tr>
            <tr><td style="padding:4px 12px 4px 0;color:#555;">Quantity</td>
                <td style="padding:4px 0;">%d unit(s)</td></tr>
            <tr><td style="padding:4px 12px 4px 0;color:#555;">Donor Name</td>
                <td style="padding:4px 0;">%s</td></tr>
            <tr><td style="padding:4px 12px 4px 0;color:#555;">Donor Contact</td>
                <td style="padding:4px 0;">%s</td></tr>
            <tr><td style="padding:4px 12px 4px 0;color:#555;">District</td>
                <td style="padding:4px 0;">%s</td></tr>
          </table>

          <p>Please coordinate directly with the donor using the contact
          information above, or through your LifeLink dashboard, to finalize
          the donation logistics.</p>

          <p>Thank you for using LifeLink.</p>

          <p>Sincerely,<br/>The LifeLink Team</p>

          <hr style="border:none;border-top:1px solid #ddd;margin:20px 0;"/>
          <p style="font-size:11px;color:#999;">This is an automated notification
          from LifeLink. Please do not reply directly to this email.</p>
        </div>
        """.formatted(
                patient.getFullName(),
                request.getBloodGroup().name().replace("_", " "),
                request.getQuantity(),
                donor.getFullName(),
                donor.getPhone() != null ? donor.getPhone() : "Not provided",
                request.getDistrict()
        );
    }
}
