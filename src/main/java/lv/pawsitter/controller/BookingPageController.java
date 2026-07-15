package lv.pawsitter.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lv.pawsitter.dto.BookingResponse;
import lv.pawsitter.dto.CreateBookingRequest;
import lv.pawsitter.dto.PetResponseDto;
import lv.pawsitter.dto.ReviewRequest;
import lv.pawsitter.dto.ReviewResponse;
import lv.pawsitter.dto.UpdateBookingRequest;
import lv.pawsitter.entity.BookingStatus;
import lv.pawsitter.entity.OwnerProfile;
import lv.pawsitter.service.BookingService;
import lv.pawsitter.service.OwnerProfileService;
import lv.pawsitter.service.PetService;
import lv.pawsitter.service.ReviewService;
import lv.pawsitter.service.SitterProfileService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class BookingPageController {

  private final BookingService bookingService;
  private final SitterProfileService sitterProfileService;
  private final OwnerProfileService ownerProfileService;
  private final PetService petService;
  private final ReviewService reviewService;

  @GetMapping("/owner/bookings")
  public String ownerBookings(
      @RequestParam(required = false) BookingStatus status,
      Authentication authentication,
      Model model
  ) {
    OwnerProfile ownerProfile = ownerProfileService.getProfileByUserEmail(authentication.getName());
    List<PetResponseDto> pets = petService.getPetsByOwnerId(ownerProfile.getId());
    List<BookingResponse> bookings = bookingService.getOwnerBookings(authentication.getName(), status);

    model.addAttribute("bookings", getOwnerBookingViews(bookings, pets));
    model.addAttribute("selectedStatus", status);
    model.addAttribute("statuses", BookingStatus.values());
    return "owner/bookings";
  }

  @GetMapping("/sitter/bookings")
  public String sitterBookings(
      @RequestParam(required = false) BookingStatus status,
      Authentication authentication,
      Model model
  ) {
    List<BookingResponse> bookings = bookingService.getSitterBookings(authentication.getName(), status);

    model.addAttribute("bookings", getBookingViews(bookings));
    model.addAttribute("selectedStatus", status);
    model.addAttribute("statuses", BookingStatus.values());
    return "sitter/bookings";
  }

  @GetMapping("/owner/bookings/new")
  public String newBooking(
      @RequestParam Long sitterId,
      Authentication authentication,
      Model model
  ) {
    CreateBookingRequest bookingRequest = new CreateBookingRequest();
    bookingRequest.setSitterId(sitterId);

    populateBookingForm(model, authentication.getName(), sitterId, bookingRequest);
    return "owner/bookingForm";
  }

  @GetMapping("/owner/bookings/{id}/edit")
  public String editBooking(
      @PathVariable Long id,
      Authentication authentication,
      Model model
  ) {
    BookingResponse booking = bookingService.getBookingById(id, authentication.getName());
    UpdateBookingRequest bookingRequest = new UpdateBookingRequest();
    bookingRequest.setStartDate(booking.startDate());
    bookingRequest.setEndDate(booking.endDate());
    bookingRequest.setPetIds(booking.petIds());
    bookingRequest.setNote(booking.note());

    populateEditForm(model, authentication.getName(), booking, bookingRequest);
    return "owner/bookingEditForm";
  }

  @GetMapping("/owner/bookings/{id}")
  public String ownerBookingDetails(
      @PathVariable Long id,
      Authentication authentication,
      Model model
  ) {
    BookingResponse booking = bookingService.getBookingById(id, authentication.getName());
    populateDetailsPage(model, booking, "/owner/bookings", true, authentication.getName());
    return "bookingDetails";
  }

  @GetMapping("/sitter/bookings/{id}")
  public String sitterBookingDetails(
      @PathVariable Long id,
      Authentication authentication,
      Model model
  ) {
    BookingResponse booking = bookingService.getBookingById(id, authentication.getName());
    populateDetailsPage(model, booking, "/sitter/bookings", false, authentication.getName());
    return "bookingDetails";
  }

  @PostMapping("/owner/bookings")
  public String createBooking(
      Authentication authentication,
      @Valid @ModelAttribute("bookingRequest") CreateBookingRequest bookingRequest,
      BindingResult bindingResult,
      Model model,
      RedirectAttributes redirectAttributes
  ) {
    Long sitterId = bookingRequest.getSitterId();

    if (bindingResult.hasErrors()) {
      populateBookingForm(model, authentication.getName(), sitterId, bookingRequest);
      return "owner/bookingForm";
    }

    try {
      bookingService.createBooking(authentication.getName(), bookingRequest);
    } catch (RuntimeException exception) {
      bindingResult.reject("booking.create", exception.getMessage());
      populateBookingForm(model, authentication.getName(), sitterId, bookingRequest);
      return "owner/bookingForm";
    }

    redirectAttributes.addFlashAttribute("bookingSuccess", "Booking request sent.");
    return "redirect:/owner/bookings";
  }

  @PostMapping("/owner/bookings/{id}/edit")
  public String updateBooking(
      @PathVariable Long id,
      Authentication authentication,
      @Valid @ModelAttribute("bookingRequest") UpdateBookingRequest bookingRequest,
      BindingResult bindingResult,
      Model model,
      RedirectAttributes redirectAttributes
  ) {
    BookingResponse booking = bookingService.getBookingById(id, authentication.getName());

    if (bindingResult.hasErrors()) {
      populateEditForm(model, authentication.getName(), booking, bookingRequest);
      return "owner/bookingEditForm";
    }

    try {
      bookingService.updateBooking(id, authentication.getName(), bookingRequest);
    } catch (RuntimeException exception) {
      bindingResult.reject("booking.update", exception.getMessage());
      populateEditForm(model, authentication.getName(), booking, bookingRequest);
      return "owner/bookingEditForm";
    }

    redirectAttributes.addFlashAttribute("bookingSuccess", "Booking updated.");
    return "redirect:/owner/bookings";
  }

  @PostMapping("/owner/bookings/{id}/cancel")
  public String cancelBooking(
      @PathVariable Long id,
      Authentication authentication,
      RedirectAttributes redirectAttributes
  ) {
    try {
      bookingService.cancel(id, authentication.getName());
      redirectAttributes.addFlashAttribute("bookingSuccess", "Booking cancelled.");
    } catch (RuntimeException exception) {
      redirectAttributes.addFlashAttribute("bookingError", exception.getMessage());
    }

    return "redirect:/owner/bookings";
  }

  @GetMapping("/owner/bookings/{id}/review")
  public String newOwnerReview(
      @PathVariable Long id,
      Authentication authentication,
      Model model
  ) {
    BookingResponse booking = bookingService.getBookingById(id, authentication.getName());
    populateReviewForm(model, booking, new ReviewRequest(), "/owner/bookings/" + id + "/review", "/owner/bookings/" + id);
    return "owner/reviewForm";
  }

  @PostMapping("/owner/bookings/{id}/review")
  public String createOwnerReview(
      @PathVariable Long id,
      Authentication authentication,
      @Valid @ModelAttribute("reviewRequest") ReviewRequest reviewRequest,
      BindingResult bindingResult,
      Model model,
      RedirectAttributes redirectAttributes
  ) {
    return createReview(id, authentication, reviewRequest, bindingResult, model, redirectAttributes, true);
  }

  @GetMapping("/sitter/bookings/{id}/review")
  public String newSitterReview(
      @PathVariable Long id,
      Authentication authentication,
      Model model
  ) {
    BookingResponse booking = bookingService.getBookingById(id, authentication.getName());
    populateReviewForm(model, booking, new ReviewRequest(), "/sitter/bookings/" + id + "/review", "/sitter/bookings/" + id);
    return "owner/reviewForm";
  }

  @PostMapping("/sitter/bookings/{id}/review")
  public String createSitterReview(
      @PathVariable Long id,
      Authentication authentication,
      @Valid @ModelAttribute("reviewRequest") ReviewRequest reviewRequest,
      BindingResult bindingResult,
      Model model,
      RedirectAttributes redirectAttributes
  ) {
    return createReview(id, authentication, reviewRequest, bindingResult, model, redirectAttributes, false);
  }

  private String createReview(
      Long id,
      Authentication authentication,
      ReviewRequest reviewRequest,
      BindingResult bindingResult,
      Model model,
      RedirectAttributes redirectAttributes,
      boolean ownerView
  ) {
    BookingResponse booking = bookingService.getBookingById(id, authentication.getName());
    String detailsUrl = ownerView ? "/owner/bookings/" + id : "/sitter/bookings/" + id;
    String formAction = detailsUrl + "/review";

    if (bindingResult.hasErrors()) {
      populateReviewForm(model, booking, reviewRequest, formAction, detailsUrl);
      return "owner/reviewForm";
    }

    try {
      reviewService.createReview(reviewRequest, authentication.getName());
    } catch (RuntimeException exception) {
      bindingResult.reject("review.create", exception.getMessage());
      populateReviewForm(model, booking, reviewRequest, formAction, detailsUrl);
      return "owner/reviewForm";
    }

    redirectAttributes.addFlashAttribute("bookingSuccess", "Review submitted.");
    return "redirect:" + detailsUrl;
  }

  @PostMapping("/sitter/bookings/{id}/accept")
  public String acceptBooking(
      @PathVariable Long id,
      Authentication authentication,
      RedirectAttributes redirectAttributes
  ) {
    try {
      bookingService.accept(id, authentication.getName());
      redirectAttributes.addFlashAttribute("bookingSuccess", "Booking accepted.");
    } catch (RuntimeException exception) {
      redirectAttributes.addFlashAttribute("bookingError", exception.getMessage());
    }

    return "redirect:/sitter/bookings";
  }

  @PostMapping("/sitter/bookings/{id}/reject")
  public String rejectBooking(
      @PathVariable Long id,
      Authentication authentication,
      RedirectAttributes redirectAttributes
  ) {
    try {
      bookingService.reject(id, authentication.getName());
      redirectAttributes.addFlashAttribute("bookingSuccess", "Booking rejected.");
    } catch (RuntimeException exception) {
      redirectAttributes.addFlashAttribute("bookingError", exception.getMessage());
    }

    return "redirect:/sitter/bookings";
  }

  @PostMapping("/sitter/bookings/{id}/complete")
  public String completeBooking(
      @PathVariable Long id,
      Authentication authentication,
      RedirectAttributes redirectAttributes
  ) {
    try {
      bookingService.complete(id, authentication.getName());
      redirectAttributes.addFlashAttribute("bookingSuccess", "Booking completed.");
    } catch (RuntimeException exception) {
      redirectAttributes.addFlashAttribute("bookingError", exception.getMessage());
    }

    return "redirect:/sitter/bookings";
  }

  private void populateBookingForm(
      Model model,
      String ownerEmail,
      Long sitterId,
      CreateBookingRequest bookingRequest
  ) {
    OwnerProfile ownerProfile = ownerProfileService.getProfileByUserEmail(ownerEmail);
    List<PetResponseDto> pets = petService.getPetsByOwnerId(ownerProfile.getId());

    model.addAttribute("bookingRequest", bookingRequest);
    model.addAttribute("pets", pets);

    if (sitterId != null) {
      model.addAttribute("sitter", sitterProfileService.getSitterById(sitterId));
    }
  }

  private void populateEditForm(
      Model model,
      String ownerEmail,
      BookingResponse booking,
      UpdateBookingRequest bookingRequest
  ) {
    OwnerProfile ownerProfile = ownerProfileService.getProfileByUserEmail(ownerEmail);
    List<PetResponseDto> pets = petService.getPetsByOwnerId(ownerProfile.getId());

    model.addAttribute("booking", booking);
    model.addAttribute("bookingRequest", bookingRequest);
    model.addAttribute("pets", pets);
    model.addAttribute("sitter", sitterProfileService.getSitterById(booking.sitterId()));
  }

  private void populateDetailsPage(
      Model model,
      BookingResponse booking,
      String backUrl,
      boolean ownerView,
      String userEmail
  ) {
    List<ReviewResponse> reviews = reviewService.getReviewByBooking(booking.id());
    Long currentUserId = ownerView
        ? ownerProfileService.getProfileByUserEmail(userEmail).getUser().getId()
        : sitterProfileService.getProfileByUserEmail(userEmail).getUser().getId();
    boolean currentUserReviewed = currentUserId != null && reviews.stream()
        .anyMatch(review -> review.getReviewerId().equals(currentUserId));

    model.addAttribute("bookingView", getBookingView(booking));
    model.addAttribute("reviews", reviews);
    model.addAttribute("backUrl", backUrl);
    model.addAttribute("ownerView", ownerView);
    model.addAttribute("reviewUrl", ownerView ? "/owner/bookings/" + booking.id() + "/review" : "/sitter/bookings/" + booking.id() + "/review");
    model.addAttribute("canReview", booking.status() == BookingStatus.COMPLETED && !currentUserReviewed);
  }

  private void populateReviewForm(
      Model model,
      BookingResponse booking,
      ReviewRequest reviewRequest,
      String formAction,
      String backUrl
  ) {
    reviewRequest.setBookingId(booking.id());

    model.addAttribute("booking", booking);
    model.addAttribute("reviewRequest", reviewRequest);
    model.addAttribute("formAction", formAction);
    model.addAttribute("backUrl", backUrl);
  }

  private Map<Long, String> getPetNamesById(List<PetResponseDto> pets) {
    return pets.stream()
        .collect(Collectors.toMap(PetResponseDto::getId, PetResponseDto::getFirstName));
  }

  private List<BookingView> getOwnerBookingViews(List<BookingResponse> bookings, List<PetResponseDto> pets) {
    Map<Long, String> petNamesById = getPetNamesById(pets);

    return bookings.stream()
        .map(booking -> new BookingView(
            booking,
            booking.petIds().stream()
                .map(petId -> petNamesById.getOrDefault(petId, "Pet #" + petId))
                .toList()
        ))
        .toList();
  }

  private List<BookingView> getBookingViews(List<BookingResponse> bookings) {
    Map<Long, String> petNamesById = bookings.stream()
        .flatMap(booking -> booking.petIds().stream())
        .distinct()
        .collect(Collectors.toMap(petId -> petId, this::getPetName));

    return bookings.stream()
        .map(booking -> new BookingView(
            booking,
            booking.petIds().stream()
                .map(petId -> petNamesById.getOrDefault(petId, "Pet #" + petId))
                .toList()
        ))
        .toList();
  }

  private BookingView getBookingView(BookingResponse booking) {
    return new BookingView(
        booking,
        booking.petIds().stream()
            .map(this::getPetName)
            .toList()
    );
  }

  private String getPetName(Long petId) {
    try {
      return petService.getById(petId).getFirstName();
    } catch (RuntimeException exception) {
      return "Pet #" + petId;
    }
  }

  public record BookingView(BookingResponse booking, List<String> petNames) {
  }
}
