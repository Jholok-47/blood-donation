package com.lifelink.blood_donation.Controllers;

import com.lifelink.blood_donation.Exceptions.InvalidOperationException;
import com.lifelink.blood_donation.Exceptions.ResourceNotFoundException;
import com.lifelink.blood_donation.Services.RequestAssignService;
import com.lifelink.blood_donation.Security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class DonorController {

    private final RequestAssignService requestAssignService;

    @GetMapping("/donor/assignments")
    public String myAssignments(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        model.addAttribute("assignments", requestAssignService.getMyAssignments(principal.getUser().getId()));
        return "donor/my-assignments";
    }

    @PostMapping("/donor/assignments/{id}/accept")
    public String acceptAssignment(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal,
                                   RedirectAttributes redirectAttributes) {
        try {
            requestAssignService.acceptAssignment(id, principal.getUser().getId());
            redirectAttributes.addFlashAttribute("successMessage", "Assignment accepted");
        } catch (InvalidOperationException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/donor/assignments";
    }

    @PostMapping("/donor/assignments/{id}/decline")
    public String declineAssignment(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal,
                                    RedirectAttributes redirectAttributes) {
        try {
            requestAssignService.declineAssignment(id, principal.getUser().getId());
            redirectAttributes.addFlashAttribute("successMessage", "Assignment declined");
        } catch (InvalidOperationException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/donor/assignments";
    }
}