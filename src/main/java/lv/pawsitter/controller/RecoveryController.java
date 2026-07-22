package lv.pawsitter.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lv.pawsitter.dto.RecoveryRequestDTO;
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
    public String sendRecoveryEmail(@RequestParam String email) {
        recoveryService.generateAndEmail(email);
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
        if (result.hasErrors()) {

            model.addAttribute("recoveryToken", recoveryToken);
            model.addAttribute("recoveryRequestDTO", request);

            return "recovery/updatePassword";
        }

        recoveryService.changePassword(
                recoveryToken,
                request.newPassword(),
                request.confirmNewPassword()
        );

        return "redirect:/authentication/login";
    }
}