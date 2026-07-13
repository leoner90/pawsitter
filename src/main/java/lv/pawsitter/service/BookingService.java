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
import lv.pawsitter.repository.BookingRepository;
import lv.pawsitter.repository.OwnerProfileRepository;
import lv.pawsitter.repository.PetRepository;
import lv.pawsitter.repository.SitterProfileRepository;

@Service
@RequiredArgsConstructor
public class BookingService {
  private final BookingRepository bookingRepository;
  private final OwnerProfileRepository ownerProfileRepository;
  private final SitterProfileRepository sitterProfileRepository;
  private final PetRepository petRepository;

  @Transactional
  public BookingResponse createBooking(CreateBookingRequest request) {
    OwnerProfile owner = ownerProfileRepository.findById(request.getOwnerId())
        .orElseThrow(() -> new IllegalArgumentException("Owner profile not found"));

    SitterProfile sitter = sitterProfileRepository.findById(request.getSitterId())
        .orElseThrow(() -> new IllegalArgumentException("Sitter profile not found"));

    if (owner.getUser().getId().equals(sitter.getUser().getId())) {
      throw new IllegalArgumentException("Owner and sitter cannot be the same user");
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
    booking.setPets(pets);

    return BookingResponse.toResponse(bookingRepository.save(booking));
  }

  @Transactional(readOnly = true)
  public BookingResponse getBookingById(Long id) {
    return BookingResponse.toResponse(getBooking(id));
  }

  @Transactional
  public BookingResponse updateBooking(Long bookingId, UpdateBookingRequest request) {
    Booking booking = getBooking(bookingId);

    if (booking.getStatus() != BookingStatus.REQUESTED) {
      throw new IllegalArgumentException("Only requested bookings can be updated");
    }

    LocalDateTime startDate = request.getStartDate() != null ? request.getStartDate() : booking.getStartDate();
    LocalDateTime endDate = request.getEndDate() != null ? request.getEndDate() : booking.getEndDate();

    if (!endDate.isAfter(startDate)) {
      throw new IllegalArgumentException("End date must be after start date");
    }

    booking.setStartDate(startDate);
    booking.setEndDate(endDate);

    if (request.getPetIds() != null) {
      if (request.getPetIds().isEmpty()) {
        throw new IllegalArgumentException("Select at least one pet");
      }

      List<Pet> pets = request.getPetIds().stream()
          .distinct()
          .map(petId -> getOwnerPet(petId, booking.getOwner()))
          .collect(Collectors.toList());

      booking.setPets(pets);
    }

    return BookingResponse.toResponse(bookingRepository.save(booking));
  }

  @Transactional(readOnly = true)
  public List<BookingResponse> getOwnerBookings(Long ownerId) {
    return bookingRepository.findByOwnerId(ownerId).stream()
        .map(BookingResponse::toResponse)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<BookingResponse> getSitterBookings(Long sitterId) {
    return bookingRepository.findBySitterId(sitterId).stream()
        .map(BookingResponse::toResponse)
        .collect(Collectors.toList());
  }

  @Transactional
  public BookingResponse accept(Long bookingId) {
    return changeStatus(
        bookingId,
        BookingStatus.ACCEPTED,
        EnumSet.of(BookingStatus.REQUESTED),
        "Only requested bookings can be accepted");
  }

  @Transactional
  public BookingResponse cancel(Long bookingId) {
    return changeStatus(
        bookingId,
        BookingStatus.CANCELLED,
        EnumSet.of(BookingStatus.REQUESTED, BookingStatus.ACCEPTED),
        "Only requested or accepted bookings can be cancelled");
  }

  @Transactional
  public BookingResponse reject(Long bookingId) {
    return changeStatus(
        bookingId,
        BookingStatus.DECLINED,
        EnumSet.of(BookingStatus.REQUESTED),
        "Only requested bookings can be declined");
  }

  @Transactional
  public BookingResponse complete(Long bookingId) {
    return changeStatus(
        bookingId,
        BookingStatus.COMPLETED,
        EnumSet.of(BookingStatus.ACCEPTED),
        "Only accepted bookings can be completed");
  }

  private BookingResponse changeStatus(Long bookingId, BookingStatus newStatus, Set<BookingStatus> allowedStatuses,
      String errorMessage) {
    Booking booking = getBooking(bookingId);

    if (!allowedStatuses.contains(booking.getStatus())) {
      throw new IllegalArgumentException(errorMessage);
    }

    booking.setStatus(newStatus);
    return BookingResponse.toResponse(bookingRepository.save(booking));
  }

  private Booking getBooking(Long id) {
    return bookingRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
  }

  // checks if pet exists and belongs to owner
  private Pet getOwnerPet(Long petId, OwnerProfile owner) {
    Pet pet = petRepository.findById(petId)
        .orElseThrow(() -> new IllegalArgumentException("Pet not found"));

    if (!pet.getUser().getId().equals(owner.getUser().getId())) {
      throw new IllegalArgumentException("Pet does not belong to this owner");
    }

    return pet;
  }

}
