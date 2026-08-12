package com.lifelink.blood_donation.Controllers;

import com.lifelink.blood_donation.DTO.BloodRequestCreateDto;
import com.lifelink.blood_donation.Entities.Enums.BloodGroup;
import com.lifelink.blood_donation.Entities.Enums.UrgencyLevel;
import com.lifelink.blood_donation.Security.UserPrincipal;
import com.lifelink.blood_donation.Services.BloodRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/patient/requests")
@RequiredArgsConstructor
public class BloodRequestController {

    private final BloodRequestService bloodRequestService;

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("bloodRequestCreateDto", new BloodRequestCreateDto());
        model.addAttribute("bloodGroups", BloodGroup.values());
        model.addAttribute("urgencyLevels", UrgencyLevel.values());
        return "patient/create-request";
    }

    @PostMapping
    public String createRequest(@Valid @ModelAttribute("bloodRequestCreateDto") BloodRequestCreateDto dto,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal UserPrincipal principal,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("bloodGroups", BloodGroup.values());
            model.addAttribute("urgencyLevels", UrgencyLevel.values());
            return "patient/create-request";
        }

        bloodRequestService.createRequest(principal.getUser(), dto);
        redirectAttributes.addFlashAttribute("successMessage", "Blood request submitted successfully.");
        return "redirect:/patient/requests";
    }

    @GetMapping
    public String myRequests(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        model.addAttribute("requests", bloodRequestService.getMyRequests(principal.getUser().getId()));
        return "patient/my-requests";
    }
}
