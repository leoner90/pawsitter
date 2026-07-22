package lv.pawsitter.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lv.pawsitter.dto.OwnerProfileUpdateDTO;
import lv.pawsitter.dto.PetResponseDto;
import lv.pawsitter.entity.OwnerProfile;
import lv.pawsitter.service.OwnerProfileService;
import lv.pawsitter.service.PetService;
import lv.pawsitter.service.ReviewService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.util.List;


@Controller
@RequiredArgsConstructor
public class OwnerProfileController
{
//******** VAR
    private final OwnerProfileService ownerProfileService;
    private final PetService petService;
    private final ReviewService reviewService;


//******** GETTERS
    //OWNER PROFILE
    @GetMapping("/owner/profile")
    public String ownerProfilePage(Authentication authentication, Model model)
    {
        OwnerProfile ownerProfile = ownerProfileService.getProfileByUserEmail(authentication.getName());
        //find only active pets to show for user
        List<PetResponseDto> activePets = petService.getActivePetsByOwnerId(ownerProfile.getId());
        model.addAttribute("owner", ownerProfile);
        model.addAttribute("pets", activePets);
        model.addAttribute("reviewSummary", reviewService.getReviewSummaryForUser(ownerProfile.getUser().getId()));

        return "owner/ownerProfile";
    }

    //EDIT PROFILE
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
    //EDIT OWNER PROFILE
    @PostMapping("/owner/profile/edit")
    public String updateOwnerProfile(
            Authentication authentication,
            @Valid @ModelAttribute("profileRequest") OwnerProfileUpdateDTO profileRequest,
            BindingResult bindingResult
    )
    {
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
}