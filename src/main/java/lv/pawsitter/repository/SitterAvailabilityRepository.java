package lv.pawsitter.repository;

import lv.pawsitter.entity.SitterAvailability;
import lv.pawsitter.entity.SitterProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
        SELECT DISTINCT availability.sitterProfile
        FROM SitterAvailability availability
        WHERE availability.sitterProfile.published = true
        AND availability.startDate <= :startDate
        AND availability.endDate >= :endDate
        """)
    List<SitterProfile> findFullyAvailableSitters(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("""
        SELECT DISTINCT availability.sitterProfile
        FROM SitterAvailability availability
        WHERE availability.sitterProfile.published = true
        AND availability.startDate <= :endDate
        AND availability.endDate >= :startDate
        """)
    List<SitterProfile> findPartiallyAvailableSitters(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
