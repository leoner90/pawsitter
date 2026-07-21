package lv.pawsitter.controller;

import lombok.RequiredArgsConstructor;
import lv.pawsitter.service.recoveryservice.RecoveryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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
        return "recovery/updatePassword";
    }

    @PostMapping("/recovery/updatePassword")
    public String updatePassword(
            @RequestParam String recoveryToken,
            @RequestParam String newPassword,
            @RequestParam String confirmNewPassword
    ) {
        recoveryService.changePassword(recoveryToken, newPassword, confirmNewPassword);
        return "redirect:/authentication/login";
    }
}