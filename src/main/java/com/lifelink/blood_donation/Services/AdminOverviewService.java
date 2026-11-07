package com.lifelink.blood_donation.Services;

import com.lifelink.blood_donation.DTO.AdminOverviewDto;
import com.lifelink.blood_donation.DTO.SystemIssueDto;
import com.lifelink.blood_donation.Entities.Enums.AssignStatus;
import com.lifelink.blood_donation.Entities.Enums.IssueSeverity;
import com.lifelink.blood_donation.Entities.Enums.RequestStatus;
import com.lifelink.blood_donation.Entities.Enums.Role;
import com.lifelink.blood_donation.Repositories.AdminReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Builds the headline numbers of the Control Center. */
@Service
@RequiredArgsConstructor
public class AdminOverviewService {

    private final AdminReportRepository repo;
    private final MlRankingClient mlRankingClient;

    @Transactional(readOnly = true)
    public AdminOverviewDto build(List<SystemIssueDto> issues) {
        LocalDate today = LocalDate.now();
        LocalDateTime from30 = today.minusDays(29).atStartOfDay();
        LocalDateTime toExclusive = today.plusDays(1).atStartOfDay();

        long requests30 = 0, fulfilled30 = 0, rejected30 = 0;
        for (Object[] r : repo.requestStatusCounts(from30, toExclusive)) {
            long n = (Long) r[1];
            requests30 += n;
            if (r[0] == RequestStatus.FULFILLED) fulfilled30 = n;
            if (r[0] == RequestStatus.REJECTED) rejected30 = n;
        }
        long decidable = requests30 - rejected30;
        double rate = decidable == 0 ? 0 : fulfilled30 * 100.0 / decidable;

        boolean mlEnabled = mlRankingClient.isEnabled();
        var health = mlEnabled ? mlRankingClient.health() : Optional.empty();
        String version = health.map(h -> String.valueOf(((java.util.Map<?, ?>) h).get("model_version"))).orElse(null);

        return AdminOverviewDto.builder()
                .pendingRequests(repo.countRequestsByStatus(RequestStatus.PENDING))
                .openRequests(repo.countRequestsByStatus(RequestStatus.APPROVED))
                .assignedRequests(repo.countRequestsByStatus(RequestStatus.ASSIGNED))
                .activeAssignments(repo.countAssignmentsByStatus(AssignStatus.ACTIVE))
                .unconfirmedAssignments(repo.countUnconfirmedAssignments())
                .verifiedDonors(repo.countDonorsVerified(true))
                .availableDonors(repo.countAvailableDonors())
                .unverifiedDonors(repo.countDonorsVerified(false))
                .patients(repo.countUsersByRole(Role.PATIENT))
                .suspendedUsers(repo.countSuspendedUsers())
                .requests30d(requests30)
                .fulfilled30d(fulfilled30)
                .fulfilmentRate30d(rate)
                .donations30d(repo.countDonations(today.minusDays(29), today))
                .donationsPrev30d(repo.countDonations(today.minusDays(59), today.minusDays(30)))
                .criticalIssues(issues.stream().filter(i -> i.getSeverity() == IssueSeverity.CRITICAL).count())
                .warningIssues(issues.stream().filter(i -> i.getSeverity() == IssueSeverity.WARNING).count())
                .mlEnabled(mlEnabled)
                .mlReachable(health.isPresent())
                .mlModelVersion(version)
                .build();
    }
}
