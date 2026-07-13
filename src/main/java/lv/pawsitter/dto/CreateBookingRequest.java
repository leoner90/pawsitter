package lv.pawsitter.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateBookingRequest
{
    @NotNull(message = "Owner profile is required")
    private Long ownerId;

    @NotNull(message = "Sitter profile is required")
    private Long sitterId;

    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date cannot be in the past")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    private LocalDateTime endDate;

    @NotEmpty(message = "Select at least one pet")
    private List<@NotNull(message = "Pet id is required") Long> petIds = new ArrayList<>();

    @AssertTrue(message = "End date must be after start date")
    public boolean isDateRangeValid()
    {
        if (startDate == null || endDate == null)
        {
            return true;
        }

        return endDate.isAfter(startDate);
    }
}
