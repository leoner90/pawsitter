package lv.pawsitter.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lv.pawsitter.dto.PetRequestDto;
import lv.pawsitter.dto.PetResponseDto;

import lv.pawsitter.service.PetService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/pet")
@RequiredArgsConstructor
public class PetController {
    private final PetService petService;

    @GetMapping
    public ResponseEntity<List<PetResponseDto>> getAllPets(){
        log.info("Fetching all pets");
        return ResponseEntity.ok(petService.getAllPets());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PetResponseDto> getPetById(@PathVariable Long id) {
        log.info("Fetching pet with id {}", id);
        PetResponseDto pet = petService.getById(id);
        log.info("Found pet with id {}", id);
        return ResponseEntity.ok(pet);
    }

    @GetMapping("/owner/{ownerProfileId}")
    public ResponseEntity<List<PetResponseDto>> getPetsByOwner(@PathVariable Long ownerProfileId) {
        log.info("Fetching pets for ownerProfileId {}", ownerProfileId);
        List<PetResponseDto> pets = petService.getPetsByOwnerId(ownerProfileId);
        log.info("Retrieved {} pets for ownerProfileId {}", pets.size(), ownerProfileId);
        return ResponseEntity.ok(pets);
    }

    @PostMapping("/owner/{ownerProfileId}")
    public ResponseEntity<PetResponseDto> createPet(@PathVariable Long ownerProfileId, @Valid @RequestBody PetRequestDto dto)
    {
        log.info("Creating pet for ownerProfileId {}", ownerProfileId);
        PetResponseDto createdPet = petService.createPet(ownerProfileId, dto);
        log.info("Created pet with id {} for ownerProfileId {}", createdPet.getId(), ownerProfileId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPet);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PetResponseDto> updatePet(@PathVariable Long id, @RequestBody PetRequestDto dto)
    {
        log.info("Updating pet with id {}", id);
        PetResponseDto updatedPet = petService.updatePet(id, dto);
        log.info("Updated pet with id {}", id);
        return ResponseEntity.ok(updatedPet);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePet(@PathVariable Long id)
    {
        log.info("Deleting pet with id {}", id);
        petService.deletePet(id);
        log.info("Deleted pet with id {}", id);
        return ResponseEntity.noContent().build();
    }

}
