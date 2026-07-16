package lv.pawsitter.dto;

import lombok.Getter;
import lombok.Setter;
import lv.pawsitter.entity.AnimalTypes;

import java.time.LocalDateTime;

@Getter
@Setter
public class PetResponseDto {
    private Long id;
    private Long ownerId;
    private String firstName;
    private String lastName;
    private String nickName;
    private AnimalTypes animalType;
    private String breed;
    private int age;
    private String description;
    private String specialNeeds;
    private String imageUrl;
    private LocalDateTime createdAt;
}
