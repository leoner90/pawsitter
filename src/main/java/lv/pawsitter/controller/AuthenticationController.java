package lv.pawsitter.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lv.pawsitter.dto.userdto.UserCreateDTO;
import lv.pawsitter.exception.EmailNotUniqueException;
import lv.pawsitter.service.userservice.UserService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.context.SecurityContextRepository;


@Controller
@RequiredArgsConstructor
public class AuthenticationController
{
//************* VAR
    private final UserService userService;

    //    Automatic login after registration
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

//************* GETTERS // if logged in already redirect to dashboard
    @GetMapping("/login")
    public String loginPage(Authentication authentication)
    {
        //redirect to home page if already longed in
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken))
        {
            return "redirect:/";
        }

        return "authentication/login";
    }

    @GetMapping("/registration")
    public String registrationPage(Model model, Authentication authentication)
    {
        //redirect to home page if already longed in
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken))
        {
            return "redirect:/";
        }
        
        model.addAttribute("registrationRequest", new UserCreateDTO("", "", "", "", "", "", "", null));
        return "authentication/registration";
    }

//************* POST
//HttpServletRequest request, HttpServletResponse response Spring save the logged-in user into the browser session
    @PostMapping("/registration")
    public String registerUser(@Valid @ModelAttribute("registrationRequest") UserCreateDTO registrationRequest, BindingResult bindingResult, HttpServletRequest request, HttpServletResponse response)
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

            // Authentication after reg via asking spring security to verify them(email password)
            UsernamePasswordAuthenticationToken authenticationToken =
                    UsernamePasswordAuthenticationToken.unauthenticated(registrationRequest.email(), registrationRequest.password());
            Authentication authentication = authenticationManager.authenticate(authenticationToken);

            //basically create session and put user context there
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);

            securityContextRepository.saveContext(securityContext, request, response);

            //check is User a sitter and redirect to proper dashboard
            boolean isSitter = authentication.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("SITTER"));
            return isSitter ? "redirect:/sitter/dashboard" : "redirect:/owner/dashboard";
        }
        catch (EmailNotUniqueException exception)
        {
            bindingResult.rejectValue("email", "email.exists", exception.getMessage());
            return "authentication/registration";
        }
    }
}