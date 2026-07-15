package lv.pawsitter.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record SitterPublishDTO(

        @NotBlank(message = "Location is required")
        String location,

        @NotBlank(message = "Description is required")
        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than 0")
        BigDecimal pricePerDay,

        @NotBlank(message = "Phone number is required")
        String phoneNumber
) {
}