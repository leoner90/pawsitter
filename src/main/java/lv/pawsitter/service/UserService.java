package lv.pawsitter.service;

import lv.pawsitter.dto.UserCreateDTO;
import lv.pawsitter.dto.UserDTO;
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

    User getCurrentUser();
}