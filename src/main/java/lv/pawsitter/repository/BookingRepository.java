package lv.pawsitter.repository;

import lv.pawsitter.entity.Booking;
import lv.pawsitter.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long>
{
    List<Booking> findByOwnerIdOrderByStartDateAsc(Long ownerId);
    List<Booking> findBySitterIdOrderByStartDateAsc(Long sitterId);
    List<Booking> findByOwnerIdAndStatusOrderByStartDateAsc(Long ownerId, BookingStatus status);
    List<Booking> findBySitterIdAndStatusOrderByStartDateAsc(Long sitterId, BookingStatus status);

    boolean existsBySitterIdAndStatusAndStartDateLessThanAndEndDateGreaterThan(
            Long sitterId,
            BookingStatus status,
            LocalDateTime endDate,
            LocalDateTime startDate
    );

    boolean existsByPetsIdAndStatusIn(Long petId, Collection<BookingStatus> statuses);

    boolean existsByPetsId(Long petId);

    @Query("""
    SELECT COUNT(booking) > 0
    FROM Booking booking
    WHERE booking.sitter.id = :sitterId
    AND booking.status IN :statuses
    AND booking.startDate <= :requestedEnd
    AND booking.endDate >= :requestedStart
    """)
    boolean existsOverlappingBooking(
            @Param("sitterId") Long sitterId,
            @Param("requestedStart") LocalDateTime requestedStart,
            @Param("requestedEnd") LocalDateTime requestedEnd,
            @Param("statuses") Collection<BookingStatus> statuses
    );
}