package com.lifelink.blood_donation.Controllers;

import com.lifelink.blood_donation.Entities.DonationHistory;
import com.lifelink.blood_donation.Exceptions.InvalidOperationException;
import com.lifelink.blood_donation.Exceptions.ResourceNotFoundException;
import com.lifelink.blood_donation.Services.*;
import com.lifelink.blood_donation.Security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class DonorController {

    private final NotificationService notificationService;
    private final RequestAssignService requestAssignService;
    private final DonationHistoryService donationHistoryService;
    private final ReviewService reviewService;
    private final DonorInterestService donorInterestService;
    @Value("${lifelink.donation.min-interval-months:4}") private int minIntervalMonths;

    @GetMapping("/donor/requests")
    public String openRequests(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        Long donorId = principal.getUser().getId();
        model.addAttribute("interests", donorInterestService.getMyInterests(donorId));
        model.addAttribute("minIntervalMonths", minIntervalMonths);
        return "donor/open-requests";
    }

    @PostMapping("/donor/requests/{requestId}/respond")
    public String respond(@PathVariable Long requestId, @RequestParam boolean interested,
                          @RequestParam(defaultValue = "false") boolean attested,
                          @AuthenticationPrincipal UserPrincipal principal, RedirectAttributes ra) {
        Long donorId = principal.getUser().getId();
        try {
            donorInterestService.respond(donorId, requestId, interested, attested);
            ra.addFlashAttribute("successMessage", interested
                    ? "Thank you! The admin can now select you for this request."
                    : "Response saved. You will not be considered for this request.");
        } catch (InvalidOperationException | ResourceNotFoundException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/donor/requests";
    }

    @GetMapping("/donor/assignments")
    public String myAssignments(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        Long donorId = principal.getUser().getId();
        model.addAttribute("assignments", requestAssignService.getMyAssignments(donorId));   // VERIFY attribute name used before
        return "donor/my-assignments";
    }

    @PostMapping("/donor/assignments/{id}/confirm")
    public String confirmAssignment(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal,
                                    RedirectAttributes ra) {
        Long donorId = principal.getUser().getId();
        try {
            requestAssignService.confirmAssignment(id, donorId);
            ra.addFlashAttribute("successMessage", "Thanks! The admin can see that you are on your way.");
        } catch (InvalidOperationException | ResourceNotFoundException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/donor/assignments";
    }

    @PostMapping("/donor/assignments/{id}/withdraw")
    public String withdraw(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal, RedirectAttributes ra) {
        Long donorId = principal.getUser().getId();
        try {
            requestAssignService.withdrawFromAssignment(id, donorId);
            ra.addFlashAttribute("successMessage", "Understood. The admin will find another donor.");
        } catch (InvalidOperationException | ResourceNotFoundException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/donor/assignments";
    }

     // Module 6: Donation history

    @GetMapping("/donor/donations")
    public String myDonations(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        Long donorId = principal.getUser().getId();
        List<DonationHistory> donations = donationHistoryService.getMyDonationHistory(donorId);
        model.addAttribute("donations", donations);
        model.addAttribute("ratingSummary", reviewService.getDonorRatingSummary(donorId));
        model.addAttribute("reviewsByDonationId", reviewService.getReviewsByDonationIdForDonor(donorId));
        return "donor/my-donations";
    }

    // Module 9: Notifications

    @GetMapping("/donor/notifications")
    public String viewNotifications(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        model.addAttribute("notifications", notificationService.getMyNotifications(principal.getUser().getId()));
        return "donor/notifications";
    }

    @PostMapping("/donor/notifications/{id}/read")
    public String markNotificationRead(@PathVariable Long id,
                                       @AuthenticationPrincipal UserPrincipal principal,
                                       RedirectAttributes redirectAttributes) {
        try {
            notificationService.markAsRead(id, principal.getUser().getId());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/donor/notifications";
    }
}