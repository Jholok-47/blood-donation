package com.lifelink.blood_donation.Services;

import com.lifelink.blood_donation.DTO.SystemIssueDto;
import com.lifelink.blood_donation.Entities.BloodRequest;
import com.lifelink.blood_donation.Entities.Enums.IssueSeverity;
import com.lifelink.blood_donation.Entities.Enums.RequestStatus;
import com.lifelink.blood_donation.Entities.Enums.UrgencyLevel;
import com.lifelink.blood_donation.Repositories.AdminReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Runs the system health and data-integrity checks and returns the problems found, most severe first.
 * Everything is derived from current data on every call; nothing is stored.
 */
@Service
@RequiredArgsConstructor
public class AdminIssueService {

    private final AdminReportRepository repo;
    private final MlRankingClient mlRankingClient;

    @Value("${lifelink.admin.pending-sla-hours:24}")
    private int pendingSlaHours;
    @Value("${lifelink.admin.no-response-hours:6}")
    private int noResponseHours;
    @Value("${lifelink.admin.stuck-assignment-hours:24}")
    private int stuckAssignmentHours;
    @Value("${lifelink.admin.unverified-donor-days:2}")
    private int unverifiedDonorDays;
    @Value("${lifelink.admin.abandoned-profile-days:7}")
    private int abandonedProfileDays;

