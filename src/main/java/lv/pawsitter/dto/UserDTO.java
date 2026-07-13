package lv.pawsitter.dto;

import lombok.Builder;
import lv.pawsitter.entity.OwnerProfile;
import lv.pawsitter.entity.SitterProfile;
import lv.pawsitter.model.RoleType;

import java.time.LocalDateTime;

public record UserDTO(Long id,

                      String phoneNumber,

                      String email,

                      RoleType role,

                      OwnerProfile ownerProfile,

                      SitterProfile sitterProfile,

                      LocalDateTime createdAt) {
}
