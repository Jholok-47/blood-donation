package com.lifelink.blood_donation.Controllers;

import com.lifelink.blood_donation.Config.GoogleMapsProperties;
import com.lifelink.blood_donation.DTO.RankedDonorDto;
import com.lifelink.blood_donation.Entities.BloodRequest;
import com.lifelink.blood_donation.Entities.Enums.RequestStatus;
import com.lifelink.blood_donation.Entities.Enums.UrgencyLevel;
import com.lifelink.blood_donation.Entities.User;
import com.lifelink.blood_donation.Exceptions.InvalidOperationException;
import com.lifelink.blood_donation.Exceptions.ResourceNotFoundException;
import com.lifelink.blood_donation.Services.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class AdminController {

    private final RequestAssignService requestAssignService;
    private final ProfileService profileService;
    private final BloodRequestService bloodRequestService;
    private final DonationHistoryService donationHistoryService;
    private final SearchService searchService;
    private final GoogleMapsProperties googleMapsProperties;
    private final DonorInterestService donorInterestService;
    private final RequestProgressService progressService;

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
    public String viewRequests(@RequestParam(required = false) RequestStatus status,
                               @RequestParam(required = false) UrgencyLevel urgency,
                               Model model) {
        List<BloodRequest> requests = searchService.filterRequests(status, urgency);

        List<Long> ids = requests.stream().map(BloodRequest::getId).toList();

        model.addAttribute("requests", requests);
        model.addAttribute("statuses", RequestStatus.values());
        model.addAttribute("urgencyLevels", UrgencyLevel.values());
        model.addAttribute("interestedCounts", donorInterestService.getInterestedCountByRequestId(ids));
        model.addAttribute("assignmentsByRequestId", requestAssignService.getActiveAssignmentsByRequestId(ids));
        model.addAttribute("progressByRequestId", progressService.getProgressByRequestId(requests));
        return "admin/requests";
    }

    @PostMapping("/admin/requests/{id}/approve")
    public String approveRequest(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            bloodRequestService.approveRequest(id);
            int notified = donorInterestService.broadcastForRequest(id);
            redirectAttributes.addFlashAttribute("successMessage", notified > 0
                    ? "Request approved. " + notified + " eligible donor(s) were notified."
                    : "Request approved, but no eligible donors could be notified right now. Use 'Notify donors' on the donor selection page later.");
        } catch (InvalidOperationException | ResourceNotFoundException e) {
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

    @GetMapping("/admin/requests/{id}/assign")
    public String assignDonorPage(@PathVariable Long id, Model model) {
        BloodRequest bloodRequest = bloodRequestService.getRequestById(id);
        List<RankedDonorDto> rankedDonors = requestAssignService.getRankedInterestedDonors(id);
        model.addAttribute("bloodRequest", bloodRequest);
        model.addAttribute("progress", progressService.getProgress(bloodRequest));
        model.addAttribute("assignments", requestAssignService.findAssignmentsForRequest(id));
        model.addAttribute("rankedDonors", rankedDonors);
        model.addAttribute("interestSummary", donorInterestService.getSummary(id));
        // KEEP: your existing lines that add the map attributes (markers, API key, config).
        //       toDonorMapMarkers(...) must now receive the interested donors (pass what it accepted before).
        return "admin/assign-donor";
    }

    /** One OR several donors in one go: the form posts repeated donorIds. */
    @PostMapping("/admin/requests/{id}/assign")
    public String assignDonors(@PathVariable Long id,
                               @RequestParam(name = "donorIds", required = false) List<Long> donorIds,
                               RedirectAttributes redirectAttributes) {
        try {
            int n = requestAssignService.assignDonors(id, donorIds);
            redirectAttributes.addFlashAttribute("successMessage", n + " donor(s) assigned. They and the patient have been notified.");
        } catch (InvalidOperationException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/requests/" + id + "/assign";
    }

    @PostMapping("/admin/donors/{id}/delete")
    public String deleteUnverifiedDonor(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            profileService.deleteUnverifiedDonor(id);
            redirectAttributes.addFlashAttribute("successMessage", "Donor account deleted successfully.");
        } catch (InvalidOperationException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/donors/unverified";
    }

    @PostMapping("/admin/requests/{id}/rebroadcast")
    public String rebroadcast(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            int n = donorInterestService.broadcastForRequest(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    n > 0 ? n + " more donor(s) were notified." : "Everyone eligible has already been contacted.");
        } catch (InvalidOperationException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/requests/" + id + "/assign";
    }

    @PostMapping("/admin/requests/{id}/close")
    public String closeRequest(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            requestAssignService.closeRequestEarly(id);
            redirectAttributes.addFlashAttribute("successMessage", "Request closed with the units donated so far.");
        } catch (InvalidOperationException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/requests/" + id + "/assign";
    }

    // ---- per-donor actions (one assignment = one unit); each returns to that request's page ----
    @PostMapping("/admin/assignments/{assignmentId}/confirm")
    public String confirmByPhone(@PathVariable Long assignmentId, RedirectAttributes ra) {
        return handleAssignmentAction(assignmentId, ra, "Marked as confirmed.", requestAssignService::adminConfirmByPhone);
    }

    @PostMapping("/admin/assignments/{assignmentId}/release-unresponsive")
    public String releaseUnresponsive(@PathVariable Long assignmentId, RedirectAttributes ra) {
        return handleAssignmentAction(assignmentId, ra, "Donor released. That unit is open again.", requestAssignService::markUnresponsive);
    }

    @PostMapping("/admin/assignments/{assignmentId}/cancel")
    public String cancelAssignment(@PathVariable Long assignmentId, RedirectAttributes ra) {
        return handleAssignmentAction(assignmentId, ra, "Assignment cancelled. That unit is open again.", requestAssignService::cancelAssignment);
    }

    @PostMapping("/admin/assignments/{assignmentId}/complete-donation")
    public String completeDonation(@PathVariable Long assignmentId, RedirectAttributes ra) {
        return handleAssignmentAction(assignmentId, ra, "Donation recorded.", donationHistoryService::completeDonation);
    }

    private String handleAssignmentAction(Long assignmentId, RedirectAttributes ra, String okMessage,
                                          java.util.function.Consumer<Long> action) {
        Long requestId = null;
        try {
            requestId = requestAssignService.getRequestIdOfAssignment(assignmentId);
            action.accept(assignmentId);
            ra.addFlashAttribute("successMessage", okMessage);
        } catch (InvalidOperationException | ResourceNotFoundException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return requestId == null ? "redirect:/admin/requests" : "redirect:/admin/requests/" + requestId + "/assign";
    }
}