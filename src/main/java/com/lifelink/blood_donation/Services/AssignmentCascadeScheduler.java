package com.lifelink.blood_donation.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AssignmentCascadeScheduler {

    private final RequestAssignService requestAssignService;

    @Value("${lifelink.cascade.window-minutes:30}")
    private int cascadeWindowMinutes;

    @Scheduled(fixedRateString = "${lifelink.cascade.check-interval-ms:300000}")
    public void checkForStaleAssignments() {
        System.out.println("Cascade check running at " + java.time.LocalDateTime.now());
        requestAssignService.cascadeStaleAssignments(cascadeWindowMinutes);
    }
}
