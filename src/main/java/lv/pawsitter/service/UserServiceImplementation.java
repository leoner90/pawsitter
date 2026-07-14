package lv.pawsitter.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lv.pawsitter.dto.UserCreateDTO;
import lv.pawsitter.dto.UserDTO;
import lv.pawsitter.entity.OwnerProfile;
import lv.pawsitter.entity.SitterProfile;
import lv.pawsitter.entity.User;
import lv.pawsitter.exception.EmailNotUniqueException;
import lv.pawsitter.exception.UserNotFoundException;
import lv.pawsitter.mapper.Converter;
import lv.pawsitter.model.RoleType;
import lv.pawsitter.repository.OwnerProfileRepository;
import lv.pawsitter.repository.SitterProfileRepository;
import lv.pawsitter.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImplementation implements UserService {
    private final UserRepository repository;

    private final PasswordEncoder encoder;

    private final Converter<User, UserCreateDTO, UserDTO> converter;

    //to create   extra database record
    private final OwnerProfileRepository ownerProfileRepository;
    private final SitterProfileRepository sitterProfileRepository;

    @Override
    @Transactional
    public UserDTO create(UserCreateDTO dto) {
        Objects.requireNonNull(dto, "UserCreateDTO must not be null");
        String email = normalizeEmail(dto.email());
        log.debug("create user with email={}", email);
        String password = normalizePassword(dto.password());
        if (!email.matches("^[^@]+@[^@]+\\.[^@]+$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        repository.findByEmail(email).ifPresent(u -> {
            log.warn("User creation failed — email already exists: {}", email);
            throw new EmailNotUniqueException("User with email " + email + " already exists.");
        });
        User user = converter.dtoToEntity(dto);
        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
        user.setPhoneNumber(dto.phoneNumber());
        user.setEmail(email);
        user.setPassword(encoder.encode(password));
        user.setRole(dto.role());
        try {
            User saved = repository.save(user);

            //Create the corresponding profile record based on the user role
            if (saved.getRole() == RoleType.USER) {
                OwnerProfile ownerProfile = new OwnerProfile();
                ownerProfile.setUser(saved);
                ownerProfileRepository.save(ownerProfile);
            } else if (saved.getRole() == RoleType.SITTER) {
                SitterProfile sitterProfile = new SitterProfile();
                sitterProfile.setUser(saved);
                sitterProfileRepository.save(sitterProfile);
            }

            log.info("User created id={}, email={}", saved.getId(), saved.getEmail());
            return converter.entityToDto(saved);
        } catch (DataIntegrityViolationException e) {
            throw new EmailNotUniqueException("User with email " + email + " already exists.");
        }
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @Transactional(readOnly = true)
    public List<UserDTO> findAll() {
        log.debug("findAll users");
        return repository.findAll().stream().map(converter::entityToDto).toList();
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @Transactional(readOnly = true)
    public UserDTO findById(long id) {
        validateId(id);
        log.debug("findById id={}", id);
        return repository.findById(id)
                .map(user -> {
                    log.info("User found id={}", id);
                    return converter.entityToDto(user);
                })
                .orElseThrow(() -> {
                    log.warn("User not found id={}", id);
                    return new UserNotFoundException("User with id " + id + " is not found.");
                });
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @Transactional
    public UserDTO update(long id, RoleType newRole) {
        validateId(id);
        Objects.requireNonNull(newRole, "Role must not be null");
        log.debug("update user id={}, newRole={}", id, newRole);
        User user = repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User update failed — not found id={}", id);
                    return new UserNotFoundException("User with id " + id + " is not found.");
                });
        user.setRole(newRole);
        User saved = repository.save(user);
        log.info("User updated id={}, newRole={}", id, newRole);
        return converter.entityToDto(saved);
    }

    @Override
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    @Transactional
    public void delete(long id) {
        validateId(id);
        User current = getCurrentUser();
        log.debug("delete user id={} by user={}", id, current.getId());
        User userToDelete = repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User delete failed — not found id={}", id);
                    return new UserNotFoundException("User with id " + id + " is not found.");
                });
        if (current.getRole() == RoleType.USER && !Objects.equals(current.getId(), id)) {
            log.warn("User {} attempted to delete another user {}", current.getId(), id);
            throw new AccessDeniedException("You do not have permission to delete another user.");
        }
        repository.delete(userToDelete);
        log.info("User deleted id={}", id);
    }

    @Override
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    @Transactional(readOnly = true)
    public UserDTO findByEmail(String email) {
        String normalized = normalizeEmail(email);
        User current = getCurrentUser();
        log.debug("findByEmail email={} by user={}", normalized, current.getId());
        User user = repository.findByEmail(normalized)
                .orElseThrow(() -> {
                    log.warn("User not found by email={}", normalized);
                    return new UserNotFoundException("User with email " + normalized + " is not found.");
                });
        if (current.getRole() == RoleType.USER && !Objects.equals(current.getEmail(), normalized)) {
            log.warn("User {} attempted to access another user's data {}", current.getId(), normalized);
            throw new AccessDeniedException("You do not have permission to view this user.");
        }
        log.info("User found by email={}", normalized);
        return converter.entityToDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            log.warn("getCurrentUser failed — no authenticated user");
            throw new SecurityException("User is not authenticated");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails details) {
            String email = normalizeEmail(details.getUsername());
            log.debug("getCurrentUser principal email={}", email);
            return repository.findByEmail(email)
                    .orElseThrow(() -> {
                        log.error("Authenticated user not found in DB email={}", email);
                        return new UserNotFoundException("User with username " + email + " is not found.");
                    });
        }
        log.error("Invalid authentication principal type={}", principal.getClass().getName());
        throw new IllegalArgumentException("Cannot obtain user from authentication principal");
    }

    private void validateId(long id) {
        if (id <= 0) throw new IllegalArgumentException("Id must be positive");
    }

    private String normalizeEmail(String email) {
        return Objects.requireNonNull(email, "email must not be null").trim().toLowerCase();
    }

    private String normalizePassword(String password) {
        return Objects.requireNonNull(password, "password must not be null").trim();
    }
}