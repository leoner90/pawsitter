package lv.pawsitter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lv.pawsitter.dto.ReviewRequest;
import lv.pawsitter.dto.ReviewResponse;
import lv.pawsitter.security.JwtService;
import lv.pawsitter.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ReviewControllerUnitTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReviewService reviewService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtService jwtService;

    private final Authentication authentication =
            new UsernamePasswordAuthenticationToken("jane@example.com", null);


    private ReviewRequest request;
    private ReviewResponse response;

    @BeforeEach
    void setUp() {
        request = new ReviewRequest();
        request.setBookingId(100L);
        request.setRating(5);
        request.setReviewComment("Great job!");

        response = new ReviewResponse(
                500L,
                100L,
                1L,
                "Jane Doe",
                2L,
                "John Smith",
                5,
                "Great job!",
                LocalDateTime.now()
        );
    }

    @Test
    void createReview_validRequest_returnsOk() throws Exception {
        ReviewRequest request = new ReviewRequest();
        request.setBookingId(100L);
        request.setRating(5);
        request.setReviewComment("Great job!");

        when(reviewService.createReview(any(ReviewRequest.class), eq("jane@example.com")))
                .thenReturn(response);

        mockMvc.perform(post("/api/reviews")
                        .principal(authentication)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.reviewerId").value(1));
    }

    @Test
    void createReview_invalidRating_returnsBadRequest() throws Exception {
        ReviewRequest request = new ReviewRequest();
        request.setBookingId(100L);
        request.setRating(10);
        request.setReviewComment("Great job!");

        mockMvc.perform(post("/api/reviews")
                        .principal(authentication)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getReviewById_found_returnsOk() throws Exception {
        when(reviewService.getReviewById(500L)).thenReturn(response);

        mockMvc.perform(get("/api/reviews/500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(500));
    }

    @Test
    void getReviewByBooking_returnsListOfReviews() throws Exception {
        when(reviewService.getReviewByBooking(100L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/reviews/booking/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getReviewsReceived_returnsListOfReviews() throws Exception {
        when(reviewService.getReviewsReceivedBy(2L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/reviews/received/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].revieweeId").value(2));
    }

    @Test
    void getReviewsWritten_returnsListOfReviews() throws Exception {
        when(reviewService.getReviewsWrittenBy(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/reviews/written/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reviewerId").value(1));
    }

    @Test
    void getAllReviews_returnsList() throws Exception {
        when(reviewService.getAllReviews()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void updateReview_validRequest_returnsOk() throws Exception {
        ReviewRequest request = new ReviewRequest();
        request.setBookingId(100L);
        request.setRating(4);
        request.setReviewComment("Updated");

        when(reviewService.updateReview(eq(500L), any(ReviewRequest.class), eq("jane@example.com")))
                .thenReturn(response);

        mockMvc.perform(put("/api/reviews/500")
                        .principal(authentication)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteReview_validRequest_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/reviews/500")
                        .principal(authentication))
                .andExpect(status().isNoContent());
    }

}
