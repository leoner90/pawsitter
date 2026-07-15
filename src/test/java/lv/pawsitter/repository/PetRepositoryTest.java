package lv.pawsitter.repository;

import jakarta.persistence.EntityManager;
import lv.pawsitter.entity.AnimalTypes;
import lv.pawsitter.entity.OwnerProfile;
import lv.pawsitter.entity.Pet;
import lv.pawsitter.entity.User;
import lv.pawsitter.model.RoleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PetRepositoryTest {

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private EntityManager entityManager;

    private OwnerProfile ownerA;
    private OwnerProfile ownerB;

    @BeforeEach
    void setUp() {
        User userA = new User();
        userA.setFirstName("Alice");
        userA.setLastName("Owner");
        userA.setEmail("alice@example.com");
        userA.setPassword("password");
        userA.setPhoneNumber("+37120000001");
        userA.setRole(RoleType.USER);
        entityManager.persist(userA);

        User userB = new User();
        userB.setFirstName("Bob");
        userB.setLastName("Owner");
        userB.setEmail("bob@example.com");
        userB.setPassword("password");
        userB.setPhoneNumber("+37120000002");
        userB.setRole(RoleType.USER);
        entityManager.persist(userB);

        ownerA = new OwnerProfile();
        ownerA.setUser(userA);
        entityManager.persist(ownerA);

        ownerB = new OwnerProfile();
        ownerB.setUser(userB);
        entityManager.persist(ownerB);

        entityManager.persist(buildPet(ownerA, "Buddy"));
        entityManager.persist(buildPet(ownerA, "Milo"));
        entityManager.persist(buildPet(ownerB, "Luna"));

        entityManager.flush();
    }

    private Pet buildPet(OwnerProfile owner, String firstName) {
        Pet pet = new Pet();
        pet.setOwnerProfile(owner);
        pet.setFirstName(firstName);
        pet.setLastName("Test");
        pet.setNickName(firstName);
        pet.setAnimalType(AnimalTypes.DOG);
        pet.setBreed("Mixed");
        pet.setAge(2);
        pet.setDescription("Test pet");
        pet.setSpecialNeeds("None");
        pet.setImageUrl("http://example.com/pet.jpg");
        pet.setCreatedAt(LocalDateTime.now());
        return pet;
    }

    @Test
    void findByOwnerProfileId_returnsOnlyThatOwnersPets() {
        List<Pet> result = petRepository.findByOwnerProfileId(ownerA.getId());

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Pet::getFirstName)
                .containsExactlyInAnyOrder("Buddy", "Milo");
    }

    @Test
    void findByOwnerProfileId_returnsEmptyList_whenOwnerHasNoPets() {
        User user = new User();
        user.setFirstName("Carl");
        user.setLastName("Owner");
        user.setEmail("carl@gmail.com");
        user.setPassword("password");
        user.setPhoneNumber("+37120000003");
        user.setRole(RoleType.USER);
        entityManager.persist(user);

        OwnerProfile ownerWithNoPets = new OwnerProfile();
        ownerWithNoPets.setUser(user);
        entityManager.persist(ownerWithNoPets);
        entityManager.flush();

        List<Pet> result = petRepository.findByOwnerProfileId(ownerWithNoPets.getId());
        assertThat(result).isEmpty();
    }

    @Test
    void findByIdAndOwnerProfileId_returnPet_whenOwnerMatches(){
        Pet petBelongingToOwnerA = petRepository.findByOwnerProfileId(ownerA.getId()).get(0);
        Optional<Pet> result = petRepository.findByIdAndOwnerProfileId(petBelongingToOwnerA.getId(), ownerB.getId());
        assertThat(result).isEmpty();
    }

    @Test
    void findByIdAndOwnerProfileId_returnsEmpty_forNoPetOwner(){
        Optional<Pet> result = petRepository.findByIdAndOwnerProfileId(999L, ownerA.getId());
        assertThat(result).isEmpty();
    }
}
