package com.lifelink.blood_donation;

import com.lifelink.blood_donation.Services.RequestAssignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * REPLACES AssignmentCascadeScheduler (delete that file).
 * Goal: every minute, flag assigned donors who have not confirmed within their urgency window so the
 * admin sees "not confirmed - call donor". It NEVER reassigns anyone by itself; a human decides.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AssignmentEscalationScheduler {

    private final RequestAssignService requestAssignService;

    @Scheduled(fixedDelayString = "${lifelink.scheduler.escalation-interval-ms:60000}")
    public void checkForUnconfirmedAssignments() {
        int flagged = requestAssignService.escalateStaleAssignments();
        if (flagged > 0) {
            log.warn("{} assignment(s) not confirmed in time - flagged for admin attention", flagged);
        }
    }
}