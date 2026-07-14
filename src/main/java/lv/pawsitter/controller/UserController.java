package lv.pawsitter.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lv.pawsitter.dto.UserCreateDTO;
import lv.pawsitter.dto.UserDTO;
import lv.pawsitter.model.RoleType;
import lv.pawsitter.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@Validated
@Slf4j
@RequiredArgsConstructor
public class UserController {
    private final UserService service;

//    private final AuthenticationService authentication;

    @PostMapping
    public ResponseEntity<UserDTO> registerUser(@Valid @RequestBody UserCreateDTO dto) {
        log.debug("registerUser email={}", dto.email());
        UserDTO created = service.create(dto);
        log.info("User created id={}, email={}", created.id(), created.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        log.debug("getAllUsers called");
        List<UserDTO> users = service.findAll();
        log.info("getAllUsers returned count={}", users.size());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable @Positive long id) {
        log.debug("getUserById id={}", id);
        UserDTO dto = service.findById(id);
        log.info("getUserById succeeded id={}, email={}", id, dto.email());
        return ResponseEntity.ok(dto);
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<UserDTO> setUserRole(@PathVariable @Positive long id,
                                               @RequestParam @NotNull RoleType newRole) {
        log.debug("setUserRole id={}, newRole={}", id, newRole);
        UserDTO updated = service.update(id, newRole);
        log.info("setUserRole succeeded id={}, newRole={}", id, newRole);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeUserById(@PathVariable @Positive long id) {
        log.debug("removeUserById id={}", id);
        service.delete(id);
        log.info("removeUserById succeeded id={}", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-email")
    public ResponseEntity<UserDTO> getUserByEmail(@RequestParam @Email @NotBlank String email) {
        log.debug("getUserByEmail email={}", email);
        UserDTO dto = service.findByEmail(email);
        log.info("getUserByEmail succeeded id={}, email={}", dto.id(), dto.email());
        return ResponseEntity.ok(dto);
    }

//    @PostMapping("/login")
//    public JwtAuthenticationResponse login(@Valid @RequestBody SignInRequest requestBody) {
//        log.debug("login attempt login={}", masking.maskLogin(requestBody.getLogin()));
//        JwtAuthenticationResponse resp = authentication.authenticate(requestBody);
//        log.info("login succeeded login={}", masking.maskLogin(requestBody.getLogin()));
//        return resp;
//    }
}
