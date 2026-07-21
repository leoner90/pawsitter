package lv.pawsitter.advice;

import lombok.extern.slf4j.Slf4j;
import lv.pawsitter.exception.PasswordMismatchException;
import lv.pawsitter.exception.UserNotFoundException;
import lv.pawsitter.exception.recoveryexception.RecoveryExpiredException;
import lv.pawsitter.exception.recoveryexception.RecoveryNotFoundException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class RecoveryExceptionHandler {

    @ExceptionHandler(RecoveryNotFoundException.class)
    public String handleRecoveryNotFound(RecoveryNotFoundException ex, Model model) {
        log.warn("Recovery not found: {}", ex.getMessage());
        model.addAttribute("error", ex.getMessage());
        return "recovery/updatePassword";
    }

    @ExceptionHandler(RecoveryExpiredException.class)
    public String handleRecoveryExpired(RecoveryExpiredException ex, Model model) {
        log.warn("Recovery expired: {}", ex.getMessage());
        model.addAttribute("error", ex.getMessage());
        return "recovery/updatePassword";
    }

    @ExceptionHandler(PasswordMismatchException.class)
    public String handlePasswordMismatch(PasswordMismatchException ex, Model model) {
        log.warn("Password mismatch: {}", ex.getMessage());
        model.addAttribute("error", ex.getMessage());
        return "recovery/updatePassword";
    }

    @ExceptionHandler(UserNotFoundException.class)
    public String handleUserNotFound(UserNotFoundException ex, Model model) {
        log.warn("User not found: {}", ex.getMessage());
        model.addAttribute("error", ex.getMessage());
        return "recovery/getRecoveryToken";
    }
}
