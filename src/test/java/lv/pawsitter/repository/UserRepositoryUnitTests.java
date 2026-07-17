package lv.pawsitter.repository;
import jakarta.persistence.EntityManager;
import lv.pawsitter.entity.User;
import lv.pawsitter.model.RoleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
public class UserRepositoryUnitTests {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User buildUser(String email) {
        User user = new User();
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setEmail(email);
        user.setPassword("encodedPassword");
        user.setPhoneNumber("+37120000001");
        user.setRole(RoleType.USER);
        return user;
    }

    @BeforeEach
    void setUp() {
        entityManager.persist(buildUser("jane@example.com"));
        entityManager.flush();
    }

    @Test
    void findByEmail_returnsUser_whenEmailExists() {
        Optional<User> result = userRepository.findByEmail("jane@example.com");

        assertThat(result).isPresent();
        assertThat(result.get().getFirstName()).isEqualTo("Jane");
    }

    @Test
    void findByEmail_returnsEmpty_whenEmailDoesNotExist() {
        Optional<User> result = userRepository.findByEmail("missing@example.com");

        assertThat(result).isEmpty();
    }

    @Test
    void findByEmail_isCaseSensitive_atRepositoryLevel() {
        Optional<User> result = userRepository.findByEmail("JANE@example.com");

        assertThat(result).isEmpty();
    }

    @Test
    void save_setsCreatedAtAutomatically() {
        User saved = userRepository.findByEmail("jane@example.com").orElseThrow();

        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void save_throwsDataIntegrityViolationException_whenEmailIsDuplicate() {
        User duplicate = buildUser("jane@example.com");

        assertThatThrownBy(() -> {
            userRepository.save(duplicate);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void save_persistsMultipleUsers_withDifferentEmails() {
        userRepository.save(buildUser("other@example.com"));
        entityManager.flush();

        assertThat(userRepository.count()).isEqualTo(2);
    }

}
