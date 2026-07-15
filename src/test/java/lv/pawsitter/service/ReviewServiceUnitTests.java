package lv.pawsitter.service;

import lv.pawsitter.dto.ReviewRequest;
import lv.pawsitter.dto.ReviewResponse;
import lv.pawsitter.entity.*;
import lv.pawsitter.exception.BookingNotFoundException;
import lv.pawsitter.exception.InvalidReviewOperationException;
import lv.pawsitter.exception.ReviewNotFoundException;
import lv.pawsitter.exception.UserNotFoundException;
import lv.pawsitter.repository.BookingRepository;
import lv.pawsitter.repository.ReviewRepository;
import lv.pawsitter.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReviewServiceUnitTests {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReviewService reviewService;

    private User owner;
    private User sitter;
    private OwnerProfile ownerProfile;
    private SitterProfile sitterProfile;
    private Booking booking;
    private ReviewRequest request;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);
        owner.setFirstName("Jane");
        owner.setLastName("Doe");
        owner.setEmail("jane@example.com");

        sitter = new User();
        sitter.setId(2L);
        sitter.setFirstName("John");
        sitter.setLastName("Smith");
        sitter.setEmail("john@example.com");

        ownerProfile = new OwnerProfile();
        ownerProfile.setId(10L);
        ownerProfile.setUser(owner);

        sitterProfile = new SitterProfile();
        sitterProfile.setId(20L);
        sitterProfile.setUser(sitter);

        booking = new Booking();
        booking.setId(13L);
        booking.setOwner(ownerProfile);
        booking.setSitter(sitterProfile);
        booking.setStatus(BookingStatus.COMPLETED);

        request = new ReviewRequest();
        request.setBookingId(13L);
        request.setRating(5);
        request.setReviewComment("Great job, I am amazed!");
    }

    @Test
    void createReview_ownerReviewsSitter_savesReviewWithCorrectReviewee() {
        when(bookingRepository.findById(13L)).thenReturn(Optional.of(booking));
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(owner));
        when(reviewRepository.existsByBookingIdAndReviewerId(13L, 1L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review saved = invocation.getArgument(0);
            saved.setId(500L);
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });

        ReviewResponse response = reviewService.createReview(request, "jane@example.com");

        assertEquals(1L, response.getReviewerId());
        assertEquals(2L, response.getRevieweeId());
        assertEquals(5, response.getRating());
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void createReview_sitterReviewsOwner_savesReviewWithCorrectReviewee() {
        when(bookingRepository.findById(13L)).thenReturn(Optional.of(booking));
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sitter));
        when(reviewRepository.existsByBookingIdAndReviewerId(13L, 2L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewResponse response = reviewService.createReview(request, "john@example.com");

        assertEquals(2L, response.getReviewerId());
        assertEquals(1L, response.getRevieweeId());
    }

    @Test
    void createReview_bookingNotFound_throwsBookingNotFoundException() {
        when(bookingRepository.findById(13L)).thenReturn(Optional.empty());

        assertThrows(BookingNotFoundException.class,
                () -> reviewService.createReview(request, "jane@example.com"));

        verifyNoInteractions(reviewRepository);
    }

    @Test
    void createReview_bookingNotCompleted_throwsInvalidReviewOperationException() {
        booking.setStatus(BookingStatus.REQUESTED);
        when(bookingRepository.findById(13L)).thenReturn(Optional.of(booking));

        assertThrows(InvalidReviewOperationException.class,
                () -> reviewService.createReview(request, "jane@example.com"));
    }

    @Test
    void createReview_reviewerNotFound_throwsUserNotFoundException() {
        when(bookingRepository.findById(13L)).thenReturn(Optional.of(booking));
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> reviewService.createReview(request, "jane@example.com"));
    }

    @Test
    void createReview_reviewerNotPartOfBooking_throwsInvalidReviewOperationException() {
        User stranger = new User();
        stranger.setId(99L);
        stranger.setEmail("stranger@example.com");

        when(bookingRepository.findById(13L)).thenReturn(Optional.of(booking));
        when(userRepository.findByEmail("stranger@example.com")).thenReturn(Optional.of(stranger));

        assertThrows(InvalidReviewOperationException.class,
                () -> reviewService.createReview(request, "stranger@example.com"));
    }

    @Test
    void createReview_alreadyReviewed_throwsInvalidReviewOperationException() {
        when(bookingRepository.findById(13L)).thenReturn(Optional.of(booking));
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(owner));
        when(reviewRepository.existsByBookingIdAndReviewerId(13L, 1L)).thenReturn(true);

        assertThrows(InvalidReviewOperationException.class,
                () -> reviewService.createReview(request, "jane@example.com"));

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void getReviewById_found_returnsResponse() {
        Review review = buildReview(500L, owner, sitter, 4);
        when(reviewRepository.findById(500L)).thenReturn(Optional.of(review));

        ReviewResponse response = reviewService.getReviewById(500L);

        assertEquals(500L, response.getId());
        assertEquals(4, response.getRating());
    }

    @Test
    void getReviewById_notFound_throwsReviewNotFoundException() {
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ReviewNotFoundException.class, () -> reviewService.getReviewById(999L));
    }

    @Test
    void getReviewsReceivedBy_returnsReviewsWhereUserIsReviewee() {
        Review review = buildReview(500L, owner, sitter, 5);
        when(reviewRepository.findByRevieweeId(2L)).thenReturn(List.of(review));

        List<ReviewResponse> responses = reviewService.getReviewsReceivedBy(2L);

        assertEquals(1, responses.size());
        assertEquals(2L, responses.get(0).getRevieweeId());
    }

    @Test
    void getReviewsWrittenBy_returnsReviewsWhereUserIsReviewer() {
        Review review = buildReview(500L, owner, sitter, 5);
        when(reviewRepository.findByReviewerId(1L)).thenReturn(List.of(review));

        List<ReviewResponse> responses = reviewService.getReviewsWrittenBy(1L);

        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).getReviewerId());
    }

    @Test
    void getReviewsReceivedBy_andGetReviewsWrittenBy_returnDifferentResults() {

        Review received = buildReview(500L, sitter, owner, 5);   // owner is reviewee
        Review written = buildReview(501L, owner, sitter, 4);    // owner is reviewer

        when(reviewRepository.findByRevieweeId(1L)).thenReturn(List.of(received));
        when(reviewRepository.findByReviewerId(1L)).thenReturn(List.of(written));

        List<ReviewResponse> receivedResult = reviewService.getReviewsReceivedBy(1L);
        List<ReviewResponse> writtenResult = reviewService.getReviewsWrittenBy(1L);

        assertEquals(500L, receivedResult.get(0).getId());
        assertEquals(501L, writtenResult.get(0).getId());
    }

    @Test
    void updateReview_byOriginalReviewer_updatesSuccessfully() {
        Review review = buildReview(500L, owner, sitter, 3);
        ReviewRequest updateRequest = new ReviewRequest();
        updateRequest.setBookingId(13L);
        updateRequest.setRating(5);
        updateRequest.setReviewComment("Updated comment");

        when(reviewRepository.findById(500L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewResponse response = reviewService.updateReview(500L, updateRequest, "jane@example.com");

        assertEquals(5, response.getRating());
    }

    @Test
    void updateReview_byDifferentUser_throwsInvalidReviewOperationException() {
        Review review = buildReview(500L, owner, sitter, 3);
        when(reviewRepository.findById(500L)).thenReturn(Optional.of(review));

        assertThrows(InvalidReviewOperationException.class,
                () -> reviewService.updateReview(500L, request, "john@example.com"));
    }

    @Test
    void updateReview_notFound_throwsReviewNotFoundException() {
        when(reviewRepository.findById(500L)).thenReturn(Optional.empty());

        assertThrows(ReviewNotFoundException.class,
                () -> reviewService.updateReview(500L, request, "jane@example.com"));
    }

    @Test
    void deleteReview_byOriginalReviewer_deletesSuccessfully() {
        Review review = buildReview(500L, owner, sitter, 3);
        when(reviewRepository.findById(500L)).thenReturn(Optional.of(review));

        reviewService.deleteReview(500L, "jane@example.com");

        verify(reviewRepository).delete(review);
    }

    @Test
    void deleteReview_byDifferentUser_throwsInvalidReviewOperationExceptionAndDoesNotDelete() {
        Review review = buildReview(500L, owner, sitter, 3);
        when(reviewRepository.findById(500L)).thenReturn(Optional.of(review));

        assertThrows(InvalidReviewOperationException.class,
                () -> reviewService.deleteReview(500L, "john@example.com"));

        verify(reviewRepository, never()).delete(any());
    }

    @Test
    void deleteReview_notFound_throwsReviewNotFoundException() {
        when(reviewRepository.findById(500L)).thenReturn(Optional.empty());

        assertThrows(ReviewNotFoundException.class,
                () -> reviewService.deleteReview(500L, "jane@example.com"));
    }

    private Review buildReview(Long id, User reviewer, User reviewee, int rating) {
        Review review = new Review();
        review.setId(id);
        review.setBooking(booking);
        review.setReviewer(reviewer);
        review.setReviewee(reviewee);
        review.setRating(rating);
        review.setComment("Test comment");
        review.setCreatedAt(LocalDateTime.now());
        return review;
    }
}