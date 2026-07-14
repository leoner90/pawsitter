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
public class BookingServiceImpl implements BookingService
{
  private final BookingRepository bookingRepository;
  private final OwnerProfileRepository ownerProfileRepository;
  private final SitterProfileRepository sitterProfileRepository;
  private final PetRepository petRepository;

  @Override
  @Transactional
  public BookingResponse createBooking(CreateBookingRequest request)
  {
    OwnerProfile owner = ownerProfileRepository.findById(request.getOwnerId())
        .orElseThrow(() -> new InvalidBookingOperationException("Owner profile not found"));

    SitterProfile sitter = sitterProfileRepository.findById(request.getSitterId())
        .orElseThrow(() -> new InvalidBookingOperationException("Sitter profile not found"));

    if (owner.getUser().getId().equals(sitter.getUser().getId()))
    {
      throw new InvalidBookingOperationException("Owner and sitter cannot be the same user");
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

  @Override
  @Transactional(readOnly = true)
  public BookingResponse getBookingById(Long id)
  {
    return BookingResponse.toResponse(getBooking(id));
  }

  @Override
  @Transactional
  public BookingResponse updateBooking(Long bookingId, UpdateBookingRequest request)
  {
    Booking booking = getBooking(bookingId);

    if (booking.getStatus() != BookingStatus.REQUESTED)
    {
      throw new InvalidBookingOperationException("Only requested bookings can be updated");
    }

    LocalDateTime startDate = request.getStartDate() != null ? request.getStartDate() : booking.getStartDate();
    LocalDateTime endDate = request.getEndDate() != null ? request.getEndDate() : booking.getEndDate();

    if (!endDate.isAfter(startDate))
    {
      throw new InvalidBookingOperationException("End date must be after start date");
    }

    booking.setStartDate(startDate);
    booking.setEndDate(endDate);

    if (request.getPetIds() != null)
    {
      if (request.getPetIds().isEmpty())
      {
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
  public List<BookingResponse> getOwnerBookings(Long ownerId)
  {
    return bookingRepository.findByOwnerId(ownerId).stream()
        .map(BookingResponse::toResponse)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<BookingResponse> getSitterBookings(Long sitterId)
  {
    return bookingRepository.findBySitterId(sitterId).stream()
        .map(BookingResponse::toResponse)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional
  public BookingResponse accept(Long bookingId)
  {
    return changeStatus(
        bookingId,
        BookingStatus.ACCEPTED,
        EnumSet.of(BookingStatus.REQUESTED),
        "Only requested bookings can be accepted");
  }

  @Override
  @Transactional
  public BookingResponse cancel(Long bookingId)
  {
    return changeStatus(
        bookingId,
        BookingStatus.CANCELLED,
        EnumSet.of(BookingStatus.REQUESTED, BookingStatus.ACCEPTED),
        "Only requested or accepted bookings can be cancelled");
  }

  @Override
  @Transactional
  public BookingResponse reject(Long bookingId)
  {
    return changeStatus(
        bookingId,
        BookingStatus.DECLINED,
        EnumSet.of(BookingStatus.REQUESTED),
        "Only requested bookings can be declined");
  }

  @Override
  @Transactional
  public BookingResponse complete(Long bookingId)
  {
    return changeStatus(
        bookingId,
        BookingStatus.COMPLETED,
        EnumSet.of(BookingStatus.ACCEPTED),
        "Only accepted bookings can be completed");
  }

  private BookingResponse changeStatus(Long bookingId, BookingStatus newStatus, Set<BookingStatus> allowedStatuses,
      String errorMessage)
  {
    Booking booking = getBooking(bookingId);

    if (!allowedStatuses.contains(booking.getStatus()))
    {
      throw new InvalidBookingOperationException(errorMessage);
    }

    booking.setStatus(newStatus);
    return BookingResponse.toResponse(bookingRepository.save(booking));
  }

  private Booking getBooking(Long id)
  {
    return bookingRepository.findById(id)
        .orElseThrow(() -> new BookingNotFoundException("Booking not found"));
  }

  private Pet getOwnerPet(Long petId, OwnerProfile owner)
  {
    return petRepository.findByIdAndOwnerProfileId(petId, owner.getId())
        .orElseThrow(() -> new InvalidBookingOperationException("Pet not found for this owner"));
  }
}
