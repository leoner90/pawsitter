package lv.pawsitter.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lv.pawsitter.entity.Booking;
import lv.pawsitter.entity.BookingStatus;
import lv.pawsitter.entity.Pet;

public record BookingResponse(
        Long id,
        Long ownerId,
        String ownerName,
        Long sitterId,
        String sitterName,
        LocalDateTime startDate,
        LocalDateTime endDate,
        LocalDateTime createdAt,
        BookingStatus status,
        List<Long> petIds,
        boolean reviewed
)
{
    public static BookingResponse toResponse(Booking booking)
    {
        return new BookingResponse(
                booking.getId(),
                booking.getOwner().getId(),
                getFullName(booking.getOwner().getUser().getFirstName(), booking.getOwner().getUser().getLastName()),
                booking.getSitter().getId(),
                getFullName(booking.getSitter().getUser().getFirstName(), booking.getSitter().getUser().getLastName()),
                booking.getStartDate(),
                booking.getEndDate(),
                booking.getCreatedAt(),
                booking.getStatus(),
                booking.getPets().stream()
                        .map(Pet::getId)
                        .collect(Collectors.toList()),
                booking.getReview() != null
        );
    }

    private static String getFullName(String firstName, String lastName)
    {
        return firstName + " " + lastName;
    }
}
