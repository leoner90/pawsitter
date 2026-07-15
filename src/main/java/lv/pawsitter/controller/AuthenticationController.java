package lv.pawsitter.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lv.pawsitter.dto.UserCreateDTO;
import lv.pawsitter.exception.EmailNotUniqueException;
import lv.pawsitter.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class AuthenticationController
{
//************* VAR
    private final UserService userService;

//************* GETTERS
    @GetMapping("/login")
    public String loginPage()
    {
        return "authentication/login";
    }

    @GetMapping("/registration")
    public String registrationPage(Model model)
    {
        model.addAttribute("registrationRequest", new UserCreateDTO("", "", "", "", "", "", "", null));
        return "authentication/registration";
    }

//************* POST
    @PostMapping("/registration")
    public String registerUser(@Valid @ModelAttribute("registrationRequest") UserCreateDTO registrationRequest, BindingResult bindingResult)
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
}