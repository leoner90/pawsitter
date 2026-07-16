package lv.pawsitter.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lv.pawsitter.dto.OwnerProfileUpdateDTO;
import lv.pawsitter.dto.PetRequestDto;
import lv.pawsitter.dto.PetResponseDto;
import lv.pawsitter.entity.OwnerProfile;
import lv.pawsitter.service.OwnerProfileService;
import lv.pawsitter.service.PetService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class OwnerProfileController
{
//******** VAR
    private final OwnerProfileService ownerProfileService;
    private final PetService petService;


//******** GETTERS
    @GetMapping("/owner/profile")
    public String ownerProfilePage(Authentication authentication, Model model)
    {
        OwnerProfile ownerProfile = ownerProfileService.getProfileByUserEmail(authentication.getName());
        model.addAttribute("owner", ownerProfile);
        return "owner/ownerProfile";
    }

    @GetMapping("/owner/pets/add")
    public String addPet(Model model){
        model.addAttribute("petRequest", new PetRequestDto());
        return "owner/addPet";
    }

    @GetMapping("/owner/profile/edit")
    public String editOwnerProfilePage(Authentication authentication, Model model){
        OwnerProfile ownerProfile = ownerProfileService.getProfileByUserEmail(authentication.getName());
        OwnerProfileUpdateDTO profileRequest = new OwnerProfileUpdateDTO(
                ownerProfile.getUser().getFirstName(),
                ownerProfile.getUser().getLastName(),
                ownerProfile.getUser().getPhoneNumber(),
                ownerProfile.getLocation(),
                ownerProfile.getDescription(),
                null
        );
        model.addAttribute("profileRequest", profileRequest);
        return "owner/editOwnerProfile";
    }


    //******** POST
    @PostMapping("/owner/pets/add")
    public String addPet(
            Authentication authentication,
            @Valid @ModelAttribute("petRequest") PetRequestDto petRequest,
            BindingResult bindingResult
    ){
        if(bindingResult.hasErrors()){
            return "owner/addPet";
        }
        OwnerProfile ownerProfile = ownerProfileService.getProfileByUserEmail(authentication.getName());

        try
        {
            petService.createPet(ownerProfile.getId(), petRequest);
        }
        catch (IllegalArgumentException exception)
        {
            bindingResult.rejectValue("image", "image.invalid", exception.getMessage());

            return "owner/addPet";
        }

        return "redirect:/owner/profile";
    }

    @PostMapping("/owner/profile/edit")
    public String updateOwnerProfile(
            Authentication authentication,
            @Valid @ModelAttribute("profileRequest") OwnerProfileUpdateDTO profileRequest,
            BindingResult bindingResult
    ){
        if(bindingResult.hasErrors()){
            return "owner/editOwnerProfile";
        }
        try
        {
            ownerProfileService.updateProfile(authentication.getName(), profileRequest);
        }
        catch (IllegalArgumentException exception)
        {
            bindingResult.rejectValue("image", "image.invalid", exception.getMessage());
            return "owner/editOwnerProfile";
        }

        return "redirect:/owner/profile";
    }

    // TODO: Move to PetController after deciding how to separate REST API and Thymeleaf routes.
    @PostMapping("/owner/pets/{id}/delete")
    public String deletePet(@PathVariable Long id, Authentication authentication)
    {
        log.info("Owner {} deleting pet with id: {}", authentication.getName(), id);
        petService.deleteOwnerPet(authentication.getName(), id);
        return "redirect:/owner/profile";
    }

    @GetMapping("/owner/pets/{id}/edit")
    public String editPetPage(@PathVariable Long id, Authentication authentication, Model model)
    {
        PetResponseDto pet = petService.getOwnerPet(authentication.getName(), id);

        PetRequestDto petRequest = new PetRequestDto();
        petRequest.setFirstName(pet.getFirstName());
        petRequest.setLastName(pet.getLastName());
        petRequest.setNickName(pet.getNickName());
        petRequest.setAnimalType(pet.getAnimalType());
        petRequest.setBreed(pet.getBreed());
        petRequest.setAge(pet.getAge());
        petRequest.setDescription(pet.getDescription());
        petRequest.setSpecialNeeds(pet.getSpecialNeeds());

        // pass obj for Html page
        model.addAttribute("petRequest", petRequest);

        // Page should know the pet ID
        model.addAttribute("petId", id);
        return "owner/editPet";
    }

    @PostMapping("/owner/pets/{id}/edit")
    public String updatePet(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @ModelAttribute("petRequest") PetRequestDto petRequest,
            BindingResult bindingResult,
            Model model)
    {
        if (bindingResult.hasErrors())
        {
            // Page should know the pet ID
            model.addAttribute("petId", id);
            return "owner/editPet";
        }

        try
        {
            petService.updateOwnerPet(authentication.getName(), id, petRequest);
        }
        catch (IllegalArgumentException exception)
        {
            bindingResult.rejectValue("image", "image.invalid", exception.getMessage());

            // Page should know the pet ID
            model.addAttribute("petId", id);
            return "owner/editPet";
        }

        return "redirect:/owner/profile";
    }
}
