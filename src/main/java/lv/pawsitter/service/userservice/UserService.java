package lv.pawsitter.service.userservice;

import lv.pawsitter.dto.userdto.UserCreateDTO;
import lv.pawsitter.dto.userdto.UserDTO;
import lv.pawsitter.entity.User;
import lv.pawsitter.model.RoleType;

import java.util.List;

public interface UserService {

    UserDTO create(UserCreateDTO dto);

    List<UserDTO> findAll();

    UserDTO findById(long id);

    UserDTO update(long id, RoleType newRole);

    void delete(long id);

    UserDTO findByEmail(String email);

    User getAuthenicatedCurrentUser();
}