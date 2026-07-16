package lv.pawsitter.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lv.pawsitter.dto.BookingResponse;
import lv.pawsitter.dto.CreateBookingRequest;
import lv.pawsitter.dto.UpdateBookingRequest;
import lv.pawsitter.entity.BookingStatus;
import lv.pawsitter.service.BookingService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/bookings")
public class BookingController
{
  private final BookingService bookingService;

  @PostMapping
  public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody CreateBookingRequest request, Authentication authentication)
  {
    return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createBooking(authentication.getName(), request));
  }

  @GetMapping("/{id}")
  public ResponseEntity<BookingResponse> getBookingById(@PathVariable Long id, Authentication authentication)
  {
    return ResponseEntity.ok(bookingService.getBookingById(id, authentication.getName()));
  }

  @PutMapping("/{id}")
  public ResponseEntity<BookingResponse> updateBooking(@PathVariable Long id, @Valid @RequestBody UpdateBookingRequest request, Authentication authentication)
  {
    return ResponseEntity.ok(bookingService.updateBooking(id, authentication.getName(), request));
  }

  @GetMapping("/my")
  public ResponseEntity<List<BookingResponse>> getOwnerBookings(Authentication authentication, @RequestParam(required = false) BookingStatus status)
  {
    return ResponseEntity.ok(bookingService.getOwnerBookings(authentication.getName(), status));
  }

  @GetMapping("/assigned")
  public ResponseEntity<List<BookingResponse>> getSitterBookings(Authentication authentication, @RequestParam(required = false) BookingStatus status)
  {
    return ResponseEntity.ok(bookingService.getSitterBookings(authentication.getName(), status));
  }

  @PatchMapping("/{id}/accept")
  public ResponseEntity<BookingResponse> acceptBooking(@PathVariable Long id, Authentication authentication)
  {
    return ResponseEntity.ok(bookingService.accept(id, authentication.getName()));
  }

  @PatchMapping("/{id}/cancel")
  public ResponseEntity<BookingResponse> cancelBooking(@PathVariable Long id, Authentication authentication)
  {
    return ResponseEntity.ok(bookingService.cancel(id, authentication.getName()));
  }

  @PatchMapping("/{id}/reject")
  public ResponseEntity<BookingResponse> rejectBooking(@PathVariable Long id, Authentication authentication)
  {
    return ResponseEntity.ok(bookingService.reject(id, authentication.getName()));
  }

  @PatchMapping("/{id}/complete")
  public ResponseEntity<BookingResponse> completeBooking(@PathVariable Long id, Authentication authentication)
  {
    return ResponseEntity.ok(bookingService.complete(id, authentication.getName()));
  }
}
