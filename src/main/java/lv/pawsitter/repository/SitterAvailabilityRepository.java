package lv.pawsitter.repository;

import lv.pawsitter.entity.SitterAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import lv.pawsitter.entity.SitterProfile;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface SitterAvailabilityRepository extends JpaRepository<SitterAvailability, Long>
{
    List<SitterAvailability> findBySitterProfileId(Long sitterProfileId);

    //THIS Query finds published sitters whose one availability range fully covers the dates selected by the owner.
    @Query("""
        SELECT DISTINCT availability.sitterProfile
        FROM SitterAvailability availability
        WHERE availability.sitterProfile.published = true
        AND availability.startDate <= :startDate
        AND availability.endDate >= :endDate
        """)
    List<SitterProfile> findFullyAvailableSitters(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    //THIS Query finds published sitters whose are Partially in range (like 1 day out of 10)
    @Query("""
        SELECT DISTINCT availability.sitterProfile
        FROM SitterAvailability availability
        WHERE availability.sitterProfile.published = true
        AND availability.startDate <= :endDate
        AND availability.endDate >= :startDate
        """)
    List<SitterProfile> findPartiallyAvailableSitters(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}