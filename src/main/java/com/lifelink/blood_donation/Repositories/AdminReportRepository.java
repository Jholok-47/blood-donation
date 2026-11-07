package com.lifelink.blood_donation.Repositories;

import com.lifelink.blood_donation.Entities.BloodRequest;
import com.lifelink.blood_donation.Entities.Enums.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Read-only aggregate queries for the admin module (counts, groupings, integrity checks).
 * A plain @Repository over EntityManager (not Spring Data) so the existing entity repositories stay untouched.
 * Every method is a pure read; callers are @Transactional(readOnly = true).
 */
@Repository
public class AdminReportRepository {

    @PersistenceContext
    private EntityManager em;

    // ---------- helpers ----------

    private long scalar(String jpql, Object... params) {
        TypedQuery<Long> q = em.createQuery(jpql, Long.class);
        for (int i = 0; i < params.length; i++) q.setParameter(i + 1, params[i]);
        return q.getSingleResult();
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> rows(String jpql, Object... params) {
        Query q = em.createQuery(jpql);
        for (int i = 0; i < params.length; i++) q.setParameter(i + 1, params[i]);
        return q.getResultList();
    }

    // ---------- headline counts ----------

    public long countUsersByRole(Role role) {
        return scalar("select count(u) from User u where u.role = ?1", role);
    }

    public long countDonorsVerified(boolean verified) {
        return scalar("select count(u) from User u where u.role = ?1 and u.verified = ?2", Role.DONOR, verified);
    }

    /** Verified, availability on, not suspended: the donors an open call can actually reach. */
    public long countAvailableDonors() {
        return scalar("select count(u) from User u where u.role = ?1 and u.verified = true "
                + "and u.available = true and u.active = true", Role.DONOR);
    }

    public long countSuspendedUsers() {
        return scalar("select count(u) from User u where u.active = false");
    }

    public long countRequestsByStatus(RequestStatus status) {
        return scalar("select count(r) from BloodRequest r where r.status = ?1", status);
    }

    public long countAssignmentsByStatus(AssignStatus status) {
        return scalar("select count(a) from RequestAssign a where a.status = ?1", status);
    }

    public long countUnconfirmedAssignments() {
        return scalar("select count(a) from RequestAssign a where a.status = ?1 "
                + "and a.respondedAt is null and a.escalatedAt is not null", AssignStatus.ACTIVE);
    }

    public long countDonations(LocalDate from, LocalDate toInclusive) {
        return scalar("select count(dh) from DonationHistory dh where dh.donationDate between ?1 and ?2",
                from, toInclusive);
    }

    // ---------- report aggregates ----------

    /** (RequestStatus, Long) for requests created in [from, toExclusive). */
    public List<Object[]> requestStatusCounts(LocalDateTime from, LocalDateTime toExclusive) {
        return rows("select r.status, count(r) from BloodRequest r "
                + "where r.createdAt >= ?1 and r.createdAt < ?2 group by r.status", from, toExclusive);
    }

    /** (BloodGroup, Long units) requested in the range, rejected requests excluded. */
    public List<Object[]> requestedUnitsByBloodGroup(LocalDateTime from, LocalDateTime toExclusive) {
        return rows("select r.bloodGroup, sum(r.quantity) from BloodRequest r "
                        + "where r.createdAt >= ?1 and r.createdAt < ?2 and r.status <> ?3 group by r.bloodGroup",
                from, toExclusive, RequestStatus.REJECTED);
    }

    /** (RequestStatus-independent) donation dates in range, for time bucketing in Java. */
    @SuppressWarnings("unchecked")
    public List<LocalDate> donationDates(LocalDate from, LocalDate toInclusive) {
        return em.createQuery("select dh.donationDate from DonationHistory dh "
                        + "where dh.donationDate between ?1 and ?2")
                .setParameter(1, from).setParameter(2, toInclusive).getResultList();
    }

    /** (BloodGroup of the donor, Long) donations in range. */
    public List<Object[]> donationsByDonorBloodGroup(LocalDate from, LocalDate toInclusive) {
        return rows("select d.bloodGroup, count(dh) from DonationHistory dh join dh.donor d "
                + "where dh.donationDate between ?1 and ?2 group by d.bloodGroup", from, toInclusive);
    }

    /** (district of the request, Long) donations in range. */
    public List<Object[]> donationsByDistrict(LocalDate from, LocalDate toInclusive) {
        return rows("select r.district, count(dh) from DonationHistory dh join dh.bloodRequest r "
                + "where dh.donationDate between ?1 and ?2 group by r.district", from, toInclusive);
    }

    /** (BloodGroup, Long) verified, available, non-suspended donors per exact blood group. */
    public List<Object[]> availableDonorsByBloodGroup() {
        return rows("select u.bloodGroup, count(u) from User u where u.role = ?1 and u.verified = true "
                + "and u.available = true and u.active = true group by u.bloodGroup", Role.DONOR);
    }

    /** (AssignStatus, AssignEndReason, Long) for assignments created in range. */
    public List<Object[]> assignmentOutcomeCounts(LocalDateTime from, LocalDateTime toExclusive) {
        return rows("select a.status, a.endReason, count(a) from RequestAssign a "
                + "where a.assignedAt >= ?1 and a.assignedAt < ?2 group by a.status, a.endReason", from, toExclusive);
    }

    /** (InterestStatus, Long) open-call rows created in range. */
    public List<Object[]> interestStatusCounts(LocalDateTime from, LocalDateTime toExclusive) {
        return rows("select i.status, count(i) from RequestInterest i "
                + "where i.createdAt >= ?1 and i.createdAt < ?2 group by i.status", from, toExclusive);
    }

    /** Minutes between "call sent" and "donor answered" for rows created in range. */
    public List<Long> responseDelaysMinutes(LocalDateTime from, LocalDateTime toExclusive) {
        List<Long> out = new ArrayList<>();
        for (Object[] r : rows("select i.createdAt, i.respondedAt from RequestInterest i "
                + "where i.respondedAt is not null and i.createdAt >= ?1 and i.createdAt < ?2", from, toExclusive)) {
            out.add(Math.max(0, Duration.between((LocalDateTime) r[0], (LocalDateTime) r[1]).toMinutes()));
        }
        return out;
    }

    /** Hours from request creation to its last donation, for FULFILLED requests created in range. */
    public List<Double> fulfilmentDurationsHours(LocalDateTime from, LocalDateTime toExclusive) {
        List<Double> out = new ArrayList<>();
        for (Object[] r : rows("select r.createdAt, max(dh.createdAt) from DonationHistory dh join dh.bloodRequest r "
                        + "where r.status = ?1 and r.createdAt >= ?2 and r.createdAt < ?3 group by r.id, r.createdAt",
                RequestStatus.FULFILLED, from, toExclusive)) {
            out.add(Duration.between((LocalDateTime) r[0], (LocalDateTime) r[1]).toMinutes() / 60.0);
        }
        return out;
    }

    /** (requestId, quantity, donations) for FULFILLED requests created in range; donations < quantity = closed early. */
    public List<Object[]> fulfilledRequestDonationCounts(LocalDateTime from, LocalDateTime toExclusive) {
        return rows("select r.id, r.quantity, count(dh) from BloodRequest r "
                        + "left join DonationHistory dh on dh.bloodRequest.id = r.id "
                        + "where r.status = ?1 and r.createdAt >= ?2 and r.createdAt < ?3 group by r.id, r.quantity",
                RequestStatus.FULFILLED, from, toExclusive);
    }

    /** (donation date, requestId, requested group, urgency, request district, donorId, donor name, donor group). */
    public List<Object[]> donationExportRows(LocalDate from, LocalDate toInclusive) {
        return rows("select dh.donationDate, r.id, r.bloodGroup, r.urgency, r.district, d.id, d.fullName, d.bloodGroup "
                + "from DonationHistory dh join dh.bloodRequest r join dh.donor d "
                + "where dh.donationDate between ?1 and ?2 order by dh.donationDate, dh.id", from, toInclusive);
    }

    // ---------- integrity / health checks (all-time, current state) ----------

    @SuppressWarnings("unchecked")
    public List<BloodRequest> requestsByStatus(Collection<RequestStatus> statuses) {
        return em.createQuery("select r from BloodRequest r where r.status in (?1) order by r.createdAt asc")
                .setParameter(1, statuses).getResultList();
    }

    /** (requestId, Long) number of donors currently INTERESTED per request. */
    public Map<Long, Long> interestedCountsByRequest(Collection<Long> requestIds) {
        Map<Long, Long> out = new HashMap<>();
        if (requestIds.isEmpty()) return out;
        for (Object[] r : rows("select i.bloodRequest.id, count(i) from RequestInterest i "
                        + "where i.status = ?1 and i.bloodRequest.id in (?2) group by i.bloodRequest.id",
                InterestStatus.INTERESTED, requestIds)) {
            out.put((Long) r[0], (Long) r[1]);
        }
        return out;
    }

    /** (requestId, AssignStatus, Long) ACTIVE and COMPLETED assignment counts per request. */
    public List<Object[]> assignmentCountsByRequest(Collection<Long> requestIds) {
        if (requestIds.isEmpty()) return List.of();
        return rows("select a.bloodRequest.id, a.status, count(a) from RequestAssign a "
                        + "where a.status in (?1) and a.bloodRequest.id in (?2) group by a.bloodRequest.id, a.status",
                List.of(AssignStatus.ACTIVE, AssignStatus.COMPLETED), requestIds);
    }

    /** (assignmentId, requestId, donor name, assignedAt) ACTIVE, never confirmed, already flagged by the scheduler. */
    public List<Object[]> escalatedUnconfirmedAssignments() {
        return rows("select a.id, a.bloodRequest.id, a.donor.fullName, a.assignedAt from RequestAssign a "
                + "where a.status = ?1 and a.respondedAt is null and a.escalatedAt is not null "
                + "order by a.assignedAt asc", AssignStatus.ACTIVE);
    }

    /** (assignmentId, requestId, donor name, assignedAt) ACTIVE assignments older than the cut-off. */
    public List<Object[]> activeAssignmentsOlderThan(LocalDateTime cutoff) {
        return rows("select a.id, a.bloodRequest.id, a.donor.fullName, a.assignedAt from RequestAssign a "
                + "where a.status = ?1 and a.assignedAt < ?2 order by a.assignedAt asc", AssignStatus.ACTIVE, cutoff);
    }

    /** (donorId, donor name, Long) donors holding more than one ACTIVE assignment (service-layer rule violated). */
    public List<Object[]> donorsWithMultipleActiveAssignments() {
        return rows("select a.donor.id, a.donor.fullName, count(a) from RequestAssign a where a.status = ?1 "
                + "group by a.donor.id, a.donor.fullName having count(a) > 1", AssignStatus.ACTIVE);
    }

    /** Assignment ids that are COMPLETED but have no DonationHistory row. */
    @SuppressWarnings("unchecked")
    public List<Long> completedAssignmentsWithoutDonation() {
        return em.createQuery("select a.id from RequestAssign a where a.status = ?1 and not exists "
                        + "(select 1 from DonationHistory dh where dh.requestAssign.id = a.id)")
                .setParameter(1, AssignStatus.COMPLETED).getResultList();
    }

    /** FULFILLED requests that have no donation recorded at all. */
    @SuppressWarnings("unchecked")
    public List<Long> fulfilledRequestsWithoutDonation() {
        return em.createQuery("select r.id from BloodRequest r where r.status = ?1 and not exists "
                        + "(select 1 from DonationHistory dh where dh.bloodRequest.id = r.id)")
                .setParameter(1, RequestStatus.FULFILLED).getResultList();
    }

    /** (requestId) requests that are not open (PENDING/REJECTED/CANCELLED) yet still hold an ACTIVE assignment. */
    @SuppressWarnings("unchecked")
    public List<Long> closedRequestsWithActiveAssignments() {
        return em.createQuery("select distinct r.id from RequestAssign a join a.bloodRequest r "
                        + "where a.status = ?1 and r.status in (?2)")
                .setParameter(1, AssignStatus.ACTIVE)
                .setParameter(2, List.of(RequestStatus.PENDING, RequestStatus.REJECTED, RequestStatus.CANCELLED))
                .getResultList();
    }

    /** (donorId, donor name) suspended donors that still hold an ACTIVE assignment. */
    public List<Object[]> suspendedDonorsWithActiveAssignments() {
        return rows("select distinct a.donor.id, a.donor.fullName from RequestAssign a "
                + "where a.status = ?1 and a.donor.active = false", AssignStatus.ACTIVE);
    }

    public long countVerifiedDonorsMissingLocation() {
        return scalar("select count(u) from User u where u.role = ?1 and u.verified = true "
                + "and (u.latitude is null or u.longitude is null)", Role.DONOR);
    }

    public long countUnverifiedDonorsWaitingSince(LocalDateTime cutoff) {
        return scalar("select count(u) from User u where u.role = ?1 and u.verified = false and u.createdAt < ?2",
                Role.DONOR, cutoff);
    }

    public long countAbandonedProfiles(LocalDateTime cutoff) {
        return scalar("select count(u) from User u where u.profileCompleted = false and u.createdAt < ?1", cutoff);
    }
}
