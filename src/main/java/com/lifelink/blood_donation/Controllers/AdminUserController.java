package com.lifelink.blood_donation.Controllers;

import com.lifelink.blood_donation.Entities.Enums.Role;
import com.lifelink.blood_donation.Entities.User;
import com.lifelink.blood_donation.Exceptions.InvalidOperationException;
import com.lifelink.blood_donation.Exceptions.ResourceNotFoundException;
import com.lifelink.blood_donation.Security.UserPrincipal;
import com.lifelink.blood_donation.Services.UserManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Admin user management: search, activity detail, suspend / reactivate. */
@Controller
@RequiredArgsConstructor
public class AdminUserController {

    private static final int PAGE_SIZE = 15;

    private final UserManagementService userManagementService;

    @GetMapping("/admin/users")
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) Role role,
                       @RequestParam(required = false) Boolean verified,
                       @RequestParam(required = false) Boolean active,
                       @RequestParam(required = false) String flag,
                       @RequestParam(defaultValue = "0") int page,
                       @AuthenticationPrincipal UserPrincipal principal, Model model) {
        Page<User> users = userManagementService.search(q, role, verified, active,
                "missingLocation".equals(flag), "incompleteProfile".equals(flag), page, PAGE_SIZE);
        model.addAttribute("displayName", principal.getUser().getFullName());
        model.addAttribute("users", users);
        model.addAttribute("q", q);
        model.addAttribute("role", role);
        model.addAttribute("verified", verified);
        model.addAttribute("active", active);
        model.addAttribute("flag", flag);
        model.addAttribute("roles", Role.values());
        return "admin/users";
    }

    @GetMapping("/admin/users/{id}")
    public String detail(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal,
                         Model model, RedirectAttributes ra) {
        try {
            model.addAttribute("detail", userManagementService.getDetail(id));
        } catch (ResourceNotFoundException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/users";
        }
        model.addAttribute("displayName", principal.getUser().getFullName());
        model.addAttribute("currentAdminId", principal.getUser().getId());
        return "admin/user-detail";
    }

    @PostMapping("/admin/users/{id}/suspend")
    public String suspend(@PathVariable Long id, @RequestParam String reason,
                          @AuthenticationPrincipal UserPrincipal principal, RedirectAttributes ra) {
        try {
            userManagementService.suspend(principal.getUser(), id, reason);
            ra.addFlashAttribute("successMessage", "Account suspended. The user is signed out on their next request.");
        } catch (InvalidOperationException | ResourceNotFoundException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users/" + id;
    }

    @PostMapping("/admin/users/{id}/reactivate")
    public String reactivate(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal,
                             RedirectAttributes ra) {
        try {
            userManagementService.reactivate(principal.getUser(), id);
            ra.addFlashAttribute("successMessage", "Account reactivated.");
        } catch (InvalidOperationException | ResourceNotFoundException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users/" + id;
    }
}
