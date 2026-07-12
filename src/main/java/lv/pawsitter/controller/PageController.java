package lv.pawsitter.controller;

import lombok.RequiredArgsConstructor;
import lv.pawsitter.service.SitterProfileService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable; // @PathVariable takes a value directly from the UR
import lv.pawsitter.entity.SitterProfile;
import org.springframework.security.core.Authentication;
import lv.pawsitter.entity.OwnerProfile;
import lv.pawsitter.service.OwnerProfileService;

//TODO: Split into separete controllers HomeController, LogginController, OwnerProfileController,SitterProfileController etc.
@Controller
@RequiredArgsConstructor
public class PageController
{
    private final SitterProfileService sitterProfileService;
    private final OwnerProfileService ownerProfileService;

    @GetMapping("/")
    public String homePage(Model model)
    {
        model.addAttribute("sitters", sitterProfileService.getAllSitters());
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

    @GetMapping("/sitter/profile")
    public String sitterProfilePage(Authentication authentication, Model model)
    {
        SitterProfile sitterProfile = sitterProfileService.getProfileByUserEmail(authentication.getName());
        model.addAttribute("sitter", sitterProfile);
        return "sitter/sitterProfile";
    }

    @GetMapping("/sitters/{id}")
    public String sitterDetailsPage(@PathVariable Long id, Model model)
    {
        model.addAttribute("sitter", sitterProfileService.getSitterById(id));
        return "sitter/sitterDetails";
    }
}