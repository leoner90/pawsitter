package lv.pawsitter.repository;

import lv.pawsitter.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long>
{
    Optional<Review> findByBookingId(Long bookingId);
    List<Review> findByReviewerId(Long reviewerId);
    boolean existsByBookingId(Long bookingId);

}
