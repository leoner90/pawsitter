package lv.pawsitter.repository;

import lv.pawsitter.entity.OwnerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OwnerProfileRepository extends JpaRepository<OwnerProfile, Long>
{
    Optional<OwnerProfile> findByUserEmail(String email);
}