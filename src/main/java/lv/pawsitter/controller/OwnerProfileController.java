package lv.pawsitter.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lv.pawsitter.dto.OwnerProfileUpdateDTO;
import lv.pawsitter.dto.PetRequestDto;
import lv.pawsitter.entity.OwnerProfile;
import lv.pawsitter.service.OwnerProfileService;
import lv.pawsitter.service.PetService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

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
                ownerProfile.getImageUrl()
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
        petService.createPet(ownerProfile.getId(), petRequest);

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

        ownerProfileService.updateProfile(authentication.getName(), profileRequest);
        return "redirect:/owner/profile";
    }
}
