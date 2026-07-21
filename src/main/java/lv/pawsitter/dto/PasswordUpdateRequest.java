package lv.pawsitter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordUpdateRequest(@NotBlank
                                    @Size(min = 6, max = 100)
                                    String newPassword,

                                    @NotBlank
                                    @Size(min = 6, max = 100)

                                    String confirmNewPassword) {
}
