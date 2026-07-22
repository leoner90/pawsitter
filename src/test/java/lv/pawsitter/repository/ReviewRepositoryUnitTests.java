package lv.pawsitter.repository;

import lv.pawsitter.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
public class ReviewRepositoryUnitTests {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OwnerProfileRepository ownerProfileRepository;

    @Autowired
    private SitterProfileRepository sitterProfileRepository;

    private User persistUser(String firstName, String email) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName("Test");
        user.setPhoneNumber("+37120000000");
        user.setEmail(email);
        user.setPassword("password123");
        user.setRole(lv.pawsitter.model.RoleType.USER);
        return userRepository.save(user);
    }

    private Booking persistCompletedBooking(User owner, User sitter) {
        OwnerProfile ownerProfile = new OwnerProfile();
        ownerProfile.setUser(owner);
        ownerProfile = ownerProfileRepository.save(ownerProfile);

        SitterProfile sitterProfile = new SitterProfile();
        sitterProfile.setUser(sitter);
        sitterProfile = sitterProfileRepository.save(sitterProfile);

        Booking booking = new Booking();
        booking.setOwner(ownerProfile);
        booking.setSitter(sitterProfile);
        booking.setStartDate(LocalDateTime.now());
        booking.setEndDate(LocalDateTime.now().plusDays(2));
        booking.setStatus(BookingStatus.COMPLETED);
        return bookingRepository.save(booking);
    }

    private OwnerProfile persistOwnerProfile(User user) {
        OwnerProfile ownerProfile = new OwnerProfile();
        ownerProfile.setUser(user);
        return ownerProfileRepository.save(ownerProfile);
    }

    private SitterProfile persistSitterProfile(User user) {
        SitterProfile sitterProfile = new SitterProfile();
        sitterProfile.setUser(user);
        return sitterProfileRepository.save(sitterProfile);
    }

    private Booking persistCompletedBooking(OwnerProfile ownerProfile, SitterProfile sitterProfile) {
        Booking booking = new Booking();
        booking.setOwner(ownerProfile);
        booking.setSitter(sitterProfile);
        booking.setStartDate(LocalDateTime.now());
        booking.setEndDate(LocalDateTime.now().plusDays(2));
        booking.setStatus(BookingStatus.COMPLETED);
        return bookingRepository.save(booking);
    }

    @Test
    void save_persistsReviewWithGeneratedIdAndTimestamp() {
        User owner = persistUser("Jane", "jane@example.com");
        User sitter = persistUser("John", "john@example.com");
        Booking booking = persistCompletedBooking(owner, sitter);

        Review review = new Review();
        review.setBooking(booking);
        review.setReviewer(owner);
        review.setReviewee(sitter);
        review.setRating(5);
        review.setComment("Excellent care");

        Review saved = reviewRepository.save(review);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void findByBookingId_returnsAllReviewsForBooking() {
        User owner = persistUser("Jane", "jane2@example.com");
        User sitter = persistUser("John", "john2@example.com");
        Booking booking = persistCompletedBooking(owner, sitter);

        Review ownerReview = new Review();
        ownerReview.setBooking(booking);
        ownerReview.setReviewer(owner);
        ownerReview.setReviewee(sitter);
        ownerReview.setRating(5);
        reviewRepository.save(ownerReview);

        Review sitterReview = new Review();
        sitterReview.setBooking(booking);
        sitterReview.setReviewer(sitter);
        sitterReview.setReviewee(owner);
        sitterReview.setRating(4);
        reviewRepository.save(sitterReview);

        List<Review> reviews = reviewRepository.findByBookingId(booking.getId());

        assertEquals(2, reviews.size());
    }

    @Test
    void existsByBookingIdAndReviewerId_returnsTrueAfterReviewSaved() {
        User owner = persistUser("Jane", "jane3@example.com");
        User sitter = persistUser("John", "john3@example.com");
        Booking booking = persistCompletedBooking(owner, sitter);

        Review review = new Review();
        review.setBooking(booking);
        review.setReviewer(owner);
        review.setReviewee(sitter);
        review.setRating(5);
        reviewRepository.save(review);

        boolean exists = reviewRepository.existsByBookingIdAndReviewerId(booking.getId(), owner.getId());
        boolean doesNotExist = reviewRepository.existsByBookingIdAndReviewerId(booking.getId(), sitter.getId());

        assertTrue(exists);
        assertFalse(doesNotExist);
    }

    @Test
    void findByBookingIdAndReviewerId_returnsMatchingReview() {
        User owner = persistUser("Jane", "jane4@example.com");
        User sitter = persistUser("John", "john4@example.com");
        Booking booking = persistCompletedBooking(owner, sitter);

        Review review = new Review();
        review.setBooking(booking);
        review.setReviewer(owner);
        review.setReviewee(sitter);
        review.setRating(3);
        reviewRepository.save(review);

        Optional<Review> found = reviewRepository.findByBookingIdAndReviewerId(booking.getId(), owner.getId());

        assertTrue(found.isPresent());
        assertEquals(3, found.get().getRating());
    }

    @Test
    void findByRevieweeId_returnsReviewsReceivedByUser() {
        User owner = persistUser("Jane", "jane5@example.com");
        User sitter = persistUser("John", "john5@example.com");
        Booking booking = persistCompletedBooking(owner, sitter);

        Review review = new Review();
        review.setBooking(booking);
        review.setReviewer(owner);
        review.setReviewee(sitter);
        review.setRating(5);
        reviewRepository.save(review);

        List<Review> received = reviewRepository.findByRevieweeId(sitter.getId());

        assertEquals(1, received.size());
        assertEquals(sitter.getId(), received.get(0).getReviewee().getId());
    }

    @Test
    void findByReviewerId_returnsReviewsWrittenByUser() {
        User owner = persistUser("Jane", "jane6@example.com");
        User sitter = persistUser("John", "john6@example.com");
        Booking booking = persistCompletedBooking(owner, sitter);

        Review review = new Review();
        review.setBooking(booking);
        review.setReviewer(owner);
        review.setReviewee(sitter);
        review.setRating(5);
        reviewRepository.save(review);

        List<Review> written = reviewRepository.findByReviewerId(owner.getId());

        assertEquals(1, written.size());
        assertEquals(owner.getId(), written.get(0).getReviewer().getId());
    }

    @Test
    void findAverageRatingByRevieweeId_returnsAverageAcrossAllReviewsForThatUser() {
        User ownerA = persistUser("Jane", "jane7@example.com");
        User ownerB = persistUser("Alice", "alice7@example.com");
        User sitter = persistUser("John", "john7@example.com");

        OwnerProfile ownerProfileA = persistOwnerProfile(ownerA);
        OwnerProfile ownerProfileB = persistOwnerProfile(ownerB);
        SitterProfile sitterProfile = persistSitterProfile(sitter);

        Booking bookingA = persistCompletedBooking(ownerProfileA, sitterProfile);
        Booking bookingB = persistCompletedBooking(ownerProfileB, sitterProfile);

        Review reviewA = new Review();
        reviewA.setBooking(bookingA);
        reviewA.setReviewer(ownerA);
        reviewA.setReviewee(sitter);
        reviewA.setRating(5);
        reviewRepository.save(reviewA);

        Review reviewB = new Review();
        reviewB.setBooking(bookingB);
        reviewB.setReviewer(ownerB);
        reviewB.setReviewee(sitter);
        reviewB.setRating(3);
        reviewRepository.save(reviewB);

        Optional<Double> average = reviewRepository.findAverageRatingByRevieweeId(sitter.getId());

        assertTrue(average.isPresent());
        assertEquals(4.0, average.get(), 0.001);
    }

    @Test
    void findAverageRatingByRevieweeId_returnsEmptyWhenUserHasNoReviews() {
        User sitter = persistUser("John", "john8@example.com");

        Optional<Double> average = reviewRepository.findAverageRatingByRevieweeId(sitter.getId());

        assertTrue(average.isEmpty());
    }

    @Test
    void findAverageRatingByRevieweeId_onlyCountsReviewsForThatSpecificUser() {
        User owner = persistUser("Jane", "jane9@example.com");
        User sitter = persistUser("John", "john9@example.com");
        User otherSitter = persistUser("Mark", "mark9@example.com");

        OwnerProfile ownerProfile = persistOwnerProfile(owner);
        SitterProfile sitterProfile = persistSitterProfile(sitter);
        SitterProfile otherSitterProfile = persistSitterProfile(otherSitter);

        Booking booking = persistCompletedBooking(ownerProfile, sitterProfile);
        Booking otherBooking = persistCompletedBooking(ownerProfile, otherSitterProfile);

        Review review = new Review();
        review.setBooking(booking);
        review.setReviewer(owner);
        review.setReviewee(sitter);
        review.setRating(5);
        reviewRepository.save(review);

        Review otherReview = new Review();
        otherReview.setBooking(otherBooking);
        otherReview.setReviewer(owner);
        otherReview.setReviewee(otherSitter);
        otherReview.setRating(1);
        reviewRepository.save(otherReview);

        Optional<Double> average = reviewRepository.findAverageRatingByRevieweeId(sitter.getId());

        assertTrue(average.isPresent());
        assertEquals(5.0, average.get(), 0.001);
    }

    @Test
    void countByRevieweeId_returnsNumberOfReviewsReceivedByThatUser() {
        User ownerA = persistUser("Jane", "jane10@example.com");
        User ownerB = persistUser("Alice", "alice10@example.com");
        User sitter = persistUser("John", "john10@example.com");

        OwnerProfile ownerProfileA = persistOwnerProfile(ownerA);
        OwnerProfile ownerProfileB = persistOwnerProfile(ownerB);
        SitterProfile sitterProfile = persistSitterProfile(sitter);

        Booking bookingA = persistCompletedBooking(ownerProfileA, sitterProfile);
        Booking bookingB = persistCompletedBooking(ownerProfileB, sitterProfile);

        Review reviewA = new Review();
        reviewA.setBooking(bookingA);
        reviewA.setReviewer(ownerA);
        reviewA.setReviewee(sitter);
        reviewA.setRating(4);
        reviewRepository.save(reviewA);

        Review reviewB = new Review();
        reviewB.setBooking(bookingB);
        reviewB.setReviewer(ownerB);
        reviewB.setReviewee(sitter);
        reviewB.setRating(2);
        reviewRepository.save(reviewB);

        long count = reviewRepository.countByRevieweeId(sitter.getId());

        assertEquals(2, count);
    }

    @Test
    void countByRevieweeId_returnsZeroWhenUserHasNoReviews() {
        User sitter = persistUser("John", "john11@example.com");

        long count = reviewRepository.countByRevieweeId(sitter.getId());

        assertEquals(0, count);
    }
}