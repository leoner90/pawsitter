package lv.pawsitter.repository;

import lv.pawsitter.entity.SitterAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SitterAvailabilityRepository extends JpaRepository<SitterAvailability, Long>
{
    List<SitterAvailability> findBySitterProfileId(Long sitterProfileId);
}