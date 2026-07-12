package lv.pawsitter.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lv.pawsitter.dto.RegistrationRequest;
import lv.pawsitter.service.RegistrationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class RegistrationController
{
    private final RegistrationService registrationService;

    @GetMapping("/registration")
    public String showRegistrationPage(Model model)
    {
        model.addAttribute("registrationRequest", new RegistrationRequest());
        return "authentication/registration";
    }

    @PostMapping("/registration")
    public String registerUser(
            @Valid @ModelAttribute("registrationRequest")
            RegistrationRequest registrationRequest,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors())
        {
            return "authentication/registration";
        }

        try
        {
            registrationService.register(registrationRequest);
        }
        catch (IllegalArgumentException exception)
        {
            model.addAttribute("registrationError", exception.getMessage());
            return "authentication/registration";
        }
        return "redirect:/login?registered";
    }
}