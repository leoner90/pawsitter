package lv.pawsitter.service;

import lv.pawsitter.entity.Pet;

import java.util.List;

public interface PetService {
    public List<Pet> getAllPets();
    public Pet getById(Long id);
    public Pet createPet(Long ownerId, Pet pet);
    public Pet updatePet(Long id, Pet updatedPet);
    public void deletePet(Long id);
}
