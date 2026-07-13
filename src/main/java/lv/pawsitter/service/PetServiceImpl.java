package lv.pawsitter.service;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import lv.pawsitter.entity.OwnerProfile;
import lv.pawsitter.entity.Pet;
import lv.pawsitter.repository.OwnerProfileRepository;
import lv.pawsitter.repository.PetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class PetServiceImpl implements PetService{

    @Autowired
    PetRepository petRepository;
    @Autowired
    OwnerProfileRepository ownerProfileRepository;

    @Override
    public List<Pet> getAllPets() {
        log.info("Fetching all existing Pets");
        return petRepository.findAll();
    }

    @Override
    public Pet getById(Long id) {
        log.info("Fetching pet with id: {}", id);
        return petRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pet not found with the idL "+id));
    }

    @Override
    @Transactional
    public Pet createPet(Long ownerId, Pet pet) {
        OwnerProfile ownerProfile = ownerProfileRepository.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("Owner not found with the id: " + ownerId));

        pet.setOwnerProfile(ownerProfile);
        log.info("{} created a pet {}", ownerId, pet);
        return petRepository.save(pet);
    }

    @Override
    public Pet updatePet(Long id, Pet updatedPet) {
        Pet existingPet= getById(id);

        log.info("Updating pet with id: {}", id);

        existingPet.setFirstName(updatedPet.getFirstName());
        existingPet.setLastName(updatedPet.getLastName());
        existingPet.setNickName(updatedPet.getNickName());
        existingPet.setAnimalType(updatedPet.getAnimalType());
        existingPet.setBreed(updatedPet.getBreed());
        existingPet.setAge(updatedPet.getAge());
        existingPet.setDescription(updatedPet.getDescription());
        existingPet.setSpecialNeeds(updatedPet.getSpecialNeeds());
        existingPet.setImageUrl(updatedPet.getImageUrl());

        log.info("Updated pet with id {}",id);

        return petRepository.save(existingPet);
    }

    @Override
    @Transactional
    public void deletePet(Long id) {
        Pet pet = getById(id);
        petRepository.delete(pet);
        log.info("Deleted pet with id: {}", id);
    }

    @Override
    public List<Pet> getPetsByOwnerId(Long ownerProfileId)
    {
        log.info("Fetching pets for ownerProfileId {}", ownerProfileId);
        return petRepository.findByOwnerProfileId(ownerProfileId);
    }
}
