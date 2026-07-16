package lv.pawsitter.service;

import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    public ReviewResponse createReview(ReviewRequest request, String reviewerEmail) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new BookingNotFoundException("Booking not found"));

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new InvalidReviewOperationException("Only completed bookings can have a review");
        }

        User reviewer = userRepository.findByEmail(reviewerEmail)
                .orElseThrow(() -> new UserNotFoundException("Reviewer not found"));

        User ownerUser = booking.getOwner().getUser();
        User sitterUser = booking.getSitter().getUser();

        User reviewee;

        if (reviewer.getId().equals(ownerUser.getId())) {
            reviewee = sitterUser;
        } else if (reviewer.getId().equals(sitterUser.getId())) {
            reviewee = ownerUser;
        } else {
            throw new InvalidReviewOperationException("Only the users of this booking can leave a review");
        }

        if (reviewRepository.existsByBookingIdAndReviewerId(booking.getId(), reviewer.getId())) {
            throw new InvalidReviewOperationException("You have already reviewed this booking");
        }

        Review review = new Review();
        review.setBooking(booking);
        review.setReviewer(reviewer);
        review.setReviewee(reviewee);
        review.setRating(request.getRating());
        review.setComment(request.getReviewComment());

        return mapToResponse(reviewRepository.save(review));

    }

    public ReviewResponse getReviewById(Long id) {

        return mapToResponse(reviewRepository.findById(id)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found")));
    }

    public List<ReviewResponse> getReviewByBooking(Long bookingId) {
        return reviewRepository.findByBookingId(bookingId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<ReviewResponse> getReviewsReceivedBy(Long userId) {

        return reviewRepository.findByRevieweeId(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();

    }

    public List<ReviewResponse> getReviewsWrittenBy(Long userId) {
        return reviewRepository.findByReviewerId(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<ReviewResponse> getAllReviews() {
        return reviewRepository
                .findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public ReviewResponse updateReview(Long id, ReviewRequest request, String requesterEmail) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found"));

        if (!review.getReviewer().getEmail().equals(requesterEmail)) {
            throw new InvalidReviewOperationException("only the creator of this comment can edit this review");
        }

        review.setRating(request.getRating());
        review.setComment(request.getReviewComment());

        return mapToResponse(reviewRepository.save(review));
    }

    public void deleteReview(Long id, String requesterEmail) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found"));

        if (!review.getReviewer().getEmail().equals(requesterEmail)) {
            throw new InvalidReviewOperationException("only the creator of this comment can delete this review");

        }
        reviewRepository.delete(review);

    }

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
