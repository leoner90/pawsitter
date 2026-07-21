package lv.pawsitter.controller;

import lombok.RequiredArgsConstructor;
import lv.pawsitter.entity.OwnerProfile;
import lv.pawsitter.entity.Pet;
import lv.pawsitter.entity.SitterProfile;
import lv.pawsitter.service.BookingService;
import lv.pawsitter.service.OwnerProfileService;
import lv.pawsitter.service.SitterProfileService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController
{
    private final SitterProfileService sitterProfileService;
    private final OwnerProfileService ownerProfileService;
    private final BookingService bookingService;

//****** GETTERS
    @GetMapping("/dashboard")
    public String dashboardRedirect(Authentication authentication)
    {
        boolean isSitter = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("SITTER"));

        return isSitter ? "redirect:/sitter/dashboard" : "redirect:/owner/dashboard";
    }

    //OWNER DASHBOARD
    @GetMapping("/owner/dashboard")
    public String ownerDashboard(Authentication authentication, Model model)
    {
        OwnerProfile ownerProfile = ownerProfileService.getProfileByUserEmail(authentication.getName());
        model.addAttribute("firstName", ownerProfile.getUser().getFirstName());
        //we want only active pets for dashboards
        model.addAttribute("petCount", ownerProfile.getPets().stream().filter(Pet::isActive).count());
        model.addAttribute("bookingCount", bookingService.getOwnerBookings(authentication.getName(), null).size());
        model.addAttribute("location", ownerProfile.getLocation());
        return "owner/ownerDashboard";
    }

    //SITTER DASHBOARD
    @GetMapping("/sitter/dashboard")
    public String sitterDashboard(Authentication authentication, Model model)
    {
        SitterProfile sitterProfile = sitterProfileService.getProfileByUserEmail(authentication.getName());
        model.addAttribute("firstName", sitterProfile.getUser().getFirstName());
        model.addAttribute("published", sitterProfile.isPublished());
        model.addAttribute("pricePerDay", sitterProfile.getPricePerDay());
        model.addAttribute("availabilityCount", sitterProfileService.getAvailability(authentication.getName()).size());
        return "sitter/sitterDashboard";
    }
}