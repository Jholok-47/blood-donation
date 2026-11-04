package com.lifelink.blood_donation.Services;

import com.lifelink.blood_donation.Entities.BloodRequest;
import com.lifelink.blood_donation.Entities.Enums.OtpPurpose;
import com.lifelink.blood_donation.Entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    @Value("${lifelink.base-url:http://localhost:8060}")
    private String baseUrl;
    @Value("${lifelink.donation.min-interval-months:4}")
    private int minIntervalMonths;

    // EmailService.java — replace the existing sendOtpEmail signature/body with:

    /**
     * Sends an OTP email. Unlike the assignment/acceptance emails (Module 13),
     * this is NOT best-effort: delivery failure is rethrown as an unchecked
     * MailSendException so the calling flow (registration or password reset)
     * can tell the user the code never went out.
     */
    public void sendOtpEmail(String toEmail, String otpCode, OtpPurpose purpose) {
        String subject;
        String heading;
        String bodyText;

        if (purpose == OtpPurpose.PASSWORD_RESET) {
            subject = "LifeLink - Password Reset Code";
            heading = "Reset your password";
            bodyText = "Use the code below to reset your LifeLink password. "
                    + "If you didn't request this, you can safely ignore this email.";
        } else {
            subject = "LifeLink - Verify Your Email";
            heading = "Verify your email";
            bodyText = "Use the code below to verify your LifeLink account.";
        }

        String html = """
            <div style="font-family: Arial, sans-serif; max-width: 480px; margin: auto;">
                <h2 style="color:#dc3545;">%s</h2>
                <p>%s</p>
                <p style="font-size: 28px; font-weight: bold; letter-spacing: 4px;">%s</p>
                <p style="color:#6c757d; font-size: 13px;">This code expires shortly. Do not share it with anyone.</p>
            </div>
            """.formatted(heading, bodyText, otpCode);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Failed to send {} OTP email to {}", purpose, toEmail, e);
            throw new MailSendException("Failed to send OTP email", e);
        }
    }

    // called from RequestAssignService's existing three notification trigger points.

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

    private void sendHtmlBestEffort(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MailException | MessagingException e) {
            log.warn("Email to {} failed: {}", to, e.getMessage());
        }
    }

    private static String esc(String s) {
        return s == null ? "" : HtmlUtils.htmlEscape(s);   // org.springframework.web.util.HtmlUtils
    }

    private String layout(String heading, String bodyHtml, String buttonText, String path) {
        return """
        <div style="font-family:Arial,sans-serif;max-width:520px;margin:auto;border:1px solid #eee;border-radius:8px;padding:24px">
          <h2 style="color:#dc3545;margin-top:0">%s</h2>
          %s
          <p style="margin:24px 0"><a href="%s%s" style="background:#dc3545;color:#fff;padding:10px 18px;border-radius:6px;text-decoration:none">%s</a></p>
          <p style="color:#888;font-size:12px">LifeLink Blood Donation Platform</p>
        </div>
        """.formatted(heading, bodyHtml, baseUrl, path, buttonText);
    }

    /** Bulk send -> @Async so approving a request never waits for SMTP. Needs @EnableAsync (section 11). */
    @Async
    public void sendBroadcastEmail(String to, String donorName, String bloodGroup, String quantity,
                                   String urgency, String district, String description) {
        String body = """
        <p>Hi %s,</p>
        <p>A patient in <b>%s</b> needs <b>%s unit(s) of %s</b> blood. Urgency: <b>%s</b>.</p>
        <p>%s</p>
        <p>If you can donate, open LifeLink and tap <b>I can donate</b>. You are not committed until an admin selects you.
        Please accept only if you have not donated in the last %d months and you feel healthy.</p>
        """.formatted(esc(donorName), esc(district), esc(quantity), esc(bloodGroup), esc(urgency),
                esc(description), minIntervalMonths);
        sendHtmlBestEffort(to, "[LifeLink] " + urgency + ": " + bloodGroup + " blood needed in " + district,
                layout("Blood needed near you", body, "Respond now", "/donor/requests"));
    }

    public void sendSelectionEmail(String to, String donorName, String bloodGroup, String district,
                                   String patientName, String patientPhone, String confirmMinutes) {
        String body = """
        <p>Hi %s,</p>
        <p>Thank you for accepting. An admin has <b>selected you</b> to donate <b>1 unit</b> of %s blood for a patient in %s.</p>
        <p>Patient: <b>%s</b>%s</p>
        <p>No further acceptance is needed. Please tap <b>Confirm I'm on my way</b> within %s minutes so we know you are reachable.
        If you can no longer donate, tell us right away so we can find someone else.</p>
        """.formatted(esc(donorName), esc(bloodGroup), esc(district), esc(patientName),
                (patientPhone == null || patientPhone.isBlank()) ? "" : " - " + esc(patientPhone), esc(confirmMinutes));
        sendHtmlBestEffort(to, "[LifeLink] You have been selected to donate " + bloodGroup,
                layout("You've been selected", body, "Open my assignments", "/donor/assignments"));
    }

    /**
     * ONE email to the patient per assignment batch (replaces the old sendAcceptanceEmail and the single-donor version).
     * donorLines = ready-made "Name - phone" strings; covered = units with a donor so far.
     */
    public void sendDonorsAssignedEmail(String to, String patientName, String bloodGroup, String unitsNeeded,
                                        String district, List<String> donorLines, int covered) {
        StringBuilder list = new StringBuilder("<ul>");
        for (String line : donorLines) {
            list.append("<li>").append(esc(line)).append("</li>");
        }
        list.append("</ul>");
        String body = """
        <p>Hi %s,</p>
        <p>Good news: donor(s) have been assigned to your %s request in %s. Units covered so far: <b>%d of %s</b>.</p>
        <p>New donor(s):</p>%s
        <p>The hospital's own screening still applies before donation.</p>
        """.formatted(esc(patientName), esc(bloodGroup), esc(district), covered, esc(unitsNeeded), list);
        sendHtmlBestEffort(to, "[LifeLink] " + covered + " of " + unitsNeeded + " unit(s) now have a donor",
                layout("Donors assigned", body, "View my requests", "/patient/requests"));
    }
}
