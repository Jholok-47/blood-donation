package com.lifelink.blood_donation.Controllers;

import com.lifelink.blood_donation.DTO.ProfileUpdateRequest;
import com.lifelink.blood_donation.Entities.Enums.BloodGroup;
import com.lifelink.blood_donation.Entities.User;
import com.lifelink.blood_donation.Repositories.UserRepository;
import com.lifelink.blood_donation.Security.UserPrincipal;
import com.lifelink.blood_donation.Services.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.lifelink.blood_donation.DTO.ProfileCompletionDto;
import com.lifelink.blood_donation.Entities.Enums.Role;
import com.lifelink.blood_donation.Exceptions.InvalidOperationException;
import com.lifelink.blood_donation.Config.GoogleMapsProperties;


@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository userRepository;
    private final ProfileService profileService;
    private final GoogleMapsProperties googleMapsProperties;

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

    // ---------- Module 11: Complete Profile (shared by both Google and local-OTP paths) ----------

    @GetMapping("/complete-profile")
    public String showCompleteProfileForm(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        User user = principal.getUser();

        // Already completed and lands here anyway (e.g. stale bookmark) — don't re-collect, just move them along.
        if (user.isProfileCompleted()) {
            return "redirect:/" + user.getRole().name().toLowerCase() + "/dashboard";
        }

        model.addAttribute("profileCompletionDto", new ProfileCompletionDto());
        model.addAttribute("roles", new Role[]{Role.PATIENT, Role.DONOR}); // never offer ADMIN here
        model.addAttribute("bloodGroups", BloodGroup.values());

        // Same map-fragment attributes AdminController/SearchController pass in for Module 10's fragments/map.html —
        // the click-picker fragment reuses maps-loader.html, which expects these.
        model.addAttribute("mapsApiKey", googleMapsProperties.getApiKey());
        model.addAttribute("defaultLat", googleMapsProperties.getDefaultLat());
        model.addAttribute("defaultLng", googleMapsProperties.getDefaultLng());
        model.addAttribute("defaultZoom", googleMapsProperties.getDefaultZoom());

        return "complete-profile";
    }

    @PostMapping("/complete-profile")
    public String completeProfile(@AuthenticationPrincipal UserPrincipal principal,
                                  @Valid @ModelAttribute("profileCompletionDto") ProfileCompletionDto dto,
                                  BindingResult bindingResult,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("roles", new Role[]{Role.PATIENT, Role.DONOR});
            model.addAttribute("bloodGroups", BloodGroup.values());
            model.addAttribute("mapsApiKey", googleMapsProperties.getApiKey());
            model.addAttribute("defaultLat", googleMapsProperties.getDefaultLat());
            model.addAttribute("defaultLng", googleMapsProperties.getDefaultLng());
            model.addAttribute("defaultZoom", googleMapsProperties.getDefaultZoom());
            return "complete-profile";
        }

        try {
            profileService.completeProfile(principal.getUser().getId(), dto);
        } catch (InvalidOperationException ex) {
            model.addAttribute("profileError", ex.getMessage());
            model.addAttribute("roles", new Role[]{Role.PATIENT, Role.DONOR});
            model.addAttribute("bloodGroups", BloodGroup.values());
            model.addAttribute("mapsApiKey", googleMapsProperties.getApiKey());
            model.addAttribute("defaultLat", googleMapsProperties.getDefaultLat());
            model.addAttribute("defaultLng", googleMapsProperties.getDefaultLng());
            model.addAttribute("defaultZoom", googleMapsProperties.getDefaultZoom());
            return "complete-profile";
        }

        // Re-fetch the now-updated User and rebuild the session's Authentication —
        // otherwise ProfileCompletionFilter keeps redirecting based on the stale in-memory copy.
        User updatedUser = userRepository.findById(principal.getUser().getId()).orElseThrow();
        UserPrincipal refreshedPrincipal = new UserPrincipal(updatedUser);
        Authentication newAuth = new UsernamePasswordAuthenticationToken(
                refreshedPrincipal, null, refreshedPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(newAuth);

        redirectAttributes.addFlashAttribute("successMessage", "Profile completed!");
        return "redirect:/" + updatedUser.getRole().name().toLowerCase() + "/dashboard";
    }
}
