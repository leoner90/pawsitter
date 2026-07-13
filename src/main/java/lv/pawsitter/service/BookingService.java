package lv.pawsitter.service;

import java.util.List;
import lv.pawsitter.dto.BookingResponse;
import lv.pawsitter.dto.CreateBookingRequest;
import lv.pawsitter.dto.UpdateBookingRequest;

public interface BookingService
{
  BookingResponse createBooking(CreateBookingRequest request);

  BookingResponse getBookingById(Long id);

  BookingResponse updateBooking(Long bookingId, UpdateBookingRequest request);

  List<BookingResponse> getOwnerBookings(Long ownerId);

  List<BookingResponse> getSitterBookings(Long sitterId);

  BookingResponse accept(Long bookingId);

  BookingResponse cancel(Long bookingId);

  BookingResponse reject(Long bookingId);

  BookingResponse complete(Long bookingId);
}
