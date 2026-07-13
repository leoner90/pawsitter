package lv.pawsitter.repository;

import lv.pawsitter.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PetRepository extends JpaRepository<Pet, Long>
{
    List<Pet> findByOwnerProfileId(Long ownerProfileId);
}