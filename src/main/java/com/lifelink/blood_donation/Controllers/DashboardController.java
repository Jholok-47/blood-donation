package com.lifelink.blood_donation.Controllers;

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

    @GetMapping("/admin/dashboard")
    public String adminDashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/donor/dashboard")
    public String donorDashboard() {
        return "donor/dashboard";
    }

    @GetMapping("/patient/dashboard")
    public String patientDashboard(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        Long patientId = principal.getUser().getId();
        model.addAttribute("pendingReviews", reviewService.getPendingReviews(patientId));
        return "patient/dashboard";
    }
}
