package lv.pawsitter.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lv.pawsitter.dto.SitterAvailabilityRequest;
import lv.pawsitter.dto.SitterProfileUpdateDTO;
import lv.pawsitter.dto.OwnerProfileUpdateDTO;
import lv.pawsitter.dto.PetRequestDto;
import lv.pawsitter.dto.UserCreateDTO;
import lv.pawsitter.exception.EmailNotUniqueException;
import lv.pawsitter.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable; // @PathVariable takes a value directly from the UR
import lv.pawsitter.entity.SitterProfile;
import org.springframework.security.core.Authentication;
import lv.pawsitter.entity.OwnerProfile;
import org.springframework.web.bind.annotation.PostMapping;

//TODO: Split into separete controllers HomeController, LogginController, OwnerProfileController,SitterProfileController etc.
@Controller
@RequiredArgsConstructor
public class PageController
{
    private final SitterProfileService sitterProfileService;
    private final OwnerProfileService ownerProfileService;
    private final UserService userService;
    private final PetService petService;

    @GetMapping("/")
    public String homePage(Model model)
    {
        model.addAttribute("sitters",sitterProfileService.getPublishedSitters());
        return "index";
    }

    @GetMapping("/login")
    public String loginPage()
    {
        return "authentication/login";
    }

    @GetMapping("/owner/profile")
    public String ownerProfilePage(Authentication authentication, Model model)
    {
        OwnerProfile ownerProfile = ownerProfileService.getProfileByUserEmail(authentication.getName());
        model.addAttribute("owner", ownerProfile);
        return "owner/ownerProfile";
    }

    @GetMapping("/sitters/{id}")
    public String sitterDetailsPage(@PathVariable Long id, Model model)
    {
        model.addAttribute("sitter", sitterProfileService.getSitterById(id));
        return "sitter/sitterDetails";
    }

    //Todo move to separate Controller
    @GetMapping("/registration")
    public String registrationPage(Model model) {
        model.addAttribute(
                "registrationRequest",
                new UserCreateDTO("", "", "", "", "", "", "",null)
        );

        return "authentication/registration";
    }

    @PostMapping("/registration")
    public String registerUser(
            @Valid
            @ModelAttribute("registrationRequest")
            UserCreateDTO registrationRequest,
            BindingResult bindingResult)
    {
        if (!registrationRequest.password().equals(registrationRequest.confirmPassword()))
        {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match");
        }

        if (bindingResult.hasErrors())
        {
            return "authentication/registration";
        }

        try
        {
            userService.create(registrationRequest);
        }
        catch (EmailNotUniqueException exception)
        {
            bindingResult.rejectValue("email", "email.exists", exception.getMessage());
            return "authentication/registration";
        }

        return "redirect:/login?registered";
    }

    //Todo move to separate Controller
    //Sitter Profile page getter
    @GetMapping("/sitter/profile/edit")
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

    //Post sitter Profile update
    @PostMapping("/sitter/profile/edit")
    public String updateSitterProfile(
            Authentication authentication,
            @Valid @ModelAttribute("sitterProfileUpdate")
            SitterProfileUpdateDTO dto,
            BindingResult bindingResult)
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

    @PostMapping("/sitter/profile/availability")
    public String addSitterAvailability(
            Authentication authentication,
            @Valid
            @ModelAttribute("availabilityRequest")
            SitterAvailabilityRequest availabilityRequest,
            BindingResult bindingResult,
            Model model)
    {
        if (bindingResult.hasErrors())
        {
            SitterProfile sitterProfile = sitterProfileService.getProfileByUserEmail(authentication.getName());
            model.addAttribute("sitter", sitterProfile);

            return "sitter/sitterProfile";
        }

        sitterProfileService.addAvailability(authentication.getName(), availabilityRequest);

        return "redirect:/sitter/profile";
    }

    @GetMapping("/sitter/profile")
    public String sitterProfilePage(Authentication authentication, Model model)
    {
        SitterProfile sitterProfile = sitterProfileService.getProfileByUserEmail(authentication.getName());
        model.addAttribute("sitter", sitterProfile);
        model.addAttribute("availabilityRequest", new SitterAvailabilityRequest(null, null));
        model.addAttribute("availabilityRanges", sitterProfileService.getAvailability(authentication.getName()));

        return "sitter/sitterProfile";
    }

    //remove Availability Date
    @PostMapping("/sitter/profile/availability/{id}/delete")
    public String deleteSitterAvailability(@PathVariable Long id, Authentication authentication)
    {
        sitterProfileService.deleteAvailability(authentication.getName(), id);

        return "redirect:/sitter/profile";
    }


    //Try to publish + error checks
    @PostMapping("/sitter/profile/publish")
    public String publishSitterProfile(Authentication authentication, Model model)
    {
        try
        {
            sitterProfileService.publishProfile(authentication.getName());
        }
        catch (IllegalStateException exception)
        {
            SitterProfile sitterProfile = sitterProfileService.getProfileByUserEmail(authentication.getName());

            model.addAttribute("sitter", sitterProfile);
            model.addAttribute("availabilityRequest", new SitterAvailabilityRequest(null, null)
            );
            model.addAttribute("availabilityRanges", sitterProfileService.getAvailability(authentication.getName()));
            model.addAttribute("publishError", exception.getMessage());

            return "sitter/sitterProfile";
        }

        return "redirect:/sitter/profile";
    }

    //unpublish profile
    @PostMapping("/sitter/profile/unpublish")
    public String unpublishSitterProfile(Authentication authentication) {
        sitterProfileService.unpublishProfile(authentication.getName());

        return "redirect:/sitter/profile";
    }

    @GetMapping("/owner/pets/add")
    public String addPet(Model model){
        model.addAttribute("petRequest", new PetRequestDto());
        return "owner/addPet";
    }

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