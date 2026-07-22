package lv.pawsitter.repository;

import jakarta.persistence.EntityManager;
import lv.pawsitter.entity.SitterProfile;
import lv.pawsitter.entity.User;
import lv.pawsitter.model.RoleType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class SitterProfileRepositoryUnitTests {

    @Autowired
    private SitterProfileRepository sitterProfileRepository;

    @Autowired
    private EntityManager entityManager;

    private User buildUser(String email) {
        User user = new User();
        user.setFirstName("John");
        user.setLastName("Sitter");
        user.setEmail(email);
        user.setPassword("encodedPassword");
        user.setPhoneNumber("+37120000009");
        user.setRole(RoleType.SITTER);
        entityManager.persist(user);
        return user;
    }

    @Test
    void findByUserEmail_returnsProfile_whenEmailExists() {
        User user = buildUser("john@example.com");
        SitterProfile profile = new SitterProfile();
        profile.setUser(user);
        profile.setLocation("Riga");
        entityManager.persist(profile);
        entityManager.flush();

        Optional<SitterProfile> result = sitterProfileRepository.findByUserEmail("john@example.com");

        assertThat(result).isPresent();
        assertThat(result.get().getLocation()).isEqualTo("Riga");
    }

    @Test
    void findByUserEmail_returnsEmpty_whenEmailDoesNotExist() {
        Optional<SitterProfile> result = sitterProfileRepository.findByUserEmail("missing@example.com");

        assertThat(result).isEmpty();
    }

    @Test
    void findByPublishedTrue_returnsOnlyPublishedProfiles() {
        User publishedUser = buildUser("published@example.com");
        SitterProfile published = new SitterProfile();
        published.setUser(publishedUser);
        published.setPublished(true);
        entityManager.persist(published);

        User unpublishedUser = buildUser("unpublished@example.com");
        SitterProfile unpublished = new SitterProfile();
        unpublished.setUser(unpublishedUser);
        unpublished.setPublished(false);
        entityManager.persist(unpublished);

        entityManager.flush();

        List<SitterProfile> result = sitterProfileRepository.findByPublishedTrue();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUser().getEmail()).isEqualTo("published@example.com");
    }

    @Test
    void findByPublishedTrue_returnsEmptyList_whenNoneArePublished() {
        User user = buildUser("draft@example.com");
        SitterProfile draft = new SitterProfile();
        draft.setUser(user);
        entityManager.persist(draft);
        entityManager.flush();

        List<SitterProfile> result = sitterProfileRepository.findByPublishedTrue();

        assertThat(result).isEmpty();
    }

    @Test
    void defaultValues_areAppliedOnNewProfile() {
        User user = buildUser("defaults@example.com");
        SitterProfile profile = new SitterProfile();
        profile.setUser(user);
        entityManager.persist(profile);
        entityManager.flush();
        entityManager.refresh(profile);

        assertThat(profile.getLocation()).isEqualTo("Not provided");
        assertThat(profile.getDescription()).isEqualTo("");
        assertThat(profile.getPricePerDay()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(profile.isPublished()).isFalse();
    }


}
