package lv.pawsitter.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lv.pawsitter.dto.BookingResponse;
import lv.pawsitter.dto.CreateBookingRequest;
import lv.pawsitter.dto.UpdateBookingRequest;
import lv.pawsitter.service.BookingService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/bookings")
public class BookingController
{
  private final BookingService bookingService;

  @PostMapping
  public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody CreateBookingRequest request)
  {
    return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createBooking(request));
  }

  @GetMapping("/{id}")
  public ResponseEntity<BookingResponse> getBookingById(@PathVariable Long id)
  {
    return ResponseEntity.ok(bookingService.getBookingById(id));
  }

  @PutMapping("/{id}")
  public ResponseEntity<BookingResponse> updateBooking(@PathVariable Long id, @Valid @RequestBody UpdateBookingRequest request)
  {
    return ResponseEntity.ok(bookingService.updateBooking(id, request));
  }

  @GetMapping("/owners/{ownerId}")
  public ResponseEntity<List<BookingResponse>> getOwnerBookings(@PathVariable Long ownerId)
  {
    return ResponseEntity.ok(bookingService.getOwnerBookings(ownerId));
  }

  @GetMapping("/sitters/{sitterId}")
  public ResponseEntity<List<BookingResponse>> getSitterBookings(@PathVariable Long sitterId)
  {
    return ResponseEntity.ok(bookingService.getSitterBookings(sitterId));
  }

  @PatchMapping("/{id}/accept")
  public ResponseEntity<BookingResponse> acceptBooking(@PathVariable Long id)
  {
    return ResponseEntity.ok(bookingService.accept(id));
  }

  @PatchMapping("/{id}/cancel")
  public ResponseEntity<BookingResponse> cancelBooking(@PathVariable Long id)
  {
    return ResponseEntity.ok(bookingService.cancel(id));
  }

  @PatchMapping("/{id}/reject")
  public ResponseEntity<BookingResponse> rejectBooking(@PathVariable Long id)
  {
    return ResponseEntity.ok(bookingService.reject(id));
  }

  @PatchMapping("/{id}/complete")
  public ResponseEntity<BookingResponse> completeBooking(@PathVariable Long id)
  {
    return ResponseEntity.ok(bookingService.complete(id));
  }
}
