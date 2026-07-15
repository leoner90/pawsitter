package lv.pawsitter.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lv.pawsitter.dto.SitterAvailabilityRequest;
import lv.pawsitter.dto.SitterProfileUpdateDTO;
import lv.pawsitter.entity.SitterProfile;
import lv.pawsitter.exception.InvalidSitterOperationException;
import lv.pawsitter.service.SitterProfileService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;


//************ GETTERS
@Controller
@RequiredArgsConstructor
@RequestMapping("/sitter/profile") // mapper simplify getter and post lie  @PostMapping("/sitter/profile/publish") == /sitter/profile/sitter/profile/publish
public class SitterProfileController
{
//************* VAR
    private final SitterProfileService sitterProfileService;


//************ GETTERS
    //SITTER PROFILE PAGE GETTER
    @GetMapping
    public String sitterProfilePage(Authentication authentication, Model model)
    {
        SitterProfile sitterProfile = sitterProfileService.getProfileByUserEmail(authentication.getName());

        model.addAttribute("sitter", sitterProfile);
        model.addAttribute("availabilityRequest", new SitterAvailabilityRequest(null, null));
        model.addAttribute("availabilityRanges", sitterProfileService.getAvailability(authentication.getName()));

        return "sitter/sitterProfile";
    }

    //EDIT SITTER PROFILE PAGE GETTER
    @GetMapping("/edit")
    public String editSitterProfile(Authentication authentication, Model model)
    {
        SitterProfile sitterProfile = sitterProfileService.getProfileByUserEmail(authentication.getName());

        SitterProfileUpdateDTO updateDTO =
                new SitterProfileUpdateDTO(
                        sitterProfile.getLocation(),
                        sitterProfile.getUser().getPhoneNumber(),
                        sitterProfile.getPricePerDay(),
                        sitterProfile.getDescription(),
                        null
                );

        model.addAttribute("sitterProfileUpdate", updateDTO);

        return "sitter/sitterProfileEdit";
    }


//************ POST
    // Save Edited Sitter Profile
    @PostMapping("/edit")
    public String updateSitterProfile(Authentication authentication, @Valid @ModelAttribute("sitterProfileUpdate") SitterProfileUpdateDTO dto, BindingResult bindingResult)
    {
        if (bindingResult.hasErrors())
        {
            return "sitter/sitterProfileEdit";
        }
        try
        {
            sitterProfileService.updateProfile(authentication.getName(), dto);
        }
        catch (IllegalArgumentException exception)
        {
            bindingResult.rejectValue("image", "image.invalid", exception.getMessage());
            return "sitter/sitterProfileEdit";
        }

        return "redirect:/sitter/profile";
    }

    // Set Available dates
    @PostMapping("/availability")
    public String addAvailability(
            Authentication authentication,
            @Valid @ModelAttribute("availabilityRequest")
            SitterAvailabilityRequest availabilityRequest,
            BindingResult bindingResult,
            Model model)
    {
        if (bindingResult.hasErrors())
        {
            SitterProfile sitterProfile = sitterProfileService.getProfileByUserEmail(authentication.getName());
            model.addAttribute("sitter", sitterProfile);
            model.addAttribute("availabilityRanges", sitterProfileService.getAvailability(authentication.getName()));
            return "sitter/sitterProfile";
        }

        sitterProfileService.addAvailability(authentication.getName(), availabilityRequest);
        return "redirect:/sitter/profile";
    }


    //remove Availability Date
    @PostMapping("/availability/{id}/delete")
    public String deleteAvailability(@PathVariable Long id, Authentication authentication)
    {
        sitterProfileService.deleteAvailability(authentication.getName(), id);

        return "redirect:/sitter/profile";
    }

    //Try to publish + error checks
    @PostMapping("/publish")
    public String publishProfile(Authentication authentication, Model model)
    {
        try
        {
            sitterProfileService.publishProfile(authentication.getName());
        }
        catch (InvalidSitterOperationException exception)
        {
            SitterProfile sitterProfile = sitterProfileService.getProfileByUserEmail(authentication.getName());
            model.addAttribute("sitter", sitterProfile);
            model.addAttribute("availabilityRequest", new SitterAvailabilityRequest(null, null));
            model.addAttribute("availabilityRanges", sitterProfileService.getAvailability(authentication.getName()));
            model.addAttribute("publishError", exception.getMessage());

            return "sitter/sitterProfile";
        }

        return "redirect:/sitter/profile";
    }

    //unpublish profile
    @PostMapping("/unpublish")
    public String unpublishProfile(Authentication authentication)
    {
        sitterProfileService.unpublishProfile(authentication.getName());
        return "redirect:/sitter/profile";
    }
}