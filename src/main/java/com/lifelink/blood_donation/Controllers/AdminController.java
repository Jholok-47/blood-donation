package com.lifelink.blood_donation.Controllers;

import com.lifelink.blood_donation.Entities.User;
import com.lifelink.blood_donation.Exceptions.InvalidOperationException;
import com.lifelink.blood_donation.Exceptions.ResourceNotFoundException;
import com.lifelink.blood_donation.Services.BloodRequestService;
import com.lifelink.blood_donation.Services.DonationHistoryService;
import com.lifelink.blood_donation.Services.ProfileService;
import com.lifelink.blood_donation.Services.RequestAssignService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class AdminController {

    private final RequestAssignService requestAssignService;
    private final ProfileService profileService;
    private final BloodRequestService bloodRequestService;
    private final DonationHistoryService donationHistoryService;

    //Module 3: User profile management for Admins
    @GetMapping("/admin/donors/unverified")
    public String unverifiedDonors(Model model) {
        List<User> donors = profileService.getUnverifiedDonors();
        model.addAttribute("donors", donors);
        return "admin/unverified-donors";
    }

    @PostMapping("/admin/donors/{id}/verify")
    public String verifyDonor(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            profileService.verifyDonor(id);
            redirectAttributes.addFlashAttribute("successMessage", "Donor verified successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/donors/unverified";
    }

    // Module 4: Blood request management for Admins
    // Add this field (constructor injection via @RequiredArgsConstructor, same pattern as existing fields)

    @GetMapping("/admin/requests")
    public String viewRequests(Model model) {
        model.addAttribute("requests", bloodRequestService.getAllRequests());
        return "admin/requests";
    }

    @PostMapping("/admin/requests/{id}/approve")
    public String approveRequest(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            bloodRequestService.approveRequest(id);
            redirectAttributes.addFlashAttribute("successMessage", "Request approved.");
        } catch (ResourceNotFoundException | InvalidOperationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/requests";
    }

    @PostMapping("/admin/requests/{id}/reject")
    public String rejectRequest(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            bloodRequestService.rejectRequest(id);
            redirectAttributes.addFlashAttribute("successMessage", "Request rejected.");
        } catch (ResourceNotFoundException | InvalidOperationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/requests";
    }

    // Module 5: Donor assignment management for Admins

    // Shows compatible donors for this request — works for APPROVED (first assign) or ASSIGNED (reassign) requests
    @GetMapping("/admin/requests/{id}/assign")
    public String showAssignDonorPage(@PathVariable Long id, Model model) {
        model.addAttribute("request", bloodRequestService.getRequestById(id));
        model.addAttribute("compatibleDonors", requestAssignService.getCompatibleDonors(id));
        return "admin/assign-donor";
    }

    // First-time assignment for an APPROVED request
    @PostMapping("/admin/requests/{id}/assign")
    public String assignDonor(@PathVariable Long id, @RequestParam Long donorId, RedirectAttributes redirectAttributes) {
        try {
            requestAssignService.assignDonor(id, donorId);
            redirectAttributes.addFlashAttribute("successMessage", "Donor assigned successfully");
        } catch (InvalidOperationException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/requests";
    }

    // Reassignment for an already-ASSIGNED request (cancels current active assignment, creates a new one)
    @PostMapping("/admin/requests/{id}/reassign")
    public String reassignDonor(@PathVariable Long id, @RequestParam Long donorId, RedirectAttributes redirectAttributes) {
        try {
            requestAssignService.reassignDonor(id, donorId);
            redirectAttributes.addFlashAttribute("successMessage", "Donor reassigned successfully");
        } catch (InvalidOperationException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/requests";
    }

    // Module 6: Donation History

    @PostMapping("/admin/requests/{id}/complete-donation")
    public String completeDonation(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            donationHistoryService.completeDonation(id);
            redirectAttributes.addFlashAttribute("successMessage", "Donation marked as completed.");
        } catch (InvalidOperationException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/requests";
    }
}