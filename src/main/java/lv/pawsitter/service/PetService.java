package lv.pawsitter.service;

import lv.pawsitter.dto.PetRequestDto;
import lv.pawsitter.dto.PetResponseDto;

import java.util.List;

public interface PetService {
    List<PetResponseDto> getAllPets();
    PetResponseDto getById(Long id);
    PetResponseDto createPet(Long ownerId, PetRequestDto pet);
    PetResponseDto updatePet(Long id, PetRequestDto updatedPet);
    void deletePet(Long id);
    List<PetResponseDto> getPetsByOwnerId(Long ownerProfileId);
}