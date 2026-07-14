package lv.pawsitter.dto;


import lv.pawsitter.model.RoleType;

import java.time.LocalDateTime;

public record UserDTO(Long id,

                      String phoneNumber,

                      String email,

                      RoleType role,

                      Long ownerProfileId,

                      Long sitterProfileId,

                      LocalDateTime createdAt) {
}
