package com.lifelink.blood_donation.Controllers;

import com.lifelink.blood_donation.DTO.SystemIssueDto;
import com.lifelink.blood_donation.Entities.Enums.AuditAction;
import com.lifelink.blood_donation.Security.UserPrincipal;
import com.lifelink.blood_donation.Services.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Admin Control Center, system Issues page, Reports page and the donations CSV export. */
@Controller
@RequiredArgsConstructor
public class AdminMonitoringController {

    private final AdminOverviewService adminOverviewService;
    private final AdminIssueService adminIssueService;
    private final AdminReportService adminReportService;
    private final AuditService auditService;

    @GetMapping("/admin/overview")
    public String overview(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        List<SystemIssueDto> issues = adminIssueService.detectIssues();
        model.addAttribute("displayName", principal.getUser().getFullName());
        model.addAttribute("overview", adminOverviewService.build(issues));
        model.addAttribute("topIssues", issues.stream().limit(5).toList());
        return "admin/overview";
    }

    @GetMapping("/admin/issues")
    public String issues(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        List<SystemIssueDto> issues = adminIssueService.detectIssues();
        model.addAttribute("displayName", principal.getUser().getFullName());
        model.addAttribute("issues", issues);
        model.addAttribute("criticalCount", issues.stream().filter(i -> i.getSeverity().name().equals("CRITICAL")).count());
        model.addAttribute("warningCount", issues.stream().filter(i -> i.getSeverity().name().equals("WARNING")).count());
        model.addAttribute("infoCount", issues.stream().filter(i -> i.getSeverity().name().equals("INFO")).count());
        return "admin/issues";
    }

    @GetMapping("/admin/reports")
    public String reports(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                          @AuthenticationPrincipal UserPrincipal principal, Model model) {
        LocalDate[] range = normalize(from, to);
        model.addAttribute("displayName", principal.getUser().getFullName());
        model.addAttribute("report", adminReportService.build(range[0], range[1]));
        model.addAttribute("from", range[0]);
        model.addAttribute("to", range[1]);
        model.addAttribute("today", LocalDate.now());
        return "admin/reports";
    }

    @GetMapping("/admin/reports/export/donations.csv")
    public void exportDonations(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                @AuthenticationPrincipal UserPrincipal principal,
                                HttpServletResponse response) throws IOException {
        LocalDate[] range = normalize(from, to);
        String csv = adminReportService.donationsCsv(range[0], range[1]);
        auditService.logAction(principal.getUser(), AuditAction.REPORT_EXPORTED, "REPORT", null,
                "Exported donations CSV for " + range[0] + " to " + range[1] + ".");
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"lifelink-donations_" + range[0] + "_" + range[1] + ".csv\"");
        response.getOutputStream().write(("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8)); // BOM so Excel reads UTF-8
    }

    /** Defaults to the last 30 days; swaps reversed dates; caps the range at one year. */
    private static LocalDate[] normalize(LocalDate from, LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(29);
        if (start.isAfter(end)) {
            LocalDate t = start;
            start = end;
            end = t;
        }
        if (ChronoUnit.DAYS.between(start, end) > 365) start = end.minusDays(365);
        return new LocalDate[]{start, end};
    }
}
