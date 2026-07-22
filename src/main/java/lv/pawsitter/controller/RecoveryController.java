package lv.pawsitter.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lv.pawsitter.dto.RecoveryRequestDTO;
import lv.pawsitter.exception.PasswordMismatchException;
import lv.pawsitter.exception.UserNotFoundException;
import lv.pawsitter.exception.recoveryexception.RecoveryExpiredException;
import lv.pawsitter.exception.recoveryexception.RecoveryNotFoundException;
import lv.pawsitter.service.recoveryservice.RecoveryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class RecoveryController {

    private final RecoveryService recoveryService;

    @GetMapping("/recovery")
    public String recoveryPage() {
        return "recovery/getRecoveryToken";
    }

    @PostMapping("/recovery")
    public String sendRecoveryEmail(
            @RequestParam String email,
            Model model
    ) {
        try {
            recoveryService.generateAndEmail(email);
        } catch (UserNotFoundException exception) {
            model.addAttribute(
                    "recoveryError",
                    "No account was found with this email address."
            );

            return "recovery/getRecoveryToken";
        }

        return "recovery/emailSent";
    }

    @GetMapping("/recovery/updatePassword")
    public String updatePasswordPage(@RequestParam String recoveryToken, Model model) {
        model.addAttribute("recoveryToken", recoveryToken);
        model.addAttribute("recoveryRequestDTO",
                new RecoveryRequestDTO("", ""));
        return "recovery/updatePassword";
    }

    @PostMapping("/recovery/updatePassword")
    public String updatePassword(
            @RequestParam String recoveryToken,
            @Valid @ModelAttribute("recoveryRequestDTO") RecoveryRequestDTO request,
            BindingResult result,
            Model model
    ) {
        model.addAttribute("recoveryToken", recoveryToken);

        if (result.hasErrors()) {
            return "recovery/updatePassword";
        }

        try {
            recoveryService.changePassword(
                    recoveryToken,
                    request.newPassword(),
                    request.confirmNewPassword()
            );
        } catch (PasswordMismatchException exception) {
            result.rejectValue(
                    "confirmNewPassword",
                    "password.mismatch",
                    exception.getMessage()
            );

            return "recovery/updatePassword";

        } catch (RecoveryExpiredException | RecoveryNotFoundException exception) {
        model.addAttribute(
                "recoveryError",
                "This recovery link is invalid or expired. Please request a new one."
        );

            return "recovery/updatePassword";
        }

        return "redirect:/authentication/login";
    }
}