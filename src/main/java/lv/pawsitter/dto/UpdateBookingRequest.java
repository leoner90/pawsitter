package lv.pawsitter.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateBookingRequest
{
    @FutureOrPresent(message = "Start date cannot be in the past")
    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private List<@NotNull(message = "Pet id is required") Long> petIds;

    @AssertTrue(message = "Provide dates or pets to update")
    public boolean hasUpdates()
    {
        return startDate != null || endDate != null || petIds != null;
    }
}
