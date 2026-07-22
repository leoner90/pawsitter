package lv.pawsitter.repository;


import jakarta.persistence.EntityManager;
import lv.pawsitter.entity.Recovery;
import lv.pawsitter.entity.User;
import lv.pawsitter.model.RoleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class RecoveryRepositoryUnitTests {
    @Autowired
    private RecoveryRepository recoveryRepository;

    @Autowired
    private EntityManager entityManager;

    private User user;

    @BeforeEach
    public void setup() {
        user = new User();
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setEmail("jane@example.com");
        user.setPassword("encodedPassword");
        user.setPhoneNumber("+37120000001");
        user.setRole(RoleType.USER);
        entityManager.persist(user);
        entityManager.flush();
    }

    @Test
    void findByUser_returnsRecovery_whenExists() {
        Recovery recovery = new Recovery();
        recovery.setUser(user);
        recovery.setRecoveryToken("hashedToken");
        entityManager.persist(recovery);
        entityManager.flush();

        Optional<Recovery> result = recoveryRepository.findByUser(user);

        assertThat(result).isPresent();
        assertThat(result.get().getRecoveryToken()).isEqualTo("hashedToken");
        assertThat(result.get().getUser().getEmail()).isEqualTo("jane@example.com");
    }

    @Test
    void findByUser_returnsEmpty_whenNoRecoveryExists() {
        Optional<Recovery> result = recoveryRepository.findByUser(user);

        assertThat(result).isEmpty();
    }

    @Test
    void setEndOfLifeCycle_isAppliedAutomatically_onPersist() {
        Recovery recovery = new Recovery();
        recovery.setUser(user);
        recovery.setRecoveryToken("anotherToken");
        entityManager.persist(recovery);
        entityManager.flush();
        entityManager.refresh(recovery);

        assertThat(recovery.getEndOfLifeCycle()).isNotNull();
        assertThat(recovery.getEndOfLifeCycle()).isAfter(LocalDateTime.now());
    }

    @Test
    void setEndOfLifeCycle_isNotOverwritten_whenAlreadySet() {
        LocalDateTime explicitExpiry = LocalDateTime.now().plusHours(1);

        Recovery recovery = new Recovery();
        recovery.setUser(user);
        recovery.setRecoveryToken("thirdToken");
        recovery.setEndOfLifeCycle(explicitExpiry);
        entityManager.persist(recovery);
        entityManager.flush();
        entityManager.refresh(recovery);

        assertThat(recovery.getEndOfLifeCycle()).isEqualToIgnoringNanos(explicitExpiry);
    }



}
