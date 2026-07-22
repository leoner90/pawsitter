package lv.pawsitter.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lv.pawsitter.dto.ReviewRequest;
import lv.pawsitter.dto.ReviewResponse;
import lv.pawsitter.entity.Booking;
import lv.pawsitter.entity.BookingStatus;
import lv.pawsitter.entity.Review;
import lv.pawsitter.entity.User;
import lv.pawsitter.repository.BookingRepository;
import lv.pawsitter.repository.ReviewRepository;
import lv.pawsitter.repository.UserRepository;
import lv.pawsitter.exception.BookingNotFoundException;
import lv.pawsitter.exception.InvalidReviewOperationException;
import lv.pawsitter.exception.ReviewNotFoundException;
import lv.pawsitter.exception.UserNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    public ReviewResponse createReview(ReviewRequest request, String reviewerEmail) {
        log.info("Creating review for booking {} by {}", request.getBookingId(), reviewerEmail);
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> {
                    log.warn("Booking {} not found while creating review", request.getBookingId());
                    return new BookingNotFoundException("Booking not found");
                });

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            log.warn("Review attempted for booking {} with status {}", booking.getId(), booking.getStatus());
            throw new InvalidReviewOperationException("Only completed bookings can have a review");
        }

        User reviewer = userRepository.findByEmail(reviewerEmail)
                .orElseThrow(() -> {
                    log.warn("Reviewer {} not found", reviewerEmail);
                    return new UserNotFoundException("Reviewer not found");
                });

        User ownerUser = booking.getOwner().getUser();
        User sitterUser = booking.getSitter().getUser();

        User reviewee;

        if (reviewer.getId().equals(ownerUser.getId())) {
            reviewee = sitterUser;
        } else if (reviewer.getId().equals(sitterUser.getId())) {
            reviewee = ownerUser;
        } else {
            log.warn("User {} attempted to review booking {} they are not part of",
                    reviewerEmail, booking.getId());
            throw new InvalidReviewOperationException("Only the users of this booking can leave a review");
        }

        if (reviewRepository.existsByBookingIdAndReviewerId(booking.getId(), reviewer.getId())) {
            log.warn("Duplicate review attempt for booking {} by user {}",
                    booking.getId(), reviewer.getId());
            throw new InvalidReviewOperationException("You have already reviewed this booking");
        }

        Review review = new Review();
        review.setBooking(booking);
        review.setReviewer(reviewer);
        review.setReviewee(reviewee);
        review.setRating(request.getRating());
        review.setComment(request.getReviewComment());

        Review saved = reviewRepository.save(review);
        log.info("Review {} created for booking {} by user {}", saved.getId(), booking.getId(), reviewer.getId());
        return mapToResponse(saved);
    }

    public ReviewResponse getReviewById(Long id) {
        log.info("Fetching review {}", id);
        return mapToResponse(reviewRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Review {} not found", id);
                    return new ReviewNotFoundException("Review not found");
                }));
    }

    public List<ReviewResponse> getReviewByBooking(Long bookingId) {
        log.info("Fetching reviews for booking {}", bookingId);
        return reviewRepository.findByBookingId(bookingId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<ReviewResponse> getReviewsReceivedBy(Long userId) {
        log.info("Fetching reviews received by user {}", userId);
        return reviewRepository.findByRevieweeId(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<ReviewResponse> getReviewsWrittenBy(Long userId) {
        log.info("Fetching reviews written by user {}", userId);
        return reviewRepository.findByReviewerId(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<ReviewResponse> getAllReviews() {
        log.info("Fetching all reviews");
        return reviewRepository
                .findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public ReviewResponse updateReview(Long id, ReviewRequest request, String requesterEmail) {
        log.info("Updating review {} by {}", id, requesterEmail);
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Review {} not found for update", id);
                    return new ReviewNotFoundException("Review not found");
                });

        if (!review.getReviewer().getEmail().equals(requesterEmail)) {
            log.warn("Unauthorized update attempt on review {} by {}", id, requesterEmail);
            throw new InvalidReviewOperationException("only the creator of this comment can edit this review");
        }

        review.setRating(request.getRating());
        review.setComment(request.getReviewComment());

        Review updated = reviewRepository.save(review);
        log.info("Review {} updated", id);
        return mapToResponse(updated);
    }

    public void deleteReview(Long id, String requesterEmail) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Review {} not found for deletion", id);
                    return new ReviewNotFoundException("Review not found");
                });

        if (!review.getReviewer().getEmail().equals(requesterEmail)) {
            log.warn("Unauthorized delete attempt on review {} by {}", id, requesterEmail);
            throw new InvalidReviewOperationException("only the creator of this comment can delete this review");
        }
        reviewRepository.delete(review);
        log.info("Review {} deleted", id);
    }

    public ReviewSummary getReviewSummaryForUser(Long userId) {
        log.debug("Fetching review summary for user {}", userId);
        double average = reviewRepository.findAverageRatingByRevieweeId(userId).orElse(0.0);
        long count = reviewRepository.countByRevieweeId(userId);
        return new ReviewSummary(average, count);
    }

    public record ReviewSummary(double averageRating, long reviewCount) {}

    private ReviewResponse mapToResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getBooking().getId(),
                review.getReviewer().getId(),
                review.getReviewer().getFirstName() + " " + review.getReviewer().getLastName(),
                review.getReviewee().getId(),
                review.getReviewee().getFirstName() + " " + review.getReviewee().getLastName(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt());
    }
}
