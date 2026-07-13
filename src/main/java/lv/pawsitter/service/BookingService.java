package lv.pawsitter.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lv.pawsitter.entity.Booking;
import lv.pawsitter.repository.BookingRepository;

@Service
@RequiredArgsConstructor
public class BookingService {
  private final BookingRepository bookingRepository;

  public BookingResponse createBooking(CreateBookingRequest request){

  }

  public BookingResponse getBookingById(Long id){
    
  }

  public List<BookingResponse> getBookingsByUserId(Long id){

  }

  public Booking updateBooking(UpdateBookingRequest request){

  }

  public void deleteBooking(Long id){

  }
}
