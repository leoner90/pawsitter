package lv.pawsitter.controller;

import lombok.RequiredArgsConstructor;
import lv.pawsitter.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;



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
}