    @Transactional(readOnly = true)
    public List<SystemIssueDto> detectIssues() {
        List<SystemIssueDto> issues = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // ---- request flow ----
        List<BloodRequest> pending = repo.requestsByStatus(List.of(RequestStatus.PENDING));
        List<Long> latePending = pending.stream()
                .filter(r -> r.getCreatedAt().isBefore(now.minus(limit(pendingSlaHours, r.getUrgency()))))
                .map(BloodRequest::getId).toList();
        add(issues, latePending.size(), IssueSeverity.CRITICAL, "Request flow",
                "Requests waiting for admin approval too long",
                "Not decided within " + pendingSlaHours + " h (urgent requests: " + Math.max(1, pendingSlaHours / 4)
                        + " h). Requests " + ids(latePending) + ".", "/admin/requests");

        List<BloodRequest> open = repo.requestsByStatus(List.of(RequestStatus.APPROVED, RequestStatus.ASSIGNED));
        List<Long> openIds = open.stream().map(BloodRequest::getId).toList();
        Map<Long, Long> interested = repo.interestedCountsByRequest(openIds);
        Map<Long, long[]> assigned = new HashMap<>(); // [active, completed]
        for (Object[] r : repo.assignmentCountsByRequest(openIds)) {
            long[] c = assigned.computeIfAbsent((Long) r[0], k -> new long[2]);
            c[r[1].toString().equals("ACTIVE") ? 0 : 1] = (Long) r[2];
        }

        List<Long> noResponse = new ArrayList<>();
        List<Long> overAssigned = new ArrayList<>();
        List<Long> mismatch = new ArrayList<>();
        for (BloodRequest r : open) {
            long[] c = assigned.getOrDefault(r.getId(), new long[2]);
            long used = c[0] + c[1];
            long qty = r.getQuantity();
            if (r.getStatus() == RequestStatus.APPROVED && used == 0
                    && interested.getOrDefault(r.getId(), 0L) == 0
                    && r.getCreatedAt().isBefore(now.minus(limit(noResponseHours, r.getUrgency())))) {
                noResponse.add(r.getId());
            }
            if (used > qty) overAssigned.add(r.getId());
            if ((r.getStatus() == RequestStatus.ASSIGNED && used < qty)
                    || (r.getStatus() == RequestStatus.APPROVED && used >= qty)) mismatch.add(r.getId());
        }
        add(issues, noResponse.size(), IssueSeverity.WARNING, "Request flow",
                "Approved requests with no donor response",
                "No donor has accepted and none is assigned after " + noResponseHours
                        + " h (urgent: " + Math.max(1, noResponseHours / 4) + " h). Consider \"Notify more donors\". Requests "
                        + ids(noResponse) + ".", "/admin/requests");

        // ---- assignments ----
        List<Object[]> escalated = repo.escalatedUnconfirmedAssignments();
        add(issues, escalated.size(), IssueSeverity.CRITICAL, "Assignments",
                "Selected donors have not confirmed",
                "Past the urgency window. Call the donor, then confirm or release. "
                        + describeAssignments(escalated), "/admin/requests");

        List<Object[]> stuck = repo.activeAssignmentsOlderThan(now.minusHours(stuckAssignmentHours));
        add(issues, stuck.size(), IssueSeverity.WARNING, "Assignments",
                "Assignments open for more than " + stuckAssignmentHours + " h without a recorded donation",
                "Mark the donation as complete or cancel the assignment. " + describeAssignments(stuck), "/admin/requests");

        // ---- data integrity ----
        List<Object[]> multi = repo.donorsWithMultipleActiveAssignments();
        add(issues, multi.size(), IssueSeverity.CRITICAL, "Data integrity",
                "Donors holding more than one active assignment",
                "Breaks the one-active-assignment rule. Donors: "
                        + multi.stream().limit(5).map(r -> r[1] + " (#" + r[0] + ")").collect(Collectors.joining(", ")) + ".",
                null);

        add(issues, overAssigned.size(), IssueSeverity.CRITICAL, "Data integrity",
                "Requests with more assignments than units needed",
                "Requests " + ids(overAssigned) + ".", "/admin/requests");

        add(issues, mismatch.size(), IssueSeverity.CRITICAL, "Data integrity",
                "Request status does not match its assignments",
                "ASSIGNED with a missing donor, or APPROVED with all units already covered. Requests "
                        + ids(mismatch) + ".", "/admin/requests");

        List<Long> noDonation = repo.completedAssignmentsWithoutDonation();
        add(issues, noDonation.size(), IssueSeverity.CRITICAL, "Data integrity",
                "Completed assignments without a donation record",
                "Assignments " + ids(noDonation) + ".", null);

        List<Long> fulfilledEmpty = repo.fulfilledRequestsWithoutDonation();
        add(issues, fulfilledEmpty.size(), IssueSeverity.CRITICAL, "Data integrity",
                "Fulfilled requests with no donation recorded",
                "Requests " + ids(fulfilledEmpty) + ".", "/admin/requests");

        List<Long> closedActive = repo.closedRequestsWithActiveAssignments();
        add(issues, closedActive.size(), IssueSeverity.CRITICAL, "Data integrity",
                "Pending, rejected or cancelled requests that still hold an active assignment",
                "Requests " + ids(closedActive) + ".", "/admin/requests");

        List<Object[]> suspended = repo.suspendedDonorsWithActiveAssignments();
        add(issues, suspended.size(), IssueSeverity.CRITICAL, "Data integrity",
                "Suspended donors with an active assignment",
                suspended.stream().limit(5).map(r -> r[1] + " (#" + r[0] + ")").collect(Collectors.joining(", ")) + ".",
                "/admin/users?active=false&role=DONOR");

        // ---- donors / accounts ----
        long unverified = repo.countUnverifiedDonorsWaitingSince(now.minusDays(unverifiedDonorDays));
        add(issues, unverified, IssueSeverity.WARNING, "Donors",
                "Donors waiting for verification",
                "Registered more than " + unverifiedDonorDays + " days ago and still unverified.", "/admin/donors/unverified");

        long noLocation = repo.countVerifiedDonorsMissingLocation();
        add(issues, noLocation, IssueSeverity.WARNING, "Donors",
                "Verified donors without map coordinates",
                "Proximity ranking cannot use real distance for these donors (district fallback only).",
                "/admin/users?role=DONOR&flag=missingLocation");

        long abandoned = repo.countAbandonedProfiles(now.minusDays(abandonedProfileDays));
        add(issues, abandoned, IssueSeverity.INFO, "Accounts",
                "Accounts that never completed their profile",
                "Created more than " + abandonedProfileDays + " days ago. They can not use the platform.",
                "/admin/users?flag=incompleteProfile");

        // ---- system ----
        if (mlRankingClient.isEnabled() && mlRankingClient.health().isEmpty()) {
            issues.add(SystemIssueDto.builder().severity(IssueSeverity.WARNING).category("System")
                    .title("AI ranking service is unreachable")
                    .detail("Donor ranking falls back to the rule-based score. Start the ml-service (port 8001).")
                    .count(1).link("/admin/ml/status").build());
        }

        issues.sort(Comparator.comparing(SystemIssueDto::getSeverity)
                .thenComparing(Comparator.comparingLong(SystemIssueDto::getCount).reversed()));
        return issues;
    }

    private static void add(List<SystemIssueDto> list, long count, IssueSeverity sev, String category,
                            String title, String detail, String link) {
        if (count <= 0) return;
        list.add(SystemIssueDto.builder().severity(sev).category(category).title(title)
                .detail(detail).count(count).link(link).build());
    }

    /** Urgent requests (CRITICAL/HIGH) get a quarter of the normal allowance, at least one hour. */
    private static Duration limit(int hours, UrgencyLevel urgency) {
        boolean urgent = urgency == UrgencyLevel.CRITICAL || urgency == UrgencyLevel.HIGH;
        return Duration.ofHours(urgent ? Math.max(1, hours / 4) : hours);
    }

    private static String ids(List<Long> ids) {
        if (ids.isEmpty()) return "";
        String head = ids.stream().limit(5).map(i -> "#" + i).collect(Collectors.joining(", "));
        return ids.size() > 5 ? head + " (+" + (ids.size() - 5) + " more)" : head;
    }

    private static String describeAssignments(List<Object[]> rows) {
        String head = rows.stream().limit(4)
                .map(r -> r[2] + " on request #" + r[1]).collect(Collectors.joining("; "));
        return rows.size() > 4 ? head + " (+" + (rows.size() - 4) + " more)." : head + ".";
    }
}
