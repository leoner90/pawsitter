package lv.pawsitter.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lv.pawsitter.dto.ReviewRequest;
import lv.pawsitter.dto.ReviewResponse;
import lv.pawsitter.service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication)
    {
        return ResponseEntity.ok(reviewService.createReview(request, authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponse> getReviewById(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.getReviewById(id));
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<ReviewResponse> getReviewByBooking(@PathVariable Long bookingId)
    {
        return ResponseEntity.ok(reviewService.getReviewByBooking(bookingId));
    }

    @GetMapping("/received/{userId}")
    public ResponseEntity<List<ReviewResponse>> getReviewsByReviewer(@PathVariable Long userId)
    {
        return ResponseEntity.ok(reviewService.getReviewsReceivedBy(userId));
    }

    @GetMapping("/written/{userId}")
    public ResponseEntity<List<ReviewResponse>> getReviewsWritten(@PathVariable Long userId)
    {
        return ResponseEntity.ok(reviewService.getReviewsWrittenBy(userId));
    }

    @GetMapping
    public ResponseEntity<List<ReviewResponse>> getAllReviews()
    {
        return ResponseEntity.ok(reviewService.getAllReviews());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication)
    {
        return ResponseEntity.ok(reviewService.updateReview(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id, Authentication authentication)

    {
        reviewService.deleteReview(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }


}
