package lv.pawsitter.service;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import lv.pawsitter.dto.PetRequestDto;
import lv.pawsitter.dto.PetResponseDto;
import lv.pawsitter.entity.OwnerProfile;
import lv.pawsitter.entity.Pet;
import lv.pawsitter.exception.PetNotFoundException;
import lv.pawsitter.exception.UserNotFoundException;
import lv.pawsitter.repository.OwnerProfileRepository;
import lv.pawsitter.repository.PetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PetServiceImpl implements PetService{

    @Autowired
    PetRepository petRepository;
    @Autowired
    OwnerProfileRepository ownerProfileRepository;
    @Autowired
    ImageStorageService imageStorageService;

    @Override
    public List<PetResponseDto> getAllPets() {
        log.info("Fetching all existing Pets");
        return petRepository.findAll().stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public PetResponseDto getById(Long id) {
        Objects.requireNonNull(id, "Pet id must not be null");
        log.info("Fetching pet with id: {}", id);
        return toResponseDto(findPetOrThrow(id));
    }

    @Override
    @Transactional
    public PetResponseDto createPet(Long ownerId, PetRequestDto dto) {
        Objects.requireNonNull(dto, "PetRequestDto must not be null");
        Objects.requireNonNull(ownerId, "Owner id must not be null");
        validatePetFields(dto);

        OwnerProfile ownerProfile = ownerProfileRepository.findById(ownerId)
                .orElseThrow(() -> new UserNotFoundException("Owner not found with the id: " + ownerId));

        Pet pet = new Pet();
        applyDtoToEntity(dto, pet);
        pet.setOwnerProfile(ownerProfile);

        if (dto.getImage() != null && !dto.getImage().isEmpty())
        {
            String imageUrl = imageStorageService.savePetImage(dto.getImage());
            pet.setImageUrl(imageUrl);
        }

        Pet savedPet = petRepository.save(pet);
        log.info("{} created a pet {}", ownerId, savedPet);
        return toResponseDto(savedPet);
    }

    @Override
    public PetResponseDto updatePet(Long id, PetRequestDto dto) {
        Objects.requireNonNull(id, "Pet id must not be null");
        Objects.requireNonNull(dto, "PetRequestDto must not be null");
        validatePetFields(dto);

        Pet existingPet = findPetOrThrow(id);

        log.info("Updating pet with id: {}", id);
        applyDtoToEntity(dto, existingPet);

        Pet savedPet = petRepository.save(existingPet);
        log.info("Updated pet with id {}", id);

        return toResponseDto(savedPet);
    }

    //not sure we need this isn't it unsafe?
    @Override
    @Transactional
    public void deletePet(Long id) {
        Objects.requireNonNull(id, "Pet id must not be null");
        Pet pet = findPetOrThrow(id);
        petRepository.delete(pet);
        log.info("Deleted pet with id: {}", id);
    }

    @Override
    @Transactional
    public void deleteOwnerPet(String email, Long petId)
    {
        OwnerProfile ownerProfile = ownerProfileRepository.findByUserEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Owner profile not found"));

        Pet pet = petRepository.findByIdAndOwnerProfileId(petId, ownerProfile.getId())
                .orElseThrow(() -> new PetNotFoundException("Pet not found"));

        imageStorageService.deletePetImage(pet.getImageUrl());
        petRepository.delete(pet);

        log.info("Deleted pet with id: {}, for user {}", petId, email);
    }

    @Override
    public List<PetResponseDto> getPetsByOwnerId(Long ownerProfileId) {
        Objects.requireNonNull(ownerProfileId, "ownerProfileId must not be null");
        log.info("Fetching pets for ownerProfileId {}", ownerProfileId);
        return petRepository.findByOwnerProfileId(ownerProfileId).stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    private Pet findPetOrThrow(Long id) {
        Objects.requireNonNull(id, "Pet id must not be null");
        return petRepository.findById(id)
                .orElseThrow(() -> new PetNotFoundException("Pet not found with the id: " + id));
    }

    // Helper methods

    private void validatePetFields(PetRequestDto dto) {
        if (dto.getFirstName() == null || dto.getFirstName().isBlank()) {
            throw new IllegalArgumentException("Pet first name must not be blank");
        }
        if (dto.getAge() < 0) {
            throw new IllegalArgumentException("Pet age must be positive");
        }
    }

    private PetResponseDto toResponseDto(Pet pet) {
        PetResponseDto dto = new PetResponseDto();
        dto.setId(pet.getId());
        dto.setOwnerId(pet.getOwnerProfile().getId());
        dto.setFirstName(pet.getFirstName());
        dto.setLastName(pet.getLastName());
        dto.setNickName(pet.getNickName());
        dto.setAnimalType(pet.getAnimalType());
        dto.setBreed(pet.getBreed());
        dto.setAge(pet.getAge());
        dto.setDescription(pet.getDescription());
        dto.setSpecialNeeds(pet.getSpecialNeeds());
        dto.setImageUrl(pet.getImageUrl());
        dto.setCreatedAt(pet.getCreatedAt());
        return dto;
    }

    private void applyDtoToEntity(PetRequestDto dto, Pet pet) {
        pet.setFirstName(dto.getFirstName());
        pet.setLastName(dto.getLastName());
        pet.setNickName(dto.getNickName());
        pet.setAnimalType(dto.getAnimalType());
        pet.setBreed(dto.getBreed());
        pet.setAge(dto.getAge());
        pet.setDescription(dto.getDescription());
        pet.setSpecialNeeds(dto.getSpecialNeeds());
    }

    //pet Edit
    @Override
    public PetResponseDto getOwnerPet(String email, Long petId)
    {
        OwnerProfile ownerProfile = ownerProfileRepository.findByUserEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Owner profile not found"));

        Pet pet = petRepository.findByIdAndOwnerProfileId(petId, ownerProfile.getId())
                .orElseThrow(() -> new PetNotFoundException("Pet not found"));

        return toResponseDto(pet);
    }

    @Override
    @Transactional
    public PetResponseDto updateOwnerPet(String email, Long petId, PetRequestDto dto)
    {
        OwnerProfile ownerProfile = ownerProfileRepository.findByUserEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Owner profile not found"));

        Pet pet = petRepository.findByIdAndOwnerProfileId(petId, ownerProfile.getId())
                .orElseThrow(() -> new PetNotFoundException("Pet not found"));

        applyDtoToEntity(dto, pet);

        if (dto.getImage() != null && !dto.getImage().isEmpty())
        {
            String oldImageUrl = pet.getImageUrl();
            String newImageUrl = imageStorageService.savePetImage(dto.getImage());
            imageStorageService.deletePetImage(oldImageUrl);
            pet.setImageUrl(newImageUrl);
        }

        return toResponseDto(petRepository.save(pet));
    }
}