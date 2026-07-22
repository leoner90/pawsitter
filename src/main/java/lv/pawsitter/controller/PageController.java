package lv.pawsitter.controller;

import lombok.RequiredArgsConstructor;
import lv.pawsitter.entity.OwnerProfile;
import lv.pawsitter.entity.SitterProfile;
import lv.pawsitter.service.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable; // @PathVariable takes a value directly from the UR
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;


@Controller
@RequiredArgsConstructor
public class PageController
{
    private final SitterProfileService sitterProfileService;
    private final OwnerProfileService ownerProfileService;
    private final BookingService bookingService;

    //Home Page
    @GetMapping("/")
    public String homePage(Model model)
    {
        model.addAttribute("sitters",sitterProfileService.getPublishedSitters());
        return "index";
    }
    @GetMapping("/dashboard")
    public String dashboardRedirect(Authentication authentication)
    {
        boolean isSitter = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("SITTER"));

        return isSitter ? "redirect:/sitter/dashboard" : "redirect:/owner/dashboard";
    }

    @GetMapping("/owner/dashboard")
    public String ownerDashboard(Authentication authentication, Model model)
    {
        OwnerProfile ownerProfile = ownerProfileService.getProfileByUserEmail(authentication.getName());
        model.addAttribute("firstName", ownerProfile.getUser().getFirstName());
        model.addAttribute("petCount", ownerProfile.getPets().size());
        model.addAttribute("bookingCount", bookingService.getOwnerBookings(authentication.getName(), null).size());
        model.addAttribute("location", ownerProfile.getLocation());
        return "owner/ownerDashboard";
    }

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

    //Sitters Details Page For Booking( not part of sitter profile)
    @GetMapping("/sitters/{id}")
    public String sitterDetailsPage(@PathVariable Long id, Model model)
    {
        model.addAttribute("sitter", sitterProfileService.getSitterById(id));
        return "sitter/sitterDetails";
    }

    //TODO  move to own controller
    //Sitters Search Page Getter
    @GetMapping("/sittersSearch")
    public String sitterSearchpage(Model model)
    {
        model.addAttribute("sitters",sitterProfileService.getPublishedSitters());
        return "sittersSearch";
    }

    // Get the requested date range, validate it, and show fully available sitters / or partially available
    //todo not sure maybe should create and validate in DTO
    @GetMapping("/sitters/search")
    public String searchSitters(@RequestParam LocalDate startDate, @RequestParam LocalDate endDate, @RequestParam(defaultValue = "false") boolean includePartial, Model model)
    {
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("includePartial", includePartial);

        if (startDate.isBefore(LocalDate.now()))
        {
            model.addAttribute("sitters", List.of());
            model.addAttribute("searchError", "Start date cannot be in the past");
            return "sittersSearch";
        }

        if (endDate.isBefore(startDate))
        {
            model.addAttribute("sitters", List.of());
            model.addAttribute("searchError", "End date cannot be before start date");
            return "sittersSearch";
        }

        List<SitterProfile> sitters;

        if (includePartial)
        {
            sitters = sitterProfileService.findPartiallyAvailableSitters(startDate, endDate);
        }
        else
        {
            sitters = sitterProfileService.findFullyAvailableSitters(startDate, endDate);
        }

        model.addAttribute("sitters", sitters);
        return "sittersSearch";
    }
}