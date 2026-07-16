package lv.pawsitter.repository;

import jakarta.persistence.EntityManager;
import lv.pawsitter.entity.OwnerProfile;
import lv.pawsitter.entity.User;
import lv.pawsitter.model.RoleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class OwnerProfileRepositoryUnitTests {

    @Autowired
    private OwnerProfileRepository ownerProfileRepository;

    @Autowired
    private EntityManager entityManager;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setEmail("jane@example.com");
        user.setPassword("encodedPassword");
        user.setPhoneNumber("+37120000001");
        user.setRole(RoleType.USER);
        entityManager.persist(user);

        OwnerProfile ownerProfile = new OwnerProfile();
        ownerProfile.setUser(user);
        ownerProfile.setLocation("Riga");
        ownerProfile.setDescription("Loves animals");
        entityManager.persist(ownerProfile);

        entityManager.flush();
    }

    @Test
    void findByUserEmail_returnsProfile_whenEmailExists() {
        Optional<OwnerProfile> result = ownerProfileRepository.findByUserEmail("jane@example.com");

        assertThat(result).isPresent();
        assertThat(result.get().getLocation()).isEqualTo("Riga");
        assertThat(result.get().getUser().getEmail()).isEqualTo("jane@example.com");
    }

    @Test
    void findByUserEmail_returnsEmpty_whenEmailDoesNotExist() {
        Optional<OwnerProfile> result = ownerProfileRepository.findByUserEmail("missing@example.com");

        assertThat(result).isEmpty();
    }

    @Test
    void findByUserEmail_returnsEmpty_whenEmailBelongsToUserWithoutOwnerProfile() {
        User sitterUser = new User();
        sitterUser.setFirstName("John");
        sitterUser.setLastName("Smith");
        sitterUser.setEmail("john@example.com");
        sitterUser.setPassword("encodedPassword");
        sitterUser.setPhoneNumber("+37120000002");
        sitterUser.setRole(RoleType.SITTER);
        entityManager.persist(sitterUser);
        entityManager.flush();

        Optional<OwnerProfile> result = ownerProfileRepository.findByUserEmail("john@example.com");

        assertThat(result).isEmpty();
    }

    @Test
    void defaultValues_areAppliedOnNewProfile() {
        User newUser = new User();
        newUser.setFirstName("Carl");
        newUser.setLastName("Owner");
        newUser.setEmail("carl@example.com");
        newUser.setPassword("encodedPassword");
        newUser.setPhoneNumber("+37120000003");
        newUser.setRole(RoleType.USER);
        entityManager.persist(newUser);

        OwnerProfile freshProfile = new OwnerProfile();
        freshProfile.setUser(newUser);
        entityManager.persist(freshProfile);
        entityManager.flush();
        entityManager.refresh(freshProfile);

        assertThat(freshProfile.getLocation()).isEqualTo("Not provided");
        assertThat(freshProfile.getDescription()).isEqualTo("");
    }

}
