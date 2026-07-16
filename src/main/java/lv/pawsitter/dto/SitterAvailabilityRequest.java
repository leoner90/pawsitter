package lv.pawsitter.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record SitterAvailabilityRequest(

        @NotNull(message = "Start date is required")
        @FutureOrPresent(message = "Start date cannot be in the past")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate
)
{
    @AssertTrue(message = "End date must be on or after start date")
    public boolean isDateRangeValid()
    {
        if (startDate == null || endDate == null)
        {
            return true;
        }

        return !endDate.isBefore(startDate);
    }
}