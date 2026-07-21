package lv.pawsitter.repository;

import lv.pawsitter.entity.Recovery;
import lv.pawsitter.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecoveryRepository extends JpaRepository<Recovery, Integer> {
    Optional<Recovery> findByRecoveryToken(String token);

    void deleteByUser(User user);
}