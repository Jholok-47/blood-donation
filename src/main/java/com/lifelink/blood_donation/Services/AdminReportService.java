package com.lifelink.blood_donation.Services;

import com.lifelink.blood_donation.DTO.AdminReportDto;
import com.lifelink.blood_donation.DTO.ChartSeriesDto;
import com.lifelink.blood_donation.DTO.SupplyDemandRowDto;
import com.lifelink.blood_donation.Entities.Enums.*;
import com.lifelink.blood_donation.Repositories.AdminReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Builds the Reports page figures and the donations CSV export for a date range. */
@Service
@RequiredArgsConstructor
public class AdminReportService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    private final AdminReportRepository repo;

    @Transactional(readOnly = true)
    public AdminReportDto build(LocalDate from, LocalDate to) {
        LocalDateTime fromTs = from.atStartOfDay();
        LocalDateTime toTs = to.plusDays(1).atStartOfDay();

        // ---- request outcomes ----
        Map<RequestStatus, Long> st = new EnumMap<>(RequestStatus.class);
        for (Object[] r : repo.requestStatusCounts(fromTs, toTs)) st.put((RequestStatus) r[0], num(r[1]));
        long created = st.values().stream().mapToLong(Long::longValue).sum();
        long fulfilled = st.getOrDefault(RequestStatus.FULFILLED, 0L);
        long rejected = st.getOrDefault(RequestStatus.REJECTED, 0L);
        long cancelled = st.getOrDefault(RequestStatus.CANCELLED, 0L);
        long stillOpen = st.getOrDefault(RequestStatus.PENDING, 0L) + st.getOrDefault(RequestStatus.APPROVED, 0L)
                + st.getOrDefault(RequestStatus.ASSIGNED, 0L);
        long closedEarly = repo.fulfilledRequestDonationCounts(fromTs, toTs).stream()
                .filter(r -> num(r[2]) < num(r[1])).count();
        double fulfilmentRate = created - rejected <= 0 ? 0 : fulfilled * 100.0 / (created - rejected);
        Double avgHours = average(repo.fulfilmentDurationsHours(fromTs, toTs));

        Map<BloodGroup, Long> requested = new EnumMap<>(BloodGroup.class);
        long unitsRequested = 0;
        for (Object[] r : repo.requestedUnitsByBloodGroup(fromTs, toTs)) {
            if (r[0] == null) continue;
            requested.put((BloodGroup) r[0], num(r[1]));
            unitsRequested += num(r[1]);
        }

        // ---- donations ----
        boolean daily = ChronoUnit.DAYS.between(from, to) <= 62;
        Map<String, Long> buckets = new LinkedHashMap<>();
        if (daily) {
            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) buckets.put(d.format(DAY), 0L);
        } else {
            for (YearMonth m = YearMonth.from(from); !m.isAfter(YearMonth.from(to)); m = m.plusMonths(1))
                buckets.put(m.format(MONTH), 0L);
        }
        List<LocalDate> dates = repo.donationDates(from, to);
        for (LocalDate d : dates) buckets.merge(daily ? d.format(DAY) : YearMonth.from(d).format(MONTH), 1L, Long::sum);

        Map<BloodGroup, Long> donated = new EnumMap<>(BloodGroup.class);
        for (Object[] r : repo.donationsByDonorBloodGroup(from, to)) {
            if (r[0] != null) donated.put((BloodGroup) r[0], num(r[1]));
        }
        Map<BloodGroup, Long> pool = new EnumMap<>(BloodGroup.class);
        for (Object[] r : repo.availableDonorsByBloodGroup()) {
            if (r[0] != null) pool.put((BloodGroup) r[0], num(r[1]));
        }

        List<String> groupLabels = new ArrayList<>();
        List<Long> groupValues = new ArrayList<>();
        List<SupplyDemandRowDto> supplyDemand = new ArrayList<>();
        for (BloodGroup g : BloodGroup.values()) {
            long req = requested.getOrDefault(g, 0L);
            long don = donated.getOrDefault(g, 0L);
            long avail = pool.getOrDefault(g, 0L);
            groupLabels.add(label(g));
            groupValues.add(don);
            supplyDemand.add(SupplyDemandRowDto.builder().bloodGroup(label(g)).requestedUnits(req)
                    .donatedUnits(don).availableDonors(avail).shortage(req > 0 && avail < req).build());
        }

        List<Object[]> districtRows = new ArrayList<>(repo.donationsByDistrict(from, to));
        districtRows.sort((a, b) -> Long.compare(num(b[1]), num(a[1])));
        List<String> districtLabels = new ArrayList<>();
        List<Long> districtValues = new ArrayList<>();
        for (Object[] r : districtRows.stream().limit(10).toList()) {
            districtLabels.add(r[0] == null || r[0].toString().isBlank() ? "Unspecified" : r[0].toString());
            districtValues.add(num(r[1]));
        }

        // ---- assignment outcomes ----
        long aCompleted = 0, aActive = 0, aWithdrew = 0, aUnresponsive = 0, aAdminCancelled = 0;
        for (Object[] r : repo.assignmentOutcomeCounts(fromTs, toTs)) {
            AssignStatus s = (AssignStatus) r[0];
            AssignEndReason e = (AssignEndReason) r[1];
            long n = num(r[2]);
            if (s == AssignStatus.COMPLETED) aCompleted += n;
            else if (s == AssignStatus.ACTIVE) aActive += n;
            else if (e == AssignEndReason.UNRESPONSIVE) aUnresponsive += n;
            else if (e == AssignEndReason.DONOR_WITHDREW || s == AssignStatus.DECLINED) aWithdrew += n;
            else aAdminCancelled += n;
        }
        long decided = aCompleted + aWithdrew + aUnresponsive;
        double completionRate = decided == 0 ? 0 : aCompleted * 100.0 / decided;

        // ---- donor responsiveness ----
        long accepted = 0, declined = 0, noAnswer = 0, sent = 0;
        for (Object[] r : repo.interestStatusCounts(fromTs, toTs)) {
            InterestStatus s = (InterestStatus) r[0];
            long n = num(r[1]);
            sent += n;
            if (s == InterestStatus.NOTIFIED) noAnswer += n;
            else if (s == InterestStatus.DECLINED) declined += n;
            else accepted += n; // INTERESTED and WITHDRAWN both started as an acceptance
        }
        double responseRate = sent == 0 ? 0 : (sent - noAnswer) * 100.0 / sent;
        List<Long> delays = repo.responseDelaysMinutes(fromTs, toTs);

        return AdminReportDto.builder()
                .from(from).to(to)
                .requestsCreated(created).requestsFulfilled(fulfilled).requestsClosedEarly(closedEarly)
                .requestsRejected(rejected).requestsCancelled(cancelled).requestsStillOpen(stillOpen)
                .fulfilmentRate(fulfilmentRate).unitsRequested(unitsRequested).avgHoursToFulfil(avgHours)
                .donations(dates.size())
                .donationsOverTime(new ChartSeriesDto(new ArrayList<>(buckets.keySet()), new ArrayList<>(buckets.values())))
                .donationsByBloodGroup(new ChartSeriesDto(groupLabels, groupValues))
                .donationsByDistrict(new ChartSeriesDto(districtLabels, districtValues))
                .dailyBuckets(daily)
                .supplyDemand(supplyDemand)
                .assignCompleted(aCompleted).assignActive(aActive).assignWithdrew(aWithdrew)
                .assignUnresponsive(aUnresponsive).assignAdminCancelled(aAdminCancelled)
                .assignCompletionRate(completionRate)
                .callsSent(sent).callsAccepted(accepted).callsDeclined(declined).callsNoAnswer(noAnswer)
                .callResponseRate(responseRate)
                .avgResponseMinutes(average(delays.stream().map(Long::doubleValue).toList()))
                .medianResponseMinutes(median(delays))
                .build();
    }

    /** CSV of donations in the range. No phone, email or address is exported (data minimisation). */
    @Transactional(readOnly = true)
    public String donationsCsv(LocalDate from, LocalDate to) {
        StringBuilder sb = new StringBuilder(
                "Donation date,Request ID,Requested blood group,Urgency,District,Donor ID,Donor name,Donor blood group\r\n");
        for (Object[] r : repo.donationExportRows(from, to)) {
            sb.append(csv(r[0])).append(',').append(csv(r[1])).append(',')
                    .append(csv(r[2] instanceof BloodGroup g ? label(g) : r[2])).append(',')
                    .append(csv(r[3])).append(',').append(csv(r[4])).append(',').append(csv(r[5])).append(',')
                    .append(csv(r[6])).append(',').append(csv(r[7] instanceof BloodGroup g ? label(g) : r[7]))
                    .append("\r\n");
        }
        return sb.toString();
    }

    // ---------- helpers ----------

    private static String label(BloodGroup g) {
        return g.name().replace("_POSITIVE", "+").replace("_NEGATIVE", "-");
    }

    private static long num(Object o) {
        return o == null ? 0L : ((Number) o).longValue();
    }

    private static Double average(List<Double> values) {
        return values.isEmpty() ? null : values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private static Double median(List<Long> values) {
        if (values.isEmpty()) return null;
        List<Long> s = new ArrayList<>(values);
        Collections.sort(s);
        int n = s.size();
        return n % 2 == 1 ? s.get(n / 2).doubleValue() : (s.get(n / 2 - 1) + s.get(n / 2)) / 2.0;
    }

    /** Quotes the cell and neutralises spreadsheet formula injection (=, +, -, @ at the start). */
    private static String csv(Object o) {
        if (o == null) return "";
        String v = o.toString();
        if (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0) v = "'" + v;
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }
}
