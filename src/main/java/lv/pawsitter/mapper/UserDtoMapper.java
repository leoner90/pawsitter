package lv.pawsitter.mapper;

import lv.pawsitter.dto.UserCreateDTO;
import lv.pawsitter.dto.UserDTO;
import lv.pawsitter.entity.User;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between User entities and User DTOs.
 * Handles transformation of user data for API responses and persistence.
 */
@Component
public class UserDtoMapper implements Converter<User, UserCreateDTO, UserDTO> {
    /**
     * Converts a User entity into a UserDTO.
     *
     * @param user the entity to convert
     * @return the corresponding DTO
     */
    @Override
    public UserDTO entityToDto(User user) {
        return new UserDTO(user.getId(),
                user.getPhoneNumber(),
                user.getEmail(),
                user.getRole(),
                //user.getOwnerProfile() != null ? user.getOwnerProfile().getId() : null,
                //user.getSitterProfile() != null ? user.getSitterProfile().getId() : null,
                user.getCreatedAt());
    }

    /**
     * Converts a UserCreateDTO into a User entity.
     *
     * @param dto the DTO containing user creation data
     * @return the new User entity
     */
    @Override
    public User dtoToEntity(UserCreateDTO dto) {
        return new User();
    }
}
