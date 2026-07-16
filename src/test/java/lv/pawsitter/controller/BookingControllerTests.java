package lv.pawsitter.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import lv.pawsitter.dto.BookingResponse;
import lv.pawsitter.dto.CreateBookingRequest;
import lv.pawsitter.dto.UpdateBookingRequest;
import lv.pawsitter.entity.BookingStatus;
import lv.pawsitter.security.JwtAuthenticationFilter;
import lv.pawsitter.service.BookingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BookingController.class)
@AutoConfigureMockMvc(addFilters = false)
class BookingControllerTests {
  private static final String USER_EMAIL = "owner@example.com";
  private static final LocalDateTime START_DATE = LocalDateTime.of(2026, 8, 10, 10, 0);
  private static final LocalDateTime END_DATE = LocalDateTime.of(2026, 8, 12, 18, 0);

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private BookingService bookingService;

  @MockitoBean
  private JwtAuthenticationFilter jwtAuthenticationFilter;

  @Test
  void createBookingReturnsCreatedAndPassesAuthenticatedUserToService() throws Exception {
    CreateBookingRequest request = createRequest();
    BookingResponse response = response(100L, BookingStatus.REQUESTED);
    when(bookingService.createBooking(eq(USER_EMAIL), any(CreateBookingRequest.class))).thenReturn(response);

    mockMvc.perform(post("/bookings")
        .principal(authentication())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(response.id()))
        .andExpect(jsonPath("$.status").value(BookingStatus.REQUESTED.name()))
        .andExpect(jsonPath("$.note").value(response.note()))
        .andExpect(jsonPath("$.petIds[0]").value(30L));

    ArgumentCaptor<CreateBookingRequest> requestCaptor = ArgumentCaptor.forClass(CreateBookingRequest.class);
    verify(bookingService).createBooking(eq(USER_EMAIL), requestCaptor.capture());
    CreateBookingRequest capturedRequest = requestCaptor.getValue();
    assertThat(capturedRequest.getSitterId()).isEqualTo(request.getSitterId());
    assertThat(capturedRequest.getPetIds()).isEqualTo(request.getPetIds());
    assertThat(capturedRequest.getNote()).isEqualTo(request.getNote());
  }

