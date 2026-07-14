package lv.pawsitter.service;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import lv.pawsitter.dto.PetRequestDto;
import lv.pawsitter.dto.PetResponseDto;
import lv.pawsitter.entity.OwnerProfile;
import lv.pawsitter.entity.Pet;
import lv.pawsitter.repository.OwnerProfileRepository;
import lv.pawsitter.repository.PetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PetServiceImpl implements PetService{

    @Autowired
    PetRepository petRepository;
    @Autowired
    OwnerProfileRepository ownerProfileRepository;

    @Override
    public List<PetResponseDto> getAllPets() {
        log.info("Fetching all existing Pets");
        return petRepository.findAll().stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public PetResponseDto getById(Long id) {
        log.info("Fetching pet with id: {}", id);
        return toResponseDto(findPetOrThrow(id));
    }

    @Override
    @Transactional
    public PetResponseDto createPet(Long ownerId, PetRequestDto dto) {
        OwnerProfile ownerProfile = ownerProfileRepository.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("Owner not found with the id: " + ownerId));

        Pet pet = new Pet();
        applyDtoToEntity(dto, pet);
        pet.setOwnerProfile(ownerProfile);

        Pet savedPet = petRepository.save(pet);
        log.info("{} created a pet {}", ownerId, savedPet);
        return toResponseDto(savedPet);
    }

    @Override
    public PetResponseDto updatePet(Long id, PetRequestDto dto) {
        Pet existingPet = findPetOrThrow(id);

        log.info("Updating pet with id: {}", id);
        applyDtoToEntity(dto, existingPet);

        Pet savedPet = petRepository.save(existingPet);
        log.info("Updated pet with id {}", id);

        return toResponseDto(savedPet);
    }

    @Override
    @Transactional
    public void deletePet(Long id) {
        Pet pet = findPetOrThrow(id);
        petRepository.delete(pet);
        log.info("Deleted pet with id: {}", id);
    }

    @Override
    public List<PetResponseDto> getPetsByOwnerId(Long ownerProfileId) {
        log.info("Fetching pets for ownerProfileId {}", ownerProfileId);
        return petRepository.findByOwnerProfileId(ownerProfileId).stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    private Pet findPetOrThrow(Long id) {
        return petRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pet not found with the id: " + id));
    }

    // Helper methods

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
        pet.setImageUrl(dto.getImageUrl());
    }
}
