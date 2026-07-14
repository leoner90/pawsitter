package lv.pawsitter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OwnerProfileUpdateDTO (
        @NotBlank
        @Size(min = 2, max = 25)
        @Pattern(regexp = "^[A-Za-zÀ-ž\\s'-]+$", message = "Must contain only letters")
        String firstName,

        @NotBlank
        @Size(min = 2, max = 25)
        @Pattern(regexp = "^[A-Za-zÀ-ž\\s'-]+$", message = "Must contain only letters")
        String lastName,

        @NotBlank
        @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Phone number must be valid international format")
        String phoneNumber,

        @Size(max = 100)
        String location,

        @Size(max = 10000)
        String description,

        String imageUrl
){

}
