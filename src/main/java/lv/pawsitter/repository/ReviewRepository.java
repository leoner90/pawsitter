package lv.pawsitter.repository;

import lv.pawsitter.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long>
{
    List<Review> findByBookingId(Long bookingId);
    Optional<Review> findByBookingIdAndReviewerId(Long bookingId, Long reviewerId);
    boolean existsByBookingIdAndReviewerId(Long bookingId, Long reviewerId);
    List<Review> findByRevieweeId(Long revieweeId);
    List<Review> findByReviewerId(Long reviewerId);


}
