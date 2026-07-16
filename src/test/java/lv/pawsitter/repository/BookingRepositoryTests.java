package lv.pawsitter.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import lv.pawsitter.entity.Booking;
import lv.pawsitter.entity.BookingStatus;
import lv.pawsitter.entity.OwnerProfile;
import lv.pawsitter.entity.SitterProfile;
import lv.pawsitter.entity.User;
import lv.pawsitter.model.RoleType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
class BookingRepositoryTests {

  private static final LocalDateTime START = LocalDateTime.of(2026, 8, 10, 9, 0);
  private static final LocalDateTime END = LocalDateTime.of(2026, 8, 12, 18, 0);

  @Autowired
  private BookingRepository bookingRepository;

  @Autowired
  private TestEntityManager entityManager;

  @Test
  void findByOwnerIdReturnsOnlyOwnerBookings() {
    OwnerProfile owner = owner("owner@example.com");
    OwnerProfile otherOwner = owner("other-owner@example.com");
    SitterProfile sitter = sitter("sitter@example.com");
    Booking ownerBooking = booking(owner, sitter, BookingStatus.REQUESTED, START, END);
    booking(otherOwner, sitter, BookingStatus.REQUESTED, START.plusDays(3), END.plusDays(3));
    flushAndClear();

    List<Booking> bookings = bookingRepository.findByOwnerId(owner.getId());

    assertThat(bookings)
        .extracting(Booking::getId)
        .containsExactly(ownerBooking.getId());
  }

  @Test
  void findBySitterIdReturnsOnlySitterBookings() {
    OwnerProfile owner = owner("owner@example.com");
    SitterProfile sitter = sitter("sitter@example.com");
    SitterProfile otherSitter = sitter("other-sitter@example.com");
    Booking sitterBooking = booking(owner, sitter, BookingStatus.REQUESTED, START, END);
    booking(owner, otherSitter, BookingStatus.REQUESTED, START.plusDays(3), END.plusDays(3));
    flushAndClear();

    List<Booking> bookings = bookingRepository.findBySitterId(sitter.getId());

    assertThat(bookings)
        .extracting(Booking::getId)
        .containsExactly(sitterBooking.getId());
  }

  @Test
  void findByOwnerIdAndStatusFiltersByOwnerAndStatus() {
    OwnerProfile owner = owner("owner@example.com");
    OwnerProfile otherOwner = owner("other-owner@example.com");
    SitterProfile sitter = sitter("sitter@example.com");
    Booking acceptedBooking = booking(owner, sitter, BookingStatus.ACCEPTED, START, END);
    booking(owner, sitter, BookingStatus.REQUESTED, START.plusDays(3), END.plusDays(3));
    booking(otherOwner, sitter, BookingStatus.ACCEPTED, START.plusDays(6), END.plusDays(6));
    flushAndClear();

    List<Booking> bookings = bookingRepository.findByOwnerIdAndStatus(owner.getId(), BookingStatus.ACCEPTED);

    assertThat(bookings)
        .extracting(Booking::getId)
        .containsExactly(acceptedBooking.getId());
  }

  @Test
  void findBySitterIdAndStatusFiltersBySitterAndStatus() {
    OwnerProfile owner = owner("owner@example.com");
    SitterProfile sitter = sitter("sitter@example.com");
    SitterProfile otherSitter = sitter("other-sitter@example.com");
    Booking acceptedBooking = booking(owner, sitter, BookingStatus.ACCEPTED, START, END);
    booking(owner, sitter, BookingStatus.REQUESTED, START.plusDays(3), END.plusDays(3));
    booking(owner, otherSitter, BookingStatus.ACCEPTED, START.plusDays(6), END.plusDays(6));
    flushAndClear();

    List<Booking> bookings = bookingRepository.findBySitterIdAndStatus(sitter.getId(), BookingStatus.ACCEPTED);

    assertThat(bookings)
        .extracting(Booking::getId)
        .containsExactly(acceptedBooking.getId());
  }

  @ParameterizedTest
  @MethodSource("overlapDateRanges")
  void existsBySitterIdAndStatusAndStartDateLessThanAndEndDateGreaterThanDetectsOverlaps(
      LocalDateTime requestedStart,
      LocalDateTime requestedEnd,
      boolean expectedOverlap
  ) {
    OwnerProfile owner = owner("owner@example.com");
    SitterProfile sitter = sitter("sitter@example.com");
    booking(owner, sitter, BookingStatus.ACCEPTED, START, END);
    flushAndClear();

    boolean overlaps = bookingRepository.existsBySitterIdAndStatusAndStartDateLessThanAndEndDateGreaterThan(
        sitter.getId(),
        BookingStatus.ACCEPTED,
        requestedEnd,
        requestedStart
    );

    assertThat(overlaps).isEqualTo(expectedOverlap);
  }

  @Test
  void existsBySitterIdAndStatusAndStartDateLessThanAndEndDateGreaterThanIgnoresOtherStatuses() {
    OwnerProfile owner = owner("owner@example.com");
    SitterProfile sitter = sitter("sitter@example.com");
    booking(owner, sitter, BookingStatus.REQUESTED, START, END);
    flushAndClear();

    boolean overlaps = bookingRepository.existsBySitterIdAndStatusAndStartDateLessThanAndEndDateGreaterThan(
        sitter.getId(),
        BookingStatus.ACCEPTED,
        START.plusDays(1),
        START.minusDays(1)
    );

    assertThat(overlaps).isFalse();
  }

  private static Stream<Arguments> overlapDateRanges() {
    return Stream.of(
        Arguments.of(START.minusDays(1), START, false),
        Arguments.of(END, END.plusDays(1), false),
        Arguments.of(START.minusHours(1), START.plusHours(1), true),
        Arguments.of(START.plusHours(1), END.minusHours(1), true),
        Arguments.of(END.minusHours(1), END.plusHours(1), true)
    );
  }

  private Booking booking(
      OwnerProfile owner,
      SitterProfile sitter,
      BookingStatus status,
      LocalDateTime startDate,
      LocalDateTime endDate
  ) {
    Booking booking = new Booking();
    booking.setOwner(owner);
    booking.setSitter(sitter);
    booking.setStatus(status);
    booking.setStartDate(startDate);
    booking.setEndDate(endDate);
    return entityManager.persistAndFlush(booking);
  }

  private OwnerProfile owner(String email) {
    User user = user(email, RoleType.USER);
    OwnerProfile owner = new OwnerProfile();
    owner.setUser(user);
    entityManager.persist(user);
    return entityManager.persistAndFlush(owner);
  }

  private SitterProfile sitter(String email) {
    User user = user(email, RoleType.SITTER);
    SitterProfile sitter = new SitterProfile();
    sitter.setUser(user);
    entityManager.persist(user);
    return entityManager.persistAndFlush(sitter);
  }

  private User user(String email, RoleType role) {
    User user = new User();
    user.setFirstName("Test");
    user.setLastName("User");
    user.setPhoneNumber("+37120000000");
    user.setEmail(email);
    user.setPassword("password");
    user.setRole(role);
    return user;
  }

  private void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }
}
