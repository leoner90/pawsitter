package lv.pawsitter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import lv.pawsitter.entity.AnimalTypes;

@Getter
@Setter
public class PetRequestDto {
    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    private String nickName;

    @NotNull
    private AnimalTypes animalType;

    private String breed;

    @Positive
    private int age;

    @NotBlank
    private String description;

    private String specialNeeds;

    private String imageUrl;
}
