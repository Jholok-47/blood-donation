package com.lifelink.blood_donation.Controllers;

import com.lifelink.blood_donation.Entities.Enums.BloodGroup;
import com.lifelink.blood_donation.Entities.Enums.RequestStatus;
import com.lifelink.blood_donation.Entities.Enums.UrgencyLevel;
import com.lifelink.blood_donation.Services.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    // Accessible to PATIENT and ADMIN — see SecurityConfig note below.
    @GetMapping("/search/donors")
    public String searchDonors(@RequestParam(required = false) BloodGroup bloodGroup,
                               @RequestParam(required = false) String district,
                               Model model) {
        model.addAttribute("bloodGroups", BloodGroup.values());
        model.addAttribute("selectedBloodGroup", bloodGroup);
        model.addAttribute("selectedDistrict", district);
        model.addAttribute("donors", searchService.searchDonors(bloodGroup, district));
        return "search/donor-search";
    }

    // Admin only.
//    @GetMapping("/search/requests")
//    public String filterRequests(@RequestParam(required = false) RequestStatus status,
//                                 @RequestParam(required = false) UrgencyLevel urgency,
//                                 Model model) {
//        model.addAttribute("statuses", RequestStatus.values());
//        model.addAttribute("urgencyLevels", UrgencyLevel.values());
//        model.addAttribute("selectedStatus", status);
//        model.addAttribute("selectedUrgency", urgency);
//        model.addAttribute("requests", searchService.filterRequests(status, urgency));
//        return "search/request-filter";
//    }
}