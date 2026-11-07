package com.lifelink.blood_donation.Controllers;

import com.lifelink.blood_donation.Entities.AuditLog;
import com.lifelink.blood_donation.Entities.Enums.AuditAction;
import com.lifelink.blood_donation.Security.UserPrincipal;
import com.lifelink.blood_donation.Services.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Read-only view of the admin audit trail. */
@Controller
@RequiredArgsConstructor
public class AdminAuditController {

    private final AuditService auditService;

    @GetMapping("/admin/audit-log")
    public String auditLog(@RequestParam(required = false) AuditAction action,
                           @RequestParam(defaultValue = "0") int page,
                           @AuthenticationPrincipal UserPrincipal principal, Model model) {
        Page<AuditLog> entries = auditService.getPage(action, page, 20);
        model.addAttribute("displayName", principal.getUser().getFullName());
        model.addAttribute("entries", entries);
        model.addAttribute("action", action);
        model.addAttribute("actions", AuditAction.values());
        return "admin/audit-log";
    }
}
