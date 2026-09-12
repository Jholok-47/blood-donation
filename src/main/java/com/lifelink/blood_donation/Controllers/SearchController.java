package com.lifelink.blood_donation.Controllers;

import com.lifelink.blood_donation.Config.GoogleMapsProperties;
import com.lifelink.blood_donation.Entities.Enums.BloodGroup;
import com.lifelink.blood_donation.Entities.Enums.RequestStatus;
import com.lifelink.blood_donation.Entities.Enums.UrgencyLevel;
import com.lifelink.blood_donation.Entities.User;
import com.lifelink.blood_donation.Services.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private final GoogleMapsProperties googleMapsProperties;

    // Accessible to PATIENT and ADMIN — see SecurityConfig note below.
    @GetMapping("/search/donors")
    public String searchDonors(@RequestParam(required = false) BloodGroup bloodGroup,
                               @RequestParam(required = false) String district,
                               Model model) {
        model.addAttribute("bloodGroups", BloodGroup.values());
        model.addAttribute("selectedBloodGroup", bloodGroup);
        model.addAttribute("selectedDistrict", district);

        List<User> donors = searchService.searchDonors(bloodGroup, district);
        model.addAttribute("donors", donors);
        model.addAttribute("mapsApiKey", googleMapsProperties.getApiKey());
        model.addAttribute("donorMarkers", searchService.toDonorMapMarkers(donors));
        model.addAttribute("defaultLat", googleMapsProperties.getDefaultLat());
        model.addAttribute("defaultLng", googleMapsProperties.getDefaultLng());
        model.addAttribute("defaultZoom", googleMapsProperties.getDefaultZoom());

        return "search/donor-search";
    }
}