  @ParameterizedTest
  @MethodSource("invalidCreateRequests")
  void createBookingRejectsInvalidRequest(CreateBookingRequest request) throws Exception {
    mockMvc.perform(post("/bookings")
        .principal(authentication())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verify(bookingService, never()).createBooking(any(), any());
  }

  @Test
  void getBookingByIdReturnsBookingAndPassesAuthenticatedUserToService() throws Exception {
    BookingResponse response = response(100L, BookingStatus.REQUESTED);
    when(bookingService.getBookingById(100L, USER_EMAIL)).thenReturn(response);

    mockMvc.perform(get("/bookings/{id}", 100L).principal(authentication()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(100L));

    verify(bookingService).getBookingById(100L, USER_EMAIL);
  }

  @Test
  void updateBookingReturnsUpdatedBookingAndPassesRequestToService() throws Exception {
    UpdateBookingRequest request = updateRequest();
    BookingResponse response = response(100L, BookingStatus.REQUESTED);
    when(bookingService.updateBooking(eq(100L), eq(USER_EMAIL), any(UpdateBookingRequest.class))).thenReturn(response);

    mockMvc.perform(put("/bookings/{id}", 100L)
        .principal(authentication())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(100L));

    ArgumentCaptor<UpdateBookingRequest> requestCaptor = ArgumentCaptor.forClass(UpdateBookingRequest.class);
    verify(bookingService).updateBooking(eq(100L), eq(USER_EMAIL), requestCaptor.capture());
    UpdateBookingRequest capturedRequest = requestCaptor.getValue();
    assertThat(capturedRequest.getStartDate()).isEqualTo(request.getStartDate());
    assertThat(capturedRequest.getEndDate()).isEqualTo(request.getEndDate());
    assertThat(capturedRequest.getNote()).isEqualTo(request.getNote());
  }

  @Test
  void updateBookingRejectsRequestWithoutUpdates() throws Exception {
    UpdateBookingRequest request = new UpdateBookingRequest();

    mockMvc.perform(put("/bookings/{id}", 100L)
        .principal(authentication())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verify(bookingService, never()).updateBooking(any(), any(), any());
  }

  @Test
  void getOwnerBookingsSupportsOptionalStatusFilter() throws Exception {
    BookingResponse response = response(100L, BookingStatus.ACCEPTED);
    when(bookingService.getOwnerBookings(USER_EMAIL, BookingStatus.ACCEPTED)).thenReturn(List.of(response));

    mockMvc.perform(get("/bookings/my")
        .principal(authentication())
        .param("status", BookingStatus.ACCEPTED.name()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(100L))
        .andExpect(jsonPath("$[0].status").value(BookingStatus.ACCEPTED.name()));

    verify(bookingService).getOwnerBookings(USER_EMAIL, BookingStatus.ACCEPTED);
  }

  @Test
  void getSitterBookingsSupportsMissingStatusFilter() throws Exception {
    BookingResponse response = response(100L, BookingStatus.REQUESTED);
    when(bookingService.getSitterBookings(USER_EMAIL, null)).thenReturn(List.of(response));

    mockMvc.perform(get("/bookings/assigned").principal(authentication()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(100L));

    verify(bookingService).getSitterBookings(USER_EMAIL, null);
  }

  @ParameterizedTest
  @MethodSource("statusActions")
  void statusActionEndpointsCallMatchingServiceMethod(StatusActionCase actionCase) throws Exception {
    BookingResponse response = response(100L, actionCase.expectedStatus());
    actionCase.stub(bookingService, 100L, USER_EMAIL, response);

    mockMvc.perform(patch(actionCase.path(), 100L).principal(authentication()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(actionCase.expectedStatus().name()));

    actionCase.verify(bookingService, 100L, USER_EMAIL);
  }

  private CreateBookingRequest createRequest() {
    CreateBookingRequest request = new CreateBookingRequest();
    request.setSitterId(20L);
    request.setStartDate(START_DATE);
    request.setEndDate(END_DATE);
    request.setPetIds(List.of(30L));
    request.setNote("Please give medicine");
    return request;
  }

  private UpdateBookingRequest updateRequest() {
    UpdateBookingRequest request = new UpdateBookingRequest();
    request.setStartDate(START_DATE.plusDays(1));
    request.setEndDate(END_DATE.plusDays(1));
    request.setNote("Updated note");
    return request;
  }

  private BookingResponse response(Long id, BookingStatus status) {
    return new BookingResponse(
        id,
        10L,
        "Olivia Owner",
        20L,
        "Sam Sitter",
        START_DATE,
        END_DATE,
        START_DATE.minusDays(1),
        status,
        "Please give medicine",
        new BigDecimal("25.00"),
        List.of(30L),
        false);
  }

  private Authentication authentication() {
    return new UsernamePasswordAuthenticationToken(USER_EMAIL, "password");
  }

  private static Stream<CreateBookingRequest> invalidCreateRequests() {
    CreateBookingRequest missingSitter = validCreateRequest();
    missingSitter.setSitterId(null);

    CreateBookingRequest emptyPets = validCreateRequest();
    emptyPets.setPetIds(List.of());

    CreateBookingRequest invalidDates = validCreateRequest();
    invalidDates.setEndDate(invalidDates.getStartDate());

    return Stream.of(missingSitter, emptyPets, invalidDates);
  }

  private static CreateBookingRequest validCreateRequest() {
    CreateBookingRequest request = new CreateBookingRequest();
    request.setSitterId(20L);
    request.setStartDate(START_DATE);
    request.setEndDate(END_DATE);
    request.setPetIds(List.of(30L));
    return request;
  }

  private static Stream<StatusActionCase> statusActions() {
    return Stream.of(
        new StatusActionCase("/bookings/{id}/accept", BookingStatus.ACCEPTED),
        new StatusActionCase("/bookings/{id}/cancel", BookingStatus.CANCELLED),
        new StatusActionCase("/bookings/{id}/reject", BookingStatus.DECLINED),
        new StatusActionCase("/bookings/{id}/complete", BookingStatus.COMPLETED));
  }

  private record StatusActionCase(String path, BookingStatus expectedStatus) {
    void stub(BookingService service, Long bookingId, String email, BookingResponse response) {
      switch (expectedStatus) {
        case ACCEPTED -> when(service.accept(bookingId, email)).thenReturn(response);
        case CANCELLED -> when(service.cancel(bookingId, email)).thenReturn(response);
        case DECLINED -> when(service.reject(bookingId, email)).thenReturn(response);
        case COMPLETED -> when(service.complete(bookingId, email)).thenReturn(response);
        default -> throw new IllegalArgumentException("Unsupported status action: " + expectedStatus);
      }
    }

    void verify(BookingService service, Long bookingId, String email) {
      switch (expectedStatus) {
        case ACCEPTED -> org.mockito.Mockito.verify(service).accept(bookingId, email);
        case CANCELLED -> org.mockito.Mockito.verify(service).cancel(bookingId, email);
        case DECLINED -> org.mockito.Mockito.verify(service).reject(bookingId, email);
        case COMPLETED -> org.mockito.Mockito.verify(service).complete(bookingId, email);
        default -> throw new IllegalArgumentException("Unsupported status action: " + expectedStatus);
      }
    }
  }
}
