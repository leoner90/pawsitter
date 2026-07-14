package lv.pawsitter.repository;

import lv.pawsitter.entity.Booking;
import lv.pawsitter.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long>
{
    List<Booking> findByOwnerId(Long ownerId);
    List<Booking> findBySitterId(Long sitterId);
    List<Booking> findByOwnerIdAndStatus(Long ownerId, BookingStatus status);
    List<Booking> findBySitterIdAndStatus(Long sitterId, BookingStatus status);
    boolean existsBySitterIdAndStatusAndStartDateLessThanAndEndDateGreaterThan(
            Long sitterId,
            BookingStatus status,
            LocalDateTime endDate,
            LocalDateTime startDate
    );
}
