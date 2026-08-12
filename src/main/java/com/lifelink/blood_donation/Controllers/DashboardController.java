package com.lifelink.blood_donation.Controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/admin/dashboard")
    public String adminDashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/donor/dashboard")
    public String donorDashboard() {
        return "donor/dashboard";
    }

    @GetMapping("/patient/dashboard")
    public String patientDashboard() {
        return "patient/dashboard";
    }
}
