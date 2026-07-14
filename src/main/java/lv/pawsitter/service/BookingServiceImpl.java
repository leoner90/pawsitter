package lv.pawsitter.service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
import lv.pawsitter.repository.BookingRepository;
import lv.pawsitter.repository.OwnerProfileRepository;
import lv.pawsitter.repository.PetRepository;
import lv.pawsitter.repository.SitterProfileRepository;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
  private final BookingRepository bookingRepository;
  private final OwnerProfileRepository ownerProfileRepository;
  private final SitterProfileRepository sitterProfileRepository;
  private final PetRepository petRepository;

  @Override
  @Transactional
  public BookingResponse createBooking(String ownerEmail, CreateBookingRequest request) {
    OwnerProfile owner = getOwnerByEmail(ownerEmail);

    SitterProfile sitter = sitterProfileRepository.findById(request.getSitterId())
        .orElseThrow(() -> new InvalidBookingOperationException("Sitter profile not found"));

    if (owner.getUser().getId().equals(sitter.getUser().getId())) {
      throw new InvalidBookingOperationException("Owner and sitter cannot be the same user");
    }

    if (!sitter.isPublished()) {
      throw new InvalidBookingOperationException("Sitter profile is not available for booking");
    }

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

    return BookingResponse.toResponse(bookingRepository.save(booking));
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
    Booking booking = getBooking(bookingId);

    if (booking.getStatus() != BookingStatus.REQUESTED) {
      throw new InvalidBookingOperationException("Only requested bookings can be updated");
    }

    LocalDateTime startDate = request.getStartDate() != null ? request.getStartDate() : booking.getStartDate();
    LocalDateTime endDate = request.getEndDate() != null ? request.getEndDate() : booking.getEndDate();

    if (!endDate.isAfter(startDate)) {
      throw new InvalidBookingOperationException("End date must be after start date");
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
        ? bookingRepository.findByOwnerId(owner.getId())
        : bookingRepository.findByOwnerIdAndStatus(owner.getId(), status);

    return bookings.stream()
        .map(BookingResponse::toResponse)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<BookingResponse> getSitterBookings(String sitterEmail, BookingStatus status) {
    SitterProfile sitter = getSitterByEmail(sitterEmail);

    List<Booking> bookings = status == null
        ? bookingRepository.findBySitterId(sitter.getId())
        : bookingRepository.findBySitterIdAndStatus(sitter.getId(), status);

    return bookings.stream()
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

    return changeStatus(
        bookingId,
        BookingStatus.CANCELLED,
        EnumSet.of(BookingStatus.REQUESTED, BookingStatus.ACCEPTED),
        "Only requested or accepted bookings can be cancelled");
  }

  @Override
  @Transactional
  public BookingResponse reject(Long bookingId, String sitterEmail) {
    Booking booking = getBooking(bookingId);
    requireSitter(booking, sitterEmail);

    return changeStatus(
        bookingId,
        BookingStatus.DECLINED,
        EnumSet.of(BookingStatus.REQUESTED),
        "Only requested bookings can be declined");
  }

  @Override
  @Transactional
  public BookingResponse complete(Long bookingId, String sitterEmail) {
    Booking booking = getBooking(bookingId);
    requireSitter(booking, sitterEmail);

    return changeStatus(
        bookingId,
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
    return bookingRepository.findById(id)
        .orElseThrow(() -> new BookingNotFoundException("Booking not found"));
  }

  private OwnerProfile getOwnerByEmail(String email) {
    return ownerProfileRepository.findByUserEmail(normalizeEmail(email))
        .orElseThrow(() -> new InvalidBookingOperationException("Owner profile not found"));
  }

  private SitterProfile getSitterByEmail(String email) {
    return sitterProfileRepository.findByUserEmail(normalizeEmail(email))
        .orElseThrow(() -> new InvalidBookingOperationException("Sitter profile not found"));
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

  private Pet getOwnerPet(Long petId, OwnerProfile owner) {
    return petRepository.findByIdAndOwnerProfileId(petId, owner.getId())
        .orElseThrow(() -> new InvalidBookingOperationException("Pet not found for this owner"));
  }
}
