package lv.pawsitter.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import lv.pawsitter.dto.BookingResponse;
import lv.pawsitter.dto.CreateBookingRequest;
import lv.pawsitter.dto.UpdateBookingRequest;
import lv.pawsitter.entity.Booking;
import lv.pawsitter.entity.BookingStatus;
import lv.pawsitter.entity.OwnerProfile;
import lv.pawsitter.entity.Pet;
import lv.pawsitter.entity.SitterProfile;
import lv.pawsitter.entity.User;
import lv.pawsitter.exception.BookingNotFoundException;
import lv.pawsitter.exception.InvalidBookingOperationException;
import lv.pawsitter.exception.PetNotFoundException;
import lv.pawsitter.repository.BookingRepository;
import lv.pawsitter.repository.OwnerProfileRepository;
import lv.pawsitter.repository.PetRepository;
import lv.pawsitter.repository.SitterProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTests {
  private static final String OWNER_EMAIL = "owner@example.com";
  private static final String SITTER_EMAIL = "sitter@example.com";
  private static final String OTHER_EMAIL = "other@example.com";
  private static final LocalDateTime START_DATE = LocalDateTime.of(2026, 8, 10, 10, 0);
  private static final LocalDateTime END_DATE = LocalDateTime.of(2026, 8, 12, 18, 0);

  @Mock
  private BookingRepository bookingRepository;

  @Mock
  private OwnerProfileRepository ownerProfileRepository;

  @Mock
  private SitterProfileRepository sitterProfileRepository;

  @Mock
  private PetRepository petRepository;

  @InjectMocks
  private BookingServiceImpl bookingService;

  private OwnerProfile owner;
  private SitterProfile sitter;
  private Pet pet;

  @BeforeEach
  void setUp() {
    owner = ownerProfile(10L, user(1L, OWNER_EMAIL, "Olivia", "Owner"));
    sitter = sitterProfile(20L, user(2L, SITTER_EMAIL, "Sam", "Sitter"), true, "25.00");
    pet = pet(30L, owner);
  }

  @Test
  void createBookingCreatesRequestedBookingWithNotePetsAndPriceSnapshot() {
    CreateBookingRequest request = createRequest(List.of(pet.getId()), " Evening medicine ");
    when(ownerProfileRepository.findByUserEmail(OWNER_EMAIL)).thenReturn(Optional.of(owner));
    when(sitterProfileRepository.findById(sitter.getId())).thenReturn(Optional.of(sitter));
    when(petRepository.findByIdAndOwnerProfileId(pet.getId(), owner.getId())).thenReturn(Optional.of(pet));
    saveReturnsBooking();

    BookingResponse response = bookingService.createBooking(OWNER_EMAIL, request);

    assertThat(response.ownerId()).isEqualTo(owner.getId());
    assertThat(response.sitterId()).isEqualTo(sitter.getId());
    assertThat(response.status()).isEqualTo(BookingStatus.REQUESTED);
    assertThat(response.note()).isEqualTo("Evening medicine");
    assertThat(response.pricePerDaySnapshot()).isEqualByComparingTo("25.00");
    assertThat(response.petIds()).containsExactly(pet.getId());

    ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
    verify(bookingRepository).save(bookingCaptor.capture());

    Booking savedBooking = bookingCaptor.getValue();
    assertThat(savedBooking.getOwner()).isEqualTo(owner);
    assertThat(savedBooking.getSitter()).isEqualTo(sitter);
    assertThat(savedBooking.getStartDate()).isEqualTo(START_DATE);
    assertThat(savedBooking.getEndDate()).isEqualTo(END_DATE);
    assertThat(savedBooking.getStatus()).isEqualTo(BookingStatus.REQUESTED);
    assertThat(savedBooking.getNote()).isEqualTo("Evening medicine");
    assertThat(savedBooking.getPricePerDaySnapshot()).isEqualByComparingTo("25.00");
    assertThat(savedBooking.getPets()).containsExactly(pet);
  }

  @Test
  void createBookingRejectsSameOwnerAndSitterUser() {
    SitterProfile sameUserSitter = sitterProfile(21L, owner.getUser(), true, "25.00");
    CreateBookingRequest request = createRequest(List.of(pet.getId()), null);
    request.setSitterId(sameUserSitter.getId());
    when(ownerProfileRepository.findByUserEmail(OWNER_EMAIL)).thenReturn(Optional.of(owner));
    when(sitterProfileRepository.findById(sameUserSitter.getId())).thenReturn(Optional.of(sameUserSitter));

    assertThatThrownBy(() -> bookingService.createBooking(OWNER_EMAIL, request))
        .isInstanceOf(InvalidBookingOperationException.class)
        .hasMessage("Owner and sitter cannot be the same user");

    verify(bookingRepository, never()).save(any());
  }

  @Test
  void createBookingRejectsUnpublishedSitter() {
    sitter.setPublished(false);
    CreateBookingRequest request = createRequest(List.of(pet.getId()), null);
    when(ownerProfileRepository.findByUserEmail(OWNER_EMAIL)).thenReturn(Optional.of(owner));
    when(sitterProfileRepository.findById(sitter.getId())).thenReturn(Optional.of(sitter));

    assertThatThrownBy(() -> bookingService.createBooking(OWNER_EMAIL, request))
        .isInstanceOf(InvalidBookingOperationException.class)
        .hasMessage("Sitter profile is not available for booking");

    verify(bookingRepository, never()).save(any());
  }

  @Test
  void createBookingRejectsPetThatDoesNotBelongToOwner() {
    CreateBookingRequest request = createRequest(List.of(pet.getId()), null);
    when(ownerProfileRepository.findByUserEmail(OWNER_EMAIL)).thenReturn(Optional.of(owner));
    when(sitterProfileRepository.findById(sitter.getId())).thenReturn(Optional.of(sitter));
    when(petRepository.findByIdAndOwnerProfileId(pet.getId(), owner.getId())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> bookingService.createBooking(OWNER_EMAIL, request))
        .isInstanceOf(PetNotFoundException.class)
        .hasMessage("Pet not found for this owner");

    verify(bookingRepository, never()).save(any());
  }

  @ParameterizedTest
  @MethodSource("participantEmails")
  void getBookingByIdAllowsOwnerAndSitter(String participantEmail) {
    Booking booking = booking(100L, BookingStatus.REQUESTED);
    when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

    BookingResponse response = bookingService.getBookingById(booking.getId(), participantEmail);

    assertThat(response.id()).isEqualTo(booking.getId());
  }

  @Test
  void getBookingByIdRejectsUnrelatedUser() {
    Booking booking = booking(100L, BookingStatus.REQUESTED);
    when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

    assertThatThrownBy(() -> bookingService.getBookingById(booking.getId(), OTHER_EMAIL))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessage("You cannot access this booking");
  }

  @Test
  void getBookingByIdThrowsWhenBookingMissing() {
    when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> bookingService.getBookingById(999L, OWNER_EMAIL))
        .isInstanceOf(BookingNotFoundException.class)
        .hasMessage("Booking not found");
  }

  @Test
  void updateBookingAllowsOwnerToUpdateDatesPetsAndNote() {
    Booking booking = booking(100L, BookingStatus.REQUESTED);
    Pet newPet = pet(31L, owner);
    UpdateBookingRequest request = updateRequest(
        START_DATE.plusDays(1),
        END_DATE.plusDays(1),
        List.of(newPet.getId()),
        " Updated note ");
    when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
    when(petRepository.findByIdAndOwnerProfileId(newPet.getId(), owner.getId())).thenReturn(Optional.of(newPet));
    saveReturnsBooking();

    BookingResponse response = bookingService.updateBooking(booking.getId(), OWNER_EMAIL, request);

    assertThat(response.startDate()).isEqualTo(request.getStartDate());
    assertThat(response.endDate()).isEqualTo(request.getEndDate());
    assertThat(response.note()).isEqualTo("Updated note");
    assertThat(response.petIds()).containsExactly(newPet.getId());
  }

  @Test
  void updateBookingWithOnlyNotePreservesDatesAndPets() {
    Booking booking = booking(100L, BookingStatus.REQUESTED);
    UpdateBookingRequest request = updateRequest(null, null, null, " Note only ");
    when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
    saveReturnsBooking();

    BookingResponse response = bookingService.updateBooking(booking.getId(), OWNER_EMAIL, request);

    assertThat(response.startDate()).isEqualTo(START_DATE);
    assertThat(response.endDate()).isEqualTo(END_DATE);
    assertThat(response.petIds()).containsExactly(pet.getId());
    assertThat(response.note()).isEqualTo("Note only");

    ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
    verify(bookingRepository).save(bookingCaptor.capture());

    Booking savedBooking = bookingCaptor.getValue();
    assertThat(savedBooking.getStartDate()).isEqualTo(START_DATE);
    assertThat(savedBooking.getEndDate()).isEqualTo(END_DATE);
    assertThat(savedBooking.getPets()).containsExactly(pet);
    assertThat(savedBooking.getNote()).isEqualTo("Note only");
  }

  @Test
  void updateBookingWithOnlyDatesPreservesNoteAndPets() {
    Booking booking = booking(100L, BookingStatus.REQUESTED);
    LocalDateTime newStartDate = START_DATE.plusDays(2);
    LocalDateTime newEndDate = END_DATE.plusDays(2);
    UpdateBookingRequest request = updateRequest(newStartDate, newEndDate, null, null);
    when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
    saveReturnsBooking();

    BookingResponse response = bookingService.updateBooking(booking.getId(), OWNER_EMAIL, request);

    assertThat(response.startDate()).isEqualTo(newStartDate);
    assertThat(response.endDate()).isEqualTo(newEndDate);
    assertThat(response.petIds()).containsExactly(pet.getId());
    assertThat(response.note()).isEqualTo("Existing note");
  }

  @Test
  void updateBookingWithOnlyPetsPreservesDatesAndNote() {
    Booking booking = booking(100L, BookingStatus.REQUESTED);
    Pet newPet = pet(31L, owner);
    UpdateBookingRequest request = updateRequest(null, null, List.of(newPet.getId()), null);
    when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
    when(petRepository.findByIdAndOwnerProfileId(newPet.getId(), owner.getId())).thenReturn(Optional.of(newPet));
    saveReturnsBooking();

    BookingResponse response = bookingService.updateBooking(booking.getId(), OWNER_EMAIL, request);

    assertThat(response.startDate()).isEqualTo(START_DATE);
    assertThat(response.endDate()).isEqualTo(END_DATE);
    assertThat(response.petIds()).containsExactly(newPet.getId());
    assertThat(response.note()).isEqualTo("Existing note");
  }

  @Test
  void updateBookingRejectsNonOwner() {
    Booking booking = booking(100L, BookingStatus.REQUESTED);
    when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

    assertThatThrownBy(() -> bookingService.updateBooking(booking.getId(), OTHER_EMAIL, updateRequest()))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessage("You cannot manage this owner booking");
  }

  @ParameterizedTest
  @EnumSource(value = BookingStatus.class, names = "REQUESTED", mode = EnumSource.Mode.EXCLUDE)
  void updateBookingRejectsNonRequestedStatuses(BookingStatus status) {
    Booking booking = booking(100L, status);
    when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

    assertThatThrownBy(() -> bookingService.updateBooking(booking.getId(), OWNER_EMAIL, updateRequest()))
        .isInstanceOf(InvalidBookingOperationException.class)
        .hasMessage("Only requested bookings can be updated");
  }

  @ParameterizedTest
  @MethodSource("invalidDateRanges")
  void updateBookingRejectsInvalidDateRange(LocalDateTime startDate, LocalDateTime endDate) {
    Booking booking = booking(100L, BookingStatus.REQUESTED);
    UpdateBookingRequest request = updateRequest(startDate, endDate, null, null);
    when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

    assertThatThrownBy(() -> bookingService.updateBooking(booking.getId(), OWNER_EMAIL, request))
        .isInstanceOf(InvalidBookingOperationException.class)
        .hasMessage("End date must be after start date");
  }

  @Test
  void updateBookingRejectsEmptyPetList() {
    Booking booking = booking(100L, BookingStatus.REQUESTED);
    UpdateBookingRequest request = updateRequest(null, null, List.of(), null);
    when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

    assertThatThrownBy(() -> bookingService.updateBooking(booking.getId(), OWNER_EMAIL, request))
        .isInstanceOf(InvalidBookingOperationException.class)
        .hasMessage("Select at least one pet");
  }

  @ParameterizedTest
  @EnumSource(value = BookingStatus.class, names = "REQUESTED", mode = EnumSource.Mode.EXCLUDE)
  void acceptRejectsNonRequestedStatuses(BookingStatus status) {
    Booking booking = booking(100L, status);
    when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

    assertThatThrownBy(() -> bookingService.accept(booking.getId(), SITTER_EMAIL))
        .isInstanceOf(InvalidBookingOperationException.class)
        .hasMessage("Only requested bookings can be accepted");
  }

  @Test
  void acceptRejectsOverlappingAcceptedBooking() {
    Booking booking = booking(100L, BookingStatus.REQUESTED);
    when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
    when(bookingRepository.existsBySitterIdAndStatusAndStartDateLessThanAndEndDateGreaterThan(
        sitter.getId(), BookingStatus.ACCEPTED, booking.getEndDate(), booking.getStartDate()))
        .thenReturn(true);

    assertThatThrownBy(() -> bookingService.accept(booking.getId(), SITTER_EMAIL))
        .isInstanceOf(InvalidBookingOperationException.class)
        .hasMessage("Sitter already has an accepted booking for these dates");
  }

  @Test
  void acceptChangesRequestedBookingToAccepted() {
    Booking booking = booking(100L, BookingStatus.REQUESTED);
    when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
    when(bookingRepository.existsBySitterIdAndStatusAndStartDateLessThanAndEndDateGreaterThan(
        sitter.getId(), BookingStatus.ACCEPTED, booking.getEndDate(), booking.getStartDate()))
        .thenReturn(false);
    saveReturnsBooking();

    BookingResponse response = bookingService.accept(booking.getId(), SITTER_EMAIL);

    assertThat(response.status()).isEqualTo(BookingStatus.ACCEPTED);
  }

  @Test
  void acceptRejectsDifferentSitter() {
    Booking booking = booking(100L, BookingStatus.REQUESTED);
    when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

    assertThatThrownBy(() -> bookingService.accept(booking.getId(), OTHER_EMAIL))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessage("You cannot manage this sitter booking");
  }

  @Test
  void cancelRejectsNonOwner() {
    Booking booking = booking(100L, BookingStatus.REQUESTED);
    when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

    assertThatThrownBy(() -> bookingService.cancel(booking.getId(), OTHER_EMAIL))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessage("You cannot manage this owner booking");
  }

  @Test
  void rejectRejectsDifferentSitter() {
    Booking booking = booking(100L, BookingStatus.REQUESTED);
    when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

    assertThatThrownBy(() -> bookingService.reject(booking.getId(), OTHER_EMAIL))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessage("You cannot manage this sitter booking");
  }

  @Test
  void completeRejectsDifferentSitter() {
    Booking booking = booking(100L, BookingStatus.ACCEPTED);
    when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

    assertThatThrownBy(() -> bookingService.complete(booking.getId(), OTHER_EMAIL))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessage("You cannot manage this sitter booking");
  }

  @ParameterizedTest
  @MethodSource("validStatusTransitions")
  void statusActionsApplyAllowedTransitions(StatusTransition transition) {
    Booking booking = booking(100L, transition.initialStatus());
    when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
    saveReturnsBooking();

    BookingResponse response = transition.execute(bookingService, booking.getId());

    assertThat(response.status()).isEqualTo(transition.expectedStatus());
  }

  @ParameterizedTest
  @MethodSource("invalidStatusTransitions")
  void statusActionsRejectInvalidTransitions(StatusTransition transition) {
    Booking booking = booking(100L, transition.initialStatus());
    when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

    assertThatThrownBy(() -> transition.execute(bookingService, booking.getId()))
        .isInstanceOf(InvalidBookingOperationException.class)
        .hasMessage(transition.errorMessage());
  }

  @Test
  void getOwnerBookingsUsesUnfilteredRepositoryWhenStatusIsMissing() {
    Booking booking = booking(100L, BookingStatus.REQUESTED);
    when(ownerProfileRepository.findByUserEmail(OWNER_EMAIL)).thenReturn(Optional.of(owner));
    when(bookingRepository.findByOwnerId(owner.getId())).thenReturn(List.of(booking));

    List<BookingResponse> responses = bookingService.getOwnerBookings(OWNER_EMAIL, null);

    assertThat(responses).hasSize(1);
    verify(bookingRepository).findByOwnerId(owner.getId());
    verify(bookingRepository, never()).findByOwnerIdAndStatus(any(), any());
  }

  @Test
  void getOwnerBookingsUsesStatusFilterWhenProvided() {
    Booking booking = booking(100L, BookingStatus.ACCEPTED);
    when(ownerProfileRepository.findByUserEmail(OWNER_EMAIL)).thenReturn(Optional.of(owner));
    when(bookingRepository.findByOwnerIdAndStatus(owner.getId(), BookingStatus.ACCEPTED)).thenReturn(List.of(booking));

    List<BookingResponse> responses = bookingService.getOwnerBookings(OWNER_EMAIL, BookingStatus.ACCEPTED);

    assertThat(responses).hasSize(1);
    verify(bookingRepository).findByOwnerIdAndStatus(owner.getId(), BookingStatus.ACCEPTED);
    verify(bookingRepository, never()).findByOwnerId(any());
  }

  @Test
  void getSitterBookingsUsesUnfilteredRepositoryWhenStatusIsMissing() {
    Booking booking = booking(100L, BookingStatus.REQUESTED);
    when(sitterProfileRepository.findByUserEmail(SITTER_EMAIL)).thenReturn(Optional.of(sitter));
    when(bookingRepository.findBySitterId(sitter.getId())).thenReturn(List.of(booking));

    List<BookingResponse> responses = bookingService.getSitterBookings(SITTER_EMAIL, null);

    assertThat(responses).hasSize(1);
    verify(bookingRepository).findBySitterId(sitter.getId());
    verify(bookingRepository, never()).findBySitterIdAndStatus(any(), any());
  }

  @Test
  void getSitterBookingsUsesStatusFilterWhenProvided() {
    Booking booking = booking(100L, BookingStatus.ACCEPTED);
    when(sitterProfileRepository.findByUserEmail(SITTER_EMAIL)).thenReturn(Optional.of(sitter));
    when(bookingRepository.findBySitterIdAndStatus(sitter.getId(), BookingStatus.ACCEPTED)).thenReturn(List.of(booking));

    List<BookingResponse> responses = bookingService.getSitterBookings(SITTER_EMAIL, BookingStatus.ACCEPTED);

    assertThat(responses).hasSize(1);
    verify(bookingRepository).findBySitterIdAndStatus(sitter.getId(), BookingStatus.ACCEPTED);
    verify(bookingRepository, never()).findBySitterId(any());
  }

  private void saveReturnsBooking() {
    when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
  }

  private CreateBookingRequest createRequest(List<Long> petIds, String note) {
    CreateBookingRequest request = new CreateBookingRequest();
    request.setSitterId(sitter.getId());
    request.setStartDate(START_DATE);
    request.setEndDate(END_DATE);
    request.setPetIds(petIds);
    request.setNote(note);
    return request;
  }

  private UpdateBookingRequest updateRequest() {
    return updateRequest(START_DATE.plusHours(1), END_DATE.plusHours(1), null, null);
  }

  private UpdateBookingRequest updateRequest(
      LocalDateTime startDate,
      LocalDateTime endDate,
      List<Long> petIds,
      String note) {
    UpdateBookingRequest request = new UpdateBookingRequest();
    request.setStartDate(startDate);
    request.setEndDate(endDate);
    request.setPetIds(petIds);
    request.setNote(note);
    return request;
  }

  private Booking booking(Long id, BookingStatus status) {
    Booking booking = new Booking();
    booking.setId(id);
    booking.setOwner(owner);
    booking.setSitter(sitter);
    booking.setStartDate(START_DATE);
    booking.setEndDate(END_DATE);
    booking.setCreatedAt(START_DATE.minusDays(1));
    booking.setStatus(status);
    booking.setNote("Existing note");
    booking.setPricePerDaySnapshot(sitter.getPricePerDay());
    booking.setPets(List.of(pet));
    return booking;
  }

  private static OwnerProfile ownerProfile(Long id, User user) {
    OwnerProfile ownerProfile = new OwnerProfile();
    ownerProfile.setId(id);
    ownerProfile.setUser(user);
    return ownerProfile;
  }

  private static SitterProfile sitterProfile(Long id, User user, boolean published, String pricePerDay) {
    SitterProfile sitterProfile = new SitterProfile();
    sitterProfile.setId(id);
    sitterProfile.setUser(user);
    sitterProfile.setPublished(published);
    sitterProfile.setPricePerDay(new BigDecimal(pricePerDay));
    return sitterProfile;
  }

  private static Pet pet(Long id, OwnerProfile owner) {
    Pet pet = new Pet();
    pet.setId(id);
    pet.setOwnerProfile(owner);
    pet.setFirstName("Buddy");
    return pet;
  }

  private static User user(Long id, String email, String firstName, String lastName) {
    User user = new User();
    user.setId(id);
    user.setEmail(email);
    user.setFirstName(firstName);
    user.setLastName(lastName);
    return user;
  }

  private static Stream<String> participantEmails() {
    return Stream.of(OWNER_EMAIL, SITTER_EMAIL);
  }

  private static Stream<org.junit.jupiter.params.provider.Arguments> invalidDateRanges() {
    return Stream.of(
        org.junit.jupiter.params.provider.Arguments.of(START_DATE, START_DATE),
        org.junit.jupiter.params.provider.Arguments.of(END_DATE, START_DATE));
  }

  private static Stream<StatusTransition> validStatusTransitions() {
    return Stream.of(
        new StatusTransition("cancel requested", BookingStatus.REQUESTED, BookingStatus.CANCELLED,
            "Only requested or accepted bookings can be cancelled",
            (service, id) -> service.cancel(id, OWNER_EMAIL)),
        new StatusTransition("cancel accepted", BookingStatus.ACCEPTED, BookingStatus.CANCELLED,
            "Only requested or accepted bookings can be cancelled",
            (service, id) -> service.cancel(id, OWNER_EMAIL)),
        new StatusTransition("reject requested", BookingStatus.REQUESTED, BookingStatus.DECLINED,
            "Only requested bookings can be declined",
            (service, id) -> service.reject(id, SITTER_EMAIL)),
        new StatusTransition("complete accepted", BookingStatus.ACCEPTED, BookingStatus.COMPLETED,
            "Only accepted bookings can be completed",
            (service, id) -> service.complete(id, SITTER_EMAIL)));
  }

  private static Stream<StatusTransition> invalidStatusTransitions() {
    return Stream.of(
        new StatusTransition("cancel completed", BookingStatus.COMPLETED, BookingStatus.CANCELLED,
            "Only requested or accepted bookings can be cancelled",
            (service, id) -> service.cancel(id, OWNER_EMAIL)),
        new StatusTransition("reject accepted", BookingStatus.ACCEPTED, BookingStatus.DECLINED,
            "Only requested bookings can be declined",
            (service, id) -> service.reject(id, SITTER_EMAIL)),
        new StatusTransition("complete requested", BookingStatus.REQUESTED, BookingStatus.COMPLETED,
            "Only accepted bookings can be completed",
            (service, id) -> service.complete(id, SITTER_EMAIL)));
  }

  private record StatusTransition(
      String name,
      BookingStatus initialStatus,
      BookingStatus expectedStatus,
      String errorMessage,
      StatusAction action) {
    BookingResponse execute(BookingService service, Long bookingId) {
      return action.execute(service, bookingId);
    }

    @Override
    public String toString() {
      return name;
    }
  }

  @FunctionalInterface
  private interface StatusAction {
    BookingResponse execute(BookingService service, Long bookingId);
  }
}
