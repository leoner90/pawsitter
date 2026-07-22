package lv.pawsitter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object used for submitting a password update request
 * during the account recovery flow.
 *
 * <p>This DTO carries two user‑provided password fields:
 * <ul>
 *     <li>{@code newPassword} – the new password the user wants to set</li>
 *     <li>{@code confirmNewPassword} – repeated password for confirmation</li>
 * </ul>
 *
 * <p>Both fields are validated using Bean Validation annotations:
 * <ul>
 *     <li>{@code @NotBlank} – ensures the field is not empty</li>
 *     <li>{@code @Size(min = 6, max = 100)} – enforces password length constraints</li>
 * </ul>
 *
 * <p>Business‑level validation (such as checking that both passwords match)
 * is performed in the {@code RecoveryService}.
 */
public record RecoveryRequestDTO(

        @NotBlank
        @Size(min = 6, max = 100)
        String newPassword,

        @NotBlank
        @Size(min = 6, max = 100)
        String confirmNewPassword
) {
}
