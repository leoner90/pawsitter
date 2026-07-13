package lv.pawsitter.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ReviewResponse {
    private Long id;
    private Long bookingId;
    private Long reviewerId;
    private String reviewerName;
    private Integer rating;
    private String reviewComment;
    private LocalDateTime createdAt;
}
