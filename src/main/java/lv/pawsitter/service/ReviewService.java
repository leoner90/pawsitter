package lv.pawsitter.service;


import lombok.RequiredArgsConstructor;
import lv.pawsitter.dto.ReviewRequest;
import lv.pawsitter.dto.ReviewResponse;
import lv.pawsitter.entity.Booking;
import lv.pawsitter.entity.BookingStatus;
import lv.pawsitter.entity.Review;
import lv.pawsitter.entity.User;
import lv.pawsitter.repository.ReviewRepository;
import lv.pawsitter.repository.UserRepository;
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
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new IllegalStateException("Only completed bookings can have a review");
        }

        if (reviewRepository.existsByBookingId(booking.getId())) {
            throw new IllegalStateException("Review for this booking already exists");
        }

        User reviewer = userRepository.findByEmail(reviewerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Reviewer not found"));

        boolean isBookingUser = booking.getOwner().getUser().getId().equals(reviewer.getId())
                || booking.getSitter().getUser().getId().equals(reviewer.getId());

        if (isBookingUser) {
            throw new IllegalStateException("Only the users of this booking can leave a review");
        }

        Review review = new Review();
        review.setBooking(booking);
        review.setReviewer(reviewer);
        review.setRating(request.getRating());
        review.setComment(request.getReviewComment());

        Review savedReview = reviewRepository.save(review);
        return mapToResponse(savedReview);


    }

    public ReviewResponse getReviewId(Long id){

        return mapToResponse(reviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Review not found")));
    }

    public ReviewResponse getReviewByBooking(Long bookingId)
    {
        return mapToResponse(reviewRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found for this booking")));
    }

    public List<ReviewResponse> getReviewsByReviewer(Long reviewerId){

        return reviewRepository.findByReviewerId(reviewerId)
                .stream()
                .map(this::mapToResponse)
                .toList();

    }

    public List<ReviewResponse> getAllReviews(){
        return reviewRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public ReviewResponse updateReview(Long id, ReviewRequest request, String reviewerEmail) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        if (!review.getReviewer().getEmail().equals(reviewerEmail)) {
            throw new IllegalStateException("only the creator of this comment can edit this review");
        }

        review.setRating(request.getRating());
        review.setComment(request.getReviewComment());

        return mapToResponse(reviewRepository.save(review));
    }

    public void deleteReview(Long id, String reqursterEmail) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        if (!review.getReviewer().getEmail().equals(reqursterEmail)) {
            throw new IllegalStateException("only the creator of this comment can delete this review");

        }
        reviewRepository.delete(review);


    }


    private ReviewResponse mapToResponse(Review review)
    {
        return new ReviewResponse(
                review.getId(),
                review.getBooking().getId(),
                review.getReviewer().getId(),
                review.getReviewer().getFirstName() + " " + review.getReviewer().getLastName(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}
