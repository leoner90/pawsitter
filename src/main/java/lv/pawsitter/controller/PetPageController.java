package lv.pawsitter.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lv.pawsitter.dto.PetRequestDto;
import lv.pawsitter.dto.PetResponseDto;
import lv.pawsitter.entity.OwnerProfile;
import lv.pawsitter.service.OwnerProfileService;
import lv.pawsitter.service.PetService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class PetPageController
{

//******** VAR
    private final OwnerProfileService ownerProfileService;
    private final PetService petService;


//******** GET
    @GetMapping("/owner/pets/add")
    public String addPet(Model model)
    {
        model.addAttribute("petRequest", new PetRequestDto());
        return "owner/addPet";
    }

    //EDIT PET GETTER
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


//******** POST

    //ADD PET
    @PostMapping("/owner/pets/add")
    public String addPet(
            Authentication authentication,
            @Valid @ModelAttribute("petRequest") PetRequestDto petRequest,
            BindingResult bindingResult
    )
    {
        if(bindingResult.hasErrors())
        {
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

    //DELETE PET
    @PostMapping("/owner/pets/{id}/delete")
    public String deletePet(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes)
    {
        try
        {
            petService.deleteOwnerPet(authentication.getName(), id);
            redirectAttributes.addFlashAttribute("petSuccess", "Pet deleted successfully");
        }
        catch (IllegalStateException exception)
        {
            redirectAttributes.addFlashAttribute("petError", exception.getMessage());
        }

        return "redirect:/owner/profile";
    }


    //EDIT PET
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
