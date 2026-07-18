package lv.pawsitter.controller;

import lombok.RequiredArgsConstructor;
import lv.pawsitter.entity.SitterProfile;
import lv.pawsitter.service.SitterProfileService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class SitterSearchController
{
//****** VAR
    private final SitterProfileService sitterProfileService;

//****** GETTERS
    //Sitters Search Page Getter
    @GetMapping("/sittersSearch")
    public String sitterSearchPage(Model model)
    {
        model.addAttribute("sitters",sitterProfileService.getPublishedSitters());
        return "sittersSearch";
    }

    //Sitters Details Page For Booking( not part of sitter profile)
    @GetMapping("/sitters/{id}")
    public String sitterDetailsPage(@PathVariable Long id, Model model)
    {
        model.addAttribute("sitter", sitterProfileService.getSitterById(id));
        return "sitter/sitterDetails";
    }

    // Get the requested date range, validate it, and show fully available sitters / or partially available
    @GetMapping("/sitters/search")
    public String searchSitters(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "false") boolean includePartial,
            Model model)
    {
        model.addAttribute("selectedCity", city);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("includePartial", includePartial);

        try
        {
            List<SitterProfile> sitters = sitterProfileService.searchSitters(city, startDate, endDate, maxPrice, includePartial);
            model.addAttribute("sitters", sitters);
        }
        catch (IllegalArgumentException exception)
        {
            model.addAttribute("sitters", List.of());
            model.addAttribute("searchError", exception.getMessage());
        }

        return "sittersSearch";
    }
}