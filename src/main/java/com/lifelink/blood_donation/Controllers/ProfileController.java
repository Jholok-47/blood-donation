package com.lifelink.blood_donation.Controllers;

import com.lifelink.blood_donation.DTO.ProfileUpdateRequest;
import com.lifelink.blood_donation.Entities.Enums.BloodGroup;
import com.lifelink.blood_donation.Entities.User;
import com.lifelink.blood_donation.Security.UserPrincipal;
import com.lifelink.blood_donation.Services.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    // ---------- DONOR ----------

    @GetMapping("/donor/profile")
    public String donorProfileForm(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        User donor = profileService.getCurrentUser(principal.getUser().getId());
        model.addAttribute("profileUpdateRequest", toDto(donor));
        model.addAttribute("bloodGroups", BloodGroup.values());
        model.addAttribute("available", donor.isAvailable());
        model.addAttribute("verified", donor.isVerified());
        return "donor-profile";
    }

    @PostMapping("/donor/profile")
    public String updateDonorProfile(@Valid @ModelAttribute("profileUpdateRequest") ProfileUpdateRequest dto,
                                     BindingResult result,
                                     @AuthenticationPrincipal UserPrincipal principal,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("bloodGroups", BloodGroup.values());
            return "donor-profile";
        }
        profileService.updateProfile(principal.getUser().getId(), dto);
        redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully.");
        return "redirect:/donor/profile";
    }

    @PostMapping("/donor/profile/toggle-availability")
    public String toggleAvailability(@AuthenticationPrincipal UserPrincipal principal,
                                     RedirectAttributes redirectAttributes) {
        profileService.toggleAvailability(principal.getUser().getId());
        redirectAttributes.addFlashAttribute("successMessage", "Availability status updated.");
        return "redirect:/donor/profile";
    }

    // ---------- PATIENT ----------

    @GetMapping("/patient/profile")
    public String patientProfileForm(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        User patient = profileService.getCurrentUser(principal.getUser().getId());
        model.addAttribute("profileUpdateRequest", toDto(patient));
        model.addAttribute("bloodGroups", BloodGroup.values());
        return "patient-profile";
    }

    @PostMapping("/patient/profile")
    public String updatePatientProfile(@Valid @ModelAttribute("profileUpdateRequest") ProfileUpdateRequest dto,
                                       BindingResult result,
                                       @AuthenticationPrincipal UserPrincipal principal,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("bloodGroups", BloodGroup.values());
            return "patient-profile";
        }
        profileService.updateProfile(principal.getUser().getId(), dto);
        redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully.");
        return "redirect:/patient/profile";
    }

    private ProfileUpdateRequest toDto(User user) {
        ProfileUpdateRequest dto = new ProfileUpdateRequest();
        dto.setFullName(user.getFullName());
        dto.setPhone(user.getPhone());
        dto.setBloodGroup(user.getBloodGroup());
        dto.setDistrict(user.getDistrict());
        dto.setLatitude(user.getLatitude());
        dto.setLongitude(user.getLongitude());
        return dto;
    }
}
