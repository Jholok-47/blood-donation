package com.lifelink.blood_donation.Repositories;

import com.lifelink.blood_donation.Entities.Enums.InterestStatus;
import com.lifelink.blood_donation.Entities.RequestInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Goal: data access for donor responses to broadcast requests.
 * findByBloodRequestIdAndDonorId is safe as Optional because (request, donor) is UNIQUE.
 */
public interface RequestInterestRepository extends JpaRepository<RequestInterest, Long> {

    Optional<RequestInterest> findByBloodRequestIdAndDonorId(Long bloodRequestId, Long donorId);

    List<RequestInterest> findByBloodRequestId(Long bloodRequestId);

    List<RequestInterest> findByBloodRequestIdAndStatus(Long bloodRequestId, InterestStatus status);

    /** Batched enrichment for list pages (group in Java, no per-row queries). */
    List<RequestInterest> findByBloodRequestIdInAndStatus(Collection<Long> bloodRequestIds, InterestStatus status);

    List<RequestInterest> findByDonorIdOrderByCreatedAtDesc(Long donorId);

    /** FK-safety check for donor hard-delete (Module 12 convention). */
    boolean existsByDonorId(Long donorId);

    /** Donor dashboard badge: open calls the donor has not answered yet. */
    @Query("""
           select count(i) from RequestInterest i
           where i.donor.id = :donorId
             and i.status = com.lifelink.blood_donation.Entities.Enums.InterestStatus.NOTIFIED
             and i.bloodRequest.status = com.lifelink.blood_donation.Entities.Enums.RequestStatus.APPROVED
           """)
    long countAwaitingResponse(@Param("donorId") Long donorId);
}

