package lv.pawsitter.dto;

import jakarta.validation.constraints.*;
import lv.pawsitter.model.RoleType;

public record UserCreateDTO
        (
            @NotBlank
            @Size(min = 2, max = 50)
            @Pattern(regexp = "^[A-Za-zÀ-ž\\s'-]+$", message = "Must contain only letters")
            String firstName,

            @NotBlank
            @Size(min = 2, max = 50)
            @Pattern(regexp = "^[A-Za-zÀ-ž\\s'-]+$", message = "Must contain only letters")
            String lastName,

            @NotBlank
            @Pattern(
                    regexp = "^\\+?[0-9]{7,15}$",
                    message = "Phone number must be valid international format"
            )
            String phoneNumber,

            @NotBlank
            @Email
            @Size(max = 100)
            String email,

            @NotBlank
            @Email
            @Size(max = 100)
            String confirmEmail,

            @NotBlank
            @Size(min = 6, max = 100)
            String password,

            @NotBlank
            @Size(min = 6, max = 100)
            String confirmPassword,

            @NotNull
            RoleType role
        )
{
}
