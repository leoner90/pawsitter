package lv.pawsitter.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignInRequest(
        @NotBlank(message = "Login must not be blank")
        @Size(max = 254)
        String login,

        @NotBlank(message = "Password must not be blank")
        @Size(min = 6, max = 100)
        String password) {
}