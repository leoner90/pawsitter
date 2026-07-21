package lv.pawsitter.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lv.pawsitter.dto.BookingResponse;
import lv.pawsitter.dto.CreateBookingRequest;
import lv.pawsitter.dto.UpdateBookingRequest;
import lv.pawsitter.entity.Booking;
import lv.pawsitter.entity.BookingStatus;
import lv.pawsitter.entity.OwnerProfile;
import lv.pawsitter.entity.Pet;
import lv.pawsitter.entity.SitterProfile;
import lv.pawsitter.exception.BookingNotFoundException;
import lv.pawsitter.exception.InvalidBookingOperationException;
import lv.pawsitter.exception.PetNotFoundException;
import lv.pawsitter.exception.UserNotFoundException;
import lv.pawsitter.repository.BookingRepository;
import lv.pawsitter.repository.OwnerProfileRepository;
import lv.pawsitter.repository.PetRepository;
import lv.pawsitter.repository.SitterAvailabilityRepository;
import lv.pawsitter.repository.SitterProfileRepository;
import lv.pawsitter.entity.SitterAvailability;
import java.time.LocalDate;


@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
  private final BookingRepository bookingRepository;
  private final OwnerProfileRepository ownerProfileRepository;
  private final SitterProfileRepository sitterProfileRepository;
  private final PetRepository petRepository;
  private final SitterAvailabilityRepository sitterAvailabilityRepository;
  private final SitterProfileService sitterProfileService;

  private static final Comparator<Booking> BOOKING_DISPLAY_ORDER = Comparator
          .comparing((Booking booking) -> booking.getEndDate().isBefore(LocalDateTime.now()))
          .thenComparing(Booking::getStartDate);

  @Override
  @Transactional
  public BookingResponse createBooking(String ownerEmail, CreateBookingRequest request) {
      requireRequest(request, "Booking request must not be null");
      OwnerProfile owner = getOwnerByEmail(ownerEmail);
      Long sitterId = requireId(request.getSitterId(), "Sitter id must not be null");

      SitterProfile sitter = sitterProfileRepository.findById(sitterId)
          .orElseThrow(() -> new InvalidBookingOperationException("Sitter profile not found"));

      if (owner.getUser().getId().equals(sitter.getUser().getId())) {
        throw new InvalidBookingOperationException("Owner and sitter cannot be the same user");
      }

      if (!sitter.isPublished()) {
        throw new InvalidBookingOperationException("Sitter profile is not available for booking");
      }

      requireSitterAvailable(sitter, request.getStartDate(), request.getEndDate());

      List<Pet> pets = request.getPetIds().stream()
          .distinct()
          .map(petId -> getOwnerPet(petId, owner))
          .collect(Collectors.toList());

      Booking booking = new Booking();
      booking.setOwner(owner);
      booking.setSitter(sitter);
      booking.setStartDate(request.getStartDate());
      booking.setEndDate(request.getEndDate());
      booking.setStatus(BookingStatus.REQUESTED);
      booking.setNote(normalizeNote(request.getNote()));
      booking.setPricePerDaySnapshot(sitter.getPricePerDay());
      booking.setPets(pets);

      //remove availability  dates - ideally should be done after payment, but then we need redo whole logic as other users can book same dates 100 times
      // and sitter can remove dates as well while in request state, and when accept one of the booking others should be canceled and deleted
      //to free pets etc. so to many bugs so we will delete dates on booking request and restore if canceled!
      Booking savedBooking = bookingRepository.save(booking);
      removeBookedAvailability(savedBooking);

      return BookingResponse.toResponse(savedBooking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long id, String userEmail) {
      Booking booking = getBooking(id);
      requireParticipant(booking, userEmail);

      return BookingResponse.toResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse updateBooking(Long bookingId, String ownerEmail, UpdateBookingRequest request) {
      requireRequest(request, "Update booking request must not be null");
      Booking booking = getBooking(bookingId);
      requireOwner(booking, ownerEmail);

      if (booking.getStatus() != BookingStatus.REQUESTED) {
        throw new InvalidBookingOperationException("Only requested bookings can be updated");
      }

      LocalDateTime startDate = request.getStartDate() != null ? request.getStartDate() : booking.getStartDate();
      LocalDateTime endDate = request.getEndDate() != null ? request.getEndDate() : booking.getEndDate();

      if (!endDate.isAfter(startDate)) {
        throw new InvalidBookingOperationException("End date must be after start date");
      }

      if (request.getStartDate() != null || request.getEndDate() != null) {
        requireSitterAvailable(booking.getSitter(), startDate, endDate);
      }

      booking.setStartDate(startDate);
      booking.setEndDate(endDate);

      if (request.getNote() != null) {
        booking.setNote(normalizeNote(request.getNote()));
      }

      if (request.getPetIds() != null) {
        if (request.getPetIds().isEmpty()) {
          throw new InvalidBookingOperationException("Select at least one pet");
        }

        List<Pet> pets = request.getPetIds().stream()
            .distinct()
            .map(petId -> getOwnerPet(petId, booking.getOwner()))
            .collect(Collectors.toList());

        booking.setPets(pets);
      }

      return BookingResponse.toResponse(bookingRepository.save(booking));
    }

  @Override
  @Transactional(readOnly = true)
  public List<BookingResponse> getOwnerBookings(String ownerEmail, BookingStatus status) {
    OwnerProfile owner = getOwnerByEmail(ownerEmail);

    List<Booking> bookings = status == null
            ? bookingRepository.findByOwnerIdOrderByStartDateAsc(owner.getId())
            : bookingRepository.findByOwnerIdAndStatusOrderByStartDateAsc(owner.getId(), status);

    return bookings.stream()
            .sorted(BOOKING_DISPLAY_ORDER)
            .map(BookingResponse::toResponse)
            .collect(Collectors.toList());
  }


  @Override
  @Transactional(readOnly = true)
  public List<BookingResponse> getSitterBookings(String sitterEmail, BookingStatus status) {
    SitterProfile sitter = getSitterByEmail(sitterEmail);

    List<Booking> bookings = status == null
            ? bookingRepository.findBySitterIdOrderByStartDateAsc(sitter.getId())
            : bookingRepository.findBySitterIdAndStatusOrderByStartDateAsc(sitter.getId(), status);

    return bookings.stream()
            .sorted(BOOKING_DISPLAY_ORDER)
            .map(BookingResponse::toResponse)
            .collect(Collectors.toList());
  }

    @Override
    @Transactional
    public BookingResponse accept(Long bookingId, String sitterEmail) {
      Booking booking = getBooking(bookingId);
      requireSitter(booking, sitterEmail);

      if (booking.getStatus() != BookingStatus.REQUESTED) {
        throw new InvalidBookingOperationException("Only requested bookings can be accepted");
      }

      if (hasAcceptedOverlap(booking)) {
        throw new InvalidBookingOperationException("Sitter already has an accepted booking for these dates");
      }

      booking.setStatus(BookingStatus.ACCEPTED);
      return BookingResponse.toResponse(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public BookingResponse cancel(Long bookingId, String ownerEmail) {
      Booking booking = getBooking(bookingId);
      requireOwner(booking, ownerEmail);

      if (booking.isPaid())
      {
        throw new InvalidBookingOperationException("A paid booking cannot be cancelled");
      }

      //change status first then restore dates and return
      BookingResponse response = changeStatus(
              booking,
              BookingStatus.CANCELLED,
              EnumSet.of(BookingStatus.REQUESTED, BookingStatus.ACCEPTED),
              "Only requested or accepted bookings can be cancelled"
      );

      // Restore reserved dates only after cancellation succeeds
      restoreAvailability(booking);

      return response;

    }

    @Override
    @Transactional
    public BookingResponse reject(Long bookingId, String sitterEmail) {
      Booking booking = getBooking(bookingId);
      requireSitter(booking, sitterEmail);

      if (booking.isPaid())
      {
        throw new InvalidBookingOperationException("A paid booking cannot be rejected");
      }

      //reject cancel first
      BookingResponse response = changeStatus(
              booking,
              BookingStatus.DECLINED,
              EnumSet.of(BookingStatus.REQUESTED),
              "Only requested bookings can be declined"
      );

      //restore availability dates
      restoreAvailability(booking);

      return response;
    }

    @Override
    @Transactional
    public BookingResponse complete(Long bookingId, String sitterEmail) {
      Booking booking = getBooking(bookingId);
      requireSitter(booking, sitterEmail);

      //only paid booking can be completed!!!
      if (!booking.isPaid())
      {
        throw new InvalidBookingOperationException("Booking must be paid before it can be completed");
      }

      return changeStatus(
          booking,
          BookingStatus.COMPLETED,
          EnumSet.of(BookingStatus.ACCEPTED),
          "Only accepted bookings can be completed");
    }

    private BookingResponse changeStatus(Booking booking, BookingStatus newStatus, Set<BookingStatus> allowedStatuses,
        String errorMessage) {
      if (!allowedStatuses.contains(booking.getStatus())) {
        throw new InvalidBookingOperationException(errorMessage);
      }

      booking.setStatus(newStatus);
      return BookingResponse.toResponse(bookingRepository.save(booking));
    }

    private Booking getBooking(Long id) {
      return bookingRepository.findById(requireId(id, "Booking id must not be null"))
          .orElseThrow(() -> new BookingNotFoundException("Booking not found"));
    }

    private OwnerProfile getOwnerByEmail(String email) {
      return ownerProfileRepository.findByUserEmail(normalizeEmail(email))
          .orElseThrow(() -> new UserNotFoundException("Owner profile not found"));
    }

    private SitterProfile getSitterByEmail(String email) {
      return sitterProfileRepository.findByUserEmail(normalizeEmail(email))
          .orElseThrow(() -> new UserNotFoundException("Sitter profile not found"));
    }

    private void requireParticipant(Booking booking, String email) {
      String normalizedEmail = normalizeEmail(email);

      if (!isOwner(booking, normalizedEmail) && !isSitter(booking, normalizedEmail)) {
        throw new AccessDeniedException("You cannot access this booking");
      }
    }

    private void requireOwner(Booking booking, String email) {
      if (!isOwner(booking, normalizeEmail(email))) {
        throw new AccessDeniedException("You cannot manage this owner booking");
      }
    }

    private void requireSitter(Booking booking, String email) {
      if (!isSitter(booking, normalizeEmail(email))) {
        throw new AccessDeniedException("You cannot manage this sitter booking");
      }
    }

    private boolean isOwner(Booking booking, String email) {
      return booking.getOwner().getUser().getEmail().equalsIgnoreCase(email);
    }

    private boolean isSitter(Booking booking, String email) {
      return booking.getSitter().getUser().getEmail().equalsIgnoreCase(email);
    }

    private String normalizeEmail(String email) {
      if (email == null || email.isBlank()) {
        throw new InvalidBookingOperationException("Email must not be blank");
      }

      return email.trim().toLowerCase();
    }

    private String normalizeNote(String note) {
      return note == null ? "" : note.trim();
    }

    private boolean hasAcceptedOverlap(Booking booking) {
      return bookingRepository.existsBySitterIdAndStatusAndStartDateLessThanAndEndDateGreaterThan(
          booking.getSitter().getId(),
          BookingStatus.ACCEPTED,
          booking.getEndDate(),
          booking.getStartDate());
    }

    private void requireSitterAvailable(SitterProfile sitter, LocalDateTime startDate, LocalDateTime endDate) {
      boolean available = sitterAvailabilityRepository
          .existsBySitterProfileIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
              sitter.getId(),
              startDate.toLocalDate(),
              endDate.toLocalDate());

      if (!available) {
        throw new InvalidBookingOperationException("Sitter is not available for selected dates");
      }
    }

    private Pet getOwnerPet(Long petId, OwnerProfile owner) {
      return petRepository.findByIdAndOwnerProfileId(requireId(petId, "Pet id must not be null"), owner.getId())
          .orElseThrow(() -> new PetNotFoundException("Pet not found for this owner"));
    }

    private <T> T requireRequest(T request, String message) {
      if (request == null) {
        throw new InvalidBookingOperationException(message);
      }

      return request;
    }

    private Long requireId(Long id, String message) {
      if (id == null) {
        throw new InvalidBookingOperationException(message);
      }

      return id;
    }


    // Checks that this booking belongs to the current sitter
    @Override
    @Transactional(readOnly = true)
    public Booking getBookingForSitter(Long bookingId, String sitterEmail)
    {
      Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new BookingNotFoundException("Booking not found"));

      String bookingSitterEmail =
              booking.getSitter()
                      .getUser()
                      .getEmail();

      //checking by email
      if (!bookingSitterEmail.equalsIgnoreCase(sitterEmail))
      {
        throw new AccessDeniedException("You cannot view this owner's profile");
      }

      return booking;
    }

//change payment status
//contains the Stripe Checkout Session ID. At the moment,  method receives it but does not use it. for history  saving in future
  @Override
  @Transactional
  public void confirmPayment(Long bookingId, String stripeSessionId)
  {
    Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new BookingNotFoundException("Booking not found: " + bookingId));

    if (booking.isPaid())
    {
      return;
    }

    if (booking.getStatus() != BookingStatus.ACCEPTED)
    {
      throw new InvalidBookingOperationException("Only accepted bookings can be paid");
    }

    //set status paid -> true -> save
    booking.setPaid(true);
    bookingRepository.save(booking);

    //reduce dates (idealy should be done here, but then we need redo whole logic as other users can book same dates 100 times
    // and sitter can remove dates as well while in request state, and when accept one of the booking others should be canceled and deleted
    //to free pets etc. so to many bugs we will delete dates on booking request and restore if canceled!
    //removeBookedAvailability(booking);
  }

//just private method to remove availability from sitter profile when booked
  private void removeBookedAvailability(Booking booking)
  {
    LocalDate bookedStart = booking.getStartDate().toLocalDate();
    LocalDate bookedEnd = booking.getEndDate().toLocalDate();
    SitterProfile sitterProfile = booking.getSitter(); // get sitter
    List<SitterAvailability> availabilityRanges = sitterAvailabilityRepository.findBySitterProfileId(sitterProfile.getId()); // Get all availability ranges

    for (SitterAvailability availability : availabilityRanges)
    {
      LocalDate availableStart = availability.getStartDate();
      LocalDate availableEnd = availability.getEndDate();

      //checks whether the availability range and booking range do not overlap at all:
      boolean doesNotOverlap = availableEnd.isBefore(bookedStart) || availableStart.isAfter(bookedEnd);

      //These dates are unrelated to the booking, so skip this range and check the next one.
      if (doesNotOverlap)
      {
        continue;
      }

      // Booking covers the complete availability range -> delete them
      if (!bookedStart.isAfter(availableStart) && !bookedEnd.isBefore(availableEnd))
      {
        sitterAvailabilityRepository.delete(availability);
      }

      // Booking removes the beginning of the range - move the availability start to the day after the booking ends.
      else if (!bookedStart.isAfter(availableStart))
      {
        availability.setStartDate(bookedEnd.plusDays(1));
        sitterAvailabilityRepository.save(availability);
      }

      // Booking removes the end of the range reduce  the availability from end
      //move the availability end to the day before the booking starts.
      else if (!bookedEnd.isBefore(availableEnd))
      {
        availability.setEndDate(bookedStart.minusDays(1));
        sitterAvailabilityRepository.save(availability);
      }

      // Booking is in the middle, so split the range into two 20 -30 Available,  booked 25-27 -> 20-24 and 28 - 30 remain
      else
      {
        //temp
        LocalDate originalEndDate = availableEnd;

        // Keep the available dates before the booking. save end of the booking
        availability.setEndDate(bookedStart.minusDays(1));
        sitterAvailabilityRepository.save(availability);

        // Create a second availability range after the booking. remove booked dates
        SitterAvailability secondRange = new SitterAvailability();
        secondRange.setSitterProfile(sitterProfile);

        secondRange.setStartDate(bookedEnd.plusDays(1));
        secondRange.setEndDate(originalEndDate);

        sitterAvailabilityRepository.save(secondRange);
      }
    }

    //checks (query) whether the sitter still has any availability ending today or later. If not, the sitter is automatically unpublished.
    boolean hasCurrentOrFutureAvailability =
            sitterAvailabilityRepository.existsBySitterProfileIdAndEndDateGreaterThanEqual(sitterProfile.getId(), LocalDate.now());

    if (!hasCurrentOrFutureAvailability)
    {
      sitterProfile.setPublished(false);
      sitterProfileRepository.save(sitterProfile);
    }
  }

  //if booking canceled restore dates
  private void restoreAvailability(Booking booking)
  {
    sitterProfileService.restoreAvailability(booking.getSitter(), booking.getStartDate().toLocalDate(), booking.getEndDate().toLocalDate());
  }

  //returns all availability dates which are pre-booked for this sitter
  @Override
  @Transactional(readOnly = true)
  public List<BookingResponse> getActiveSitterBookings(String sitterEmail)
  {
    SitterProfile sitter = getSitterByEmail(sitterEmail);

    return bookingRepository.findBySitterIdOrderByStartDateAsc(sitter.getId())
            .stream()
            .filter(booking ->
                    booking.getStatus() == BookingStatus.REQUESTED
                            || booking.getStatus() == BookingStatus.ACCEPTED
            )
            .map(BookingResponse::toResponse)
            .toList();
  }
}
