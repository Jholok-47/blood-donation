package com.lifelink.blood_donation.Controllers;

import com.lifelink.blood_donation.DTO.ReviewCreateDto;
import com.lifelink.blood_donation.DTO.ReviewableDonationDto;
import com.lifelink.blood_donation.Exceptions.InvalidOperationException;
import com.lifelink.blood_donation.Exceptions.ResourceNotFoundException;
import com.lifelink.blood_donation.Security.UserPrincipal;
import com.lifelink.blood_donation.Services.ReviewService;
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
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/patient/reviews/new")
    public String showReviewForm(@RequestParam("donationId") Long donationId,
                                 @AuthenticationPrincipal UserPrincipal principal,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        try {
            ReviewableDonationDto donation =
                    reviewService.getReviewableDonation(principal.getUser().getId(), donationId);

            if (donation.isReviewed()) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "You have already reviewed this donation.");
                return "redirect:/patient/requests";
            }
            if (!donation.isCanReview()) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "The review period for this donation has ended.");
                return "redirect:/patient/requests";
            }

            ReviewCreateDto form = new ReviewCreateDto();
            form.setDonationHistoryId(donationId);
            model.addAttribute("reviewForm", form);
            model.addAttribute("donation", donation);
            return "patient/review-form";
        } catch (ResourceNotFoundException | InvalidOperationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/patient/requests";
        }
    }

    @PostMapping("/patient/reviews")
    public String submitReview(@Valid @ModelAttribute("reviewForm") ReviewCreateDto form,
                               BindingResult bindingResult,
                               @AuthenticationPrincipal UserPrincipal principal,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        Long patientId = principal.getUser().getId();

        if (form.getDonationHistoryId() == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid donation record.");
            return "redirect:/patient/requests";
        }

        if (bindingResult.hasErrors()) {
            try {
                model.addAttribute("donation",
                        reviewService.getReviewableDonation(patientId, form.getDonationHistoryId()));
                return "patient/review-form";
            } catch (ResourceNotFoundException | InvalidOperationException e) {
                redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
                return "redirect:/patient/requests";
            }
        }

        try {
            reviewService.submitReview(patientId, form);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Thank you! Your review has been submitted.");
        } catch (ResourceNotFoundException | InvalidOperationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/patient/requests";
    }
}
