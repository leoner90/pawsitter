package lv.pawsitter.controller;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lv.pawsitter.entity.Pet;
import lv.pawsitter.service.PetService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/pet")
@RequiredArgsConstructor
public class PetController {
    private final PetService petService;

    @GetMapping
    public ResponseEntity<List<Pet>> getAllPets(){
        log.info("Fetching all pets");
        List<Pet> pets = petService.getAllPets();
        log.info("Retrieved {} pets", pets.size());
        return ResponseEntity.ok(pets);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pet> getPetById(@PathVariable Long id) {
        log.info("Fetching pet with id {}", id);
        try {
            Pet pet = petService.getById(id);
            return ResponseEntity.ok(pet);
        } catch (RuntimeException e) {
            log.warn("Pet not found with id {}", id);
            throw e;
        }
    }

    @GetMapping("/owner/{ownerProfileId}")
    public ResponseEntity<List<Pet>> getPetsByOwner(@PathVariable Long ownerProfileId)
    {
        log.info("Fetching pets for ownerProfileId {}", ownerProfileId);
        return ResponseEntity.ok(petService.getPetsByOwnerId(ownerProfileId));
    }

    @PostMapping("/owner/{ownerProfileId}")
    public ResponseEntity<Pet> createPet(@PathVariable Long ownerProfileId, @RequestBody Pet pet)
    {
        log.info("Creating pet for ownerProfileId {}", ownerProfileId);
        try {
            Pet createdPet = petService.createPet(ownerProfileId, pet);
            log.info("Created pet with id {} for ownerProfileId {}", createdPet.getId(), ownerProfileId);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdPet);
        } catch (RuntimeException e) {
            log.error("Failed to create pet for ownerProfileId {}: {}", ownerProfileId, e.getMessage());
            throw e;
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Pet> updatePet(@PathVariable Long id, @RequestBody Pet pet)
    {
        log.info("Updating pet with id {}", id);
        try {
            Pet updatedPet = petService.updatePet(id, pet);
            log.info("Updated pet with id {}", id);
            return ResponseEntity.ok(updatedPet);
        } catch (RuntimeException e) {
            log.warn("Failed to update pet with id {}: {}", id, e.getMessage());
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePet(@PathVariable Long id)
    {
        log.info("Deleting pet with id {}", id);
        try {
            petService.deletePet(id);
            log.info("Deleted pet with id {}", id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.warn("Failed to delete pet with id {}: {}", id, e.getMessage());
            throw e;
        }
    }
}
