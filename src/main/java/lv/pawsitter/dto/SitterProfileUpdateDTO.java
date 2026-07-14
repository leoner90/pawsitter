package lv.pawsitter.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

public record SitterProfileUpdateDTO(

        @NotBlank
        String location,

        @Pattern(
                regexp = "^\\+?[0-9]{7,15}$",
                message = "Phone number must be valid international format"
        )
        String phoneNumber,

        @DecimalMin(
                value = "0.00",
                message = "Price cannot be negative"
        )
        BigDecimal pricePerDay,

        @Size(
                max = 1000,
                message = "Description cannot exceed 1000 characters"
        )
        String description,

        MultipartFile image
) {
}