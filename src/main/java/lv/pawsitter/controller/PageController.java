package lv.pawsitter.controller;

import lombok.RequiredArgsConstructor;
import lv.pawsitter.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable; // @PathVariable takes a value directly from the UR


@Controller
@RequiredArgsConstructor
public class PageController
{
    private final SitterProfileService sitterProfileService;

    //Home Page
    @GetMapping("/")
    public String homePage(Model model)
    {
        model.addAttribute("sitters",sitterProfileService.getPublishedSitters());
        return "index";
    }

    //Sitters Details Page For Booking( not part of sitter profile)
    @GetMapping("/sitters/{id}")
    public String sitterDetailsPage(@PathVariable Long id, Model model)
    {
        model.addAttribute("sitter", sitterProfileService.getSitterById(id));
        return "sitter/sitterDetails";
    }
}