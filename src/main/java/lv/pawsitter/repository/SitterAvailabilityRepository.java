package lv.pawsitter.repository;

import lv.pawsitter.entity.SitterAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SitterAvailabilityRepository extends JpaRepository<SitterAvailability, Long>
{
    List<SitterAvailability> findBySitterProfileId(Long sitterProfileId);

    List<SitterAvailability> findBySitterProfileIdAndEndDateGreaterThanEqualOrderByStartDateAsc(
            Long sitterProfileId,
            LocalDate date
    );

    boolean existsBySitterProfileIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long sitterProfileId,
            LocalDate startDate,
            LocalDate endDate
    );

    boolean existsBySitterProfileIdAndEndDateGreaterThanEqual(Long sitterProfileId, LocalDate date);
}
