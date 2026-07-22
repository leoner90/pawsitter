package lv.pawsitter.repository;

import lv.pawsitter.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long>
{
    List<Review> findByBookingId(Long bookingId);
    Optional<Review> findByBookingIdAndReviewerId(Long bookingId, Long reviewerId);
    boolean existsByBookingIdAndReviewerId(Long bookingId, Long reviewerId);
    List<Review> findByRevieweeId(Long revieweeId);
    List<Review> findByReviewerId(Long reviewerId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.reviewee.id = :revieweeId")
    Optional<Double> findAverageRatingByRevieweeId(@Param("revieweeId") Long revieweeId);
    long countByRevieweeId(Long revieweeId);
}
