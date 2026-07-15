package lv.pawsitter.service;

import jakarta.annotation.Nonnull;
import lv.pawsitter.dto.PetRequestDto;
import lv.pawsitter.dto.PetResponseDto;
import lv.pawsitter.entity.AnimalTypes;
import lv.pawsitter.entity.OwnerProfile;
import lv.pawsitter.entity.Pet;
import lv.pawsitter.exception.GlobalExceptionHandler;
import lv.pawsitter.exception.PetNotFoundException;
import lv.pawsitter.exception.UserNotFoundException;
import lv.pawsitter.repository.OwnerProfileRepository;
import lv.pawsitter.repository.PetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PetServiceUnitTests {

    @Mock
    private PetRepository petRepository;

    @Mock
    private OwnerProfileRepository ownerProfileRepository;

    @InjectMocks
    private PetServiceImpl petService;

    private OwnerProfile ownerProfile;
    private Pet pet;
    private PetRequestDto petRequestDto;

    @BeforeEach
    void setUp(){
        ownerProfile = new OwnerProfile();
        ownerProfile.setId(1L);

        pet = new Pet();
        pet.setId(100L);
        pet.setOwnerProfile(ownerProfile);
        pet.setFirstName("Moe");
        pet.setLastName("Johnson");
        pet.setNickName("Mo");
        pet.setAnimalType(AnimalTypes.CAT);
        pet.setBreed("Black");
        pet.setAge(4);
        pet.setDescription("Friendly cat who sleeps a lot");
        pet.setSpecialNeeds("");
        pet.setImageUrl("example.com/image/link.jpg");
        pet.setCreatedAt(LocalDateTime.now());

        petRequestDto = new PetRequestDto();
        petRequestDto.setFirstName("Moe");
        petRequestDto.setLastName("Johnson");
        petRequestDto.setNickName("Mo");
        petRequestDto.setAnimalType(AnimalTypes.CAT);
        petRequestDto.setBreed("Black");
        petRequestDto.setAge(4);
        petRequestDto.setDescription("Friendly cat who sleeps a lot");
        petRequestDto.setSpecialNeeds("");
        petRequestDto.setImageUrl("example.com/image/link.jpg");
    }

    @Test
    void getAllPets_returnsMappedDtos() {
        when(petRepository.findAll()).thenReturn(List.of(pet));
        List<PetResponseDto> result = petService.getAllPets();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(pet.getId());
        assertThat(result.get(0).getFirstName()).isEqualTo("Moe");
        verify(petRepository).findAll();
    }

    @Test
    void getAllPets_returnsEptyList_whenNoPetsExist(){
        when(petRepository.findAll()).thenReturn(List.of());
        List<PetResponseDto> result = petService.getAllPets();
        assertThat(result).isEmpty();
    }

    @Test
    void getById_returnsDtoForExistingPet() {
        when(petRepository.findById(100L)).thenReturn(Optional.of(pet));

        PetResponseDto result = petService.getById(100L);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getOwnerId()).isEqualTo(1L);
    }


    @Test
    void getById_throwsPetNotFoundException_whenPetDoesNotExist() {
        when(petRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> petService.getById(999L)).isInstanceOf(PetNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void getById_throwsException_whenNegativeValueIsPassed() {
        when(petRepository.findById(-100L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> petService.getById(-100L)).isInstanceOf(PetNotFoundException.class)
                .hasMessageContaining("-100");
    }

    @Test
    void getById_throwsException_whenIdIsNull() {
        assertThatThrownBy(() -> petService.getById(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void createPet_savesAndReturnsDto_ForExistingOwner(){
        when(ownerProfileRepository.findById(1L)).thenReturn(Optional.of(ownerProfile));
        when(petRepository.save(any(Pet.class))).thenReturn(pet);

        PetResponseDto result = petService.createPet(1L, petRequestDto);

        assertThat(result.getFirstName()).isEqualTo("Moe");
        assertThat(result.getOwnerId()).isEqualTo(1L);
        verify(ownerProfileRepository).findById(1L);
        verify(petRepository).save(any(Pet.class));
    }

    @Test
    void createPet_throwsUserNotFoundException_ForNonExistingOwner(){
        when(ownerProfileRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(()->petService.createPet(1L, petRequestDto))
                .isInstanceOf(UserNotFoundException.class).hasMessageContaining("1");
        verify(petRepository, never()).save(any());
    }

    @Test
    void createPet_throwsNullPointerException_whenDtoIsNull(){
        assertThatThrownBy(()->petService.createPet(1L, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void createPet_throwsException_whenOwnerIdIsNull() {
        assertThatThrownBy(() -> petService.createPet(null, petRequestDto)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void createPet_throwsIllegalArgumentException_whenFirstNameIsBlank() {
        petRequestDto.setFirstName("");

        assertThatThrownBy(() -> petService.createPet(1L, petRequestDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("first name");

        verify(ownerProfileRepository, never()).findById(any());
        verify(petRepository, never()).save(any());
    }

    @Test
    void createPet_throwsIllegalArgumentException_whenFirstNameIsNull() {
        petRequestDto.setFirstName(null);

        assertThatThrownBy(() -> petService.createPet(1L, petRequestDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("first name");
    }

    @Test
    void createPet_throwsIllegalArgumentException_whenAgeIsNegative() {
        petRequestDto.setAge(-5);

        assertThatThrownBy(() -> petService.createPet(1L, petRequestDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("age");

        verify(petRepository, never()).save(any());
    }

    @Test
    void createPet_succeeds_whenAgeIsZero() {
        petRequestDto.setAge(0);

        when(ownerProfileRepository.findById(1L)).thenReturn(Optional.of(ownerProfile));
        when(petRepository.save(any(Pet.class))).thenAnswer(invocation -> {
            Pet savedPet = invocation.getArgument(0);
            savedPet.setId(100L);
            return savedPet;
        });

        PetResponseDto result = petService.createPet(1L, petRequestDto);

        assertThat(result.getAge()).isEqualTo(0);
        verify(petRepository).save(any(Pet.class));
    }

    @Nonnull
    private static PetRequestDto getPetRequestDto() {
        PetRequestDto updateDto = new PetRequestDto();
        updateDto.setFirstName("Moe");
        updateDto.setLastName("Johnson");
        updateDto.setNickName("Mo");
        updateDto.setAnimalType(AnimalTypes.CAT);
        updateDto.setBreed("Black");
        // Updated age
        updateDto.setAge(5);
        updateDto.setDescription("Friendly cat who sleeps a lot");
        // Added new special need
        updateDto.setSpecialNeeds("Diabetic");
        updateDto.setImageUrl("example.com/image/link.jpg");
        return updateDto;
    }

    @Test
    void updatePet_updatesAndReturnsDro_ForExistingPet(){
        PetRequestDto updateDto = getPetRequestDto();

        when(petRepository.findById(100L)).thenReturn(Optional.of(pet));
        when(petRepository.save(any(Pet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PetResponseDto result = petService.updatePet(100L, updateDto);

        assertThat(result.getFirstName()).isEqualTo("Moe");
        assertThat(result.getBreed()).isEqualTo("Black");
        assertThat(result.getAge()).isEqualTo(5);
        assertThat(result.getSpecialNeeds()).isEqualTo("Diabetic");
        verify(petRepository).save(pet);

    }

    @Test
    void updatePet_throwsNullPointerException_whenDtoIsNull(){
        assertThatThrownBy(()->petService.updatePet(100L, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void updatePet_throwsIllegalArgumentException_whenFirstNameIsBlank() {
        PetRequestDto updateDto = getPetRequestDto();
        updateDto.setFirstName(" ");

        assertThatThrownBy(() -> petService.updatePet(100L, updateDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("first name");

        verify(petRepository, never()).save(any());
    }

    @Test
    void updatePet_throwsIllegalArgumentException_whenAgeIsNegative() {
        PetRequestDto updateDto = getPetRequestDto();
        updateDto.setAge(-1);

        assertThatThrownBy(() -> petService.updatePet(100L, updateDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("age");
    }

    @Test
    void deletePet_deletesPet_ForExistingPets(){
        when(petRepository.findById(100L)).thenReturn(Optional.of(pet));
        petService.deletePet(100L);
        verify(petRepository).delete(pet);
    }

    @Test
    void deletePet_throwsPetNotFound_ForNonExistingPets(){
        when(petRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(()->petService.deletePet(999L)).isInstanceOf(PetNotFoundException.class);
        verify(petRepository, never()).delete(any());
    }

    @Test
    void deletePet_throwsException_whenIdIsNull() {
        assertThatThrownBy(() -> petService.deletePet(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void getPetsByOwnerId_returnsMappedDtos_forExistingPet(){
        when(petRepository.findByOwnerProfileId(1L)).thenReturn(List.of(pet));
        List<PetResponseDto> result = petService.getPetsByOwnerId(1L);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOwnerId()).isEqualTo(1L);
    }

    @Test
    void getPetsOwnerById_returnsEmptyList_forNonExistingPetsForOwner(){
        when(petRepository.findByOwnerProfileId(2L)).thenReturn(List.of());
        List<PetResponseDto> result = petService.getPetsByOwnerId(2L);
        assertThat(result).isEmpty();
    }

    @Test
    void getPetsByOwnerId_throwsNullPointerException_whenOwnerIdIsNull() {
        assertThatThrownBy(() -> petService.getPetsByOwnerId(null))
                .isInstanceOf(NullPointerException.class);
    }

}
