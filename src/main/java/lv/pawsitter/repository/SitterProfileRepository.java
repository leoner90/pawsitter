package lv.pawsitter.repository;

import lv.pawsitter.entity.SitterProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SitterProfileRepository extends JpaRepository<SitterProfile, Long>
{
    Optional<SitterProfile> findByUserEmail(String email);
    List<SitterProfile> findByPublishedTrue(); // for future use (filtering)
}