package com.lifelink.blood_donation.Controllers;

import com.lifelink.blood_donation.Services.MlRankingClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.stereotype.Controller;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GOAL: tiny admin-only JSON check (GET /admin/ml/status) to see whether the AI service is
 * reachable and which model version is loaded. Useful for testing and for the defense demo.*/

@Controller
@RequiredArgsConstructor
public class MlStatusController {

    private final MlRankingClient client;

    @GetMapping("/admin/ml/status")
    @ResponseBody
    public Map<String, Object> status() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", client.isEnabled());
        Map<String, Object> health = client.health().orElse(null);
        out.put("reachable", health != null);
        out.put("service", health);
        return out;
    }
}
