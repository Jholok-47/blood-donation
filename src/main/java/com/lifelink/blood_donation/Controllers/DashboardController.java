package com.lifelink.blood_donation.Controllers;

import com.lifelink.blood_donation.Services.DashboardService;
import com.lifelink.blood_donation.Services.NotificationService;
import org.springframework.ui.Model;
import com.lifelink.blood_donation.Security.UserPrincipal;
import com.lifelink.blood_donation.Services.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {
    private final ReviewService reviewService;
    private final DashboardService dashboardService;
    private final NotificationService notificationService;

    @GetMapping("/admin/dashboard")
    public String adminDashboard(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        model.addAttribute("displayName", principal.getUser().getFullName());
        model.addAttribute("pendingRequestCount", dashboardService.pendingRequestCount());
        model.addAttribute("awaitingSelectionCount", dashboardService.awaitingSelectionCount());
        model.addAttribute("unconfirmedAssignmentCount", dashboardService.unconfirmedAssignmentCount());
        return "admin/dashboard";
    }

    @GetMapping("/donor/dashboard")
    public String donorDashboard(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        Long donorId = principal.getUser().getId();
        model.addAttribute("displayName", principal.getUser().getFullName());
        model.addAttribute("openRequestCount", dashboardService.openRequestsForDonor(donorId));
        model.addAttribute("unreadCount", notificationService.getUnreadCount(donorId));      // VERIFY signature
        return "donor/dashboard";
    }

    @GetMapping("/patient/dashboard")
    public String patientDashboard(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        Long patientId = principal.getUser().getId();
        model.addAttribute("pendingReviews", reviewService.getPendingReviews(patientId));
        return "patient/dashboard";
    }
}
