package lv.pawsitter.service;

import java.util.List;
import lv.pawsitter.dto.BookingResponse;
import lv.pawsitter.dto.CreateBookingRequest;
import lv.pawsitter.dto.UpdateBookingRequest;
import lv.pawsitter.entity.BookingStatus;

public interface BookingService
{
  BookingResponse createBooking(String ownerEmail, CreateBookingRequest request);

  BookingResponse getBookingById(Long id, String userEmail);

  BookingResponse updateBooking(Long bookingId, String ownerEmail, UpdateBookingRequest request);

  List<BookingResponse> getOwnerBookings(String ownerEmail, BookingStatus status);

  List<BookingResponse> getSitterBookings(String sitterEmail, BookingStatus status);

  BookingResponse accept(Long bookingId, String sitterEmail);

  BookingResponse cancel(Long bookingId, String ownerEmail);

  BookingResponse reject(Long bookingId, String sitterEmail);

  BookingResponse complete(Long bookingId, String sitterEmail);
}
