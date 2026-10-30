package com.lifelink.blood_donation.Repositories;

import com.lifelink.blood_donation.Entities.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByDonationHistoryId(Long donationHistoryId);

    // Safe as Optional: donation_history_id is unique
    Optional<Review> findByDonationHistoryId(Long donationHistoryId);

    // Batched enrichment for list pages
    List<Review> findByDonationHistoryIdIn(Collection<Long> donationHistoryIds);

    List<Review> findByDonorIdOrderByCreatedAtDesc(Long donorId);

    long countByDonorId(Long donorId);

    @Query("select avg(r.rating) from Review r where r.donor.id = :donorId")
    Double findAverageRatingByDonorId(@Param("donorId") Long donorId);
}
