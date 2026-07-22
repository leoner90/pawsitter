package lv.pawsitter.service.userservice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lv.pawsitter.dto.userdto.UserCreateDTO;
import lv.pawsitter.dto.userdto.UserDTO;
import lv.pawsitter.entity.OwnerProfile;
import lv.pawsitter.entity.SitterProfile;
import lv.pawsitter.entity.User;
import lv.pawsitter.exception.EmailMismatchException;
import lv.pawsitter.exception.EmailNotUniqueException;
import lv.pawsitter.exception.PasswordMismatchException;
import lv.pawsitter.exception.UserNotFoundException;
import lv.pawsitter.mapper.Converter;
import lv.pawsitter.model.RoleType;
import lv.pawsitter.repository.UserRepository;
import lv.pawsitter.utility.MaskingUtil;
import lv.pawsitter.utility.ValidationUtil;
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
public class UserServiceImpl implements UserService {
    private final UserRepository repository;

    private final PasswordEncoder encoder;

    private final Converter<User, UserCreateDTO, UserDTO> converter;

    private final MaskingUtil maskingUtil;

    private final ValidationUtil validationUtil;

    @Override
    @Transactional
    public UserDTO create(UserCreateDTO dto) {
        Objects.requireNonNull(dto, "UserCreateDTO must not be null");
        String email = validationUtil.normalizeEmail(dto.email());
        String maskedEmail = maskingUtil.maskEmail(email);
        String confirmEmail = validationUtil.normalizeEmail(dto.confirmEmail());
        String maskedConfirmEmail = maskingUtil.maskEmail(confirmEmail);

        log.debug("checking if email {} matches other {}", maskedEmail, maskedConfirmEmail);

        if (!Objects.equals(email, confirmEmail)) {

            log.warn("Email {} does not match other {}", maskedEmail, maskedConfirmEmail);

            throw new EmailMismatchException("Emails do not match");
        }
        String password = validationUtil.normalizePassword(dto.password());
        String maskedPassword = maskingUtil.maskPassword(password);
        String confirmPassword = validationUtil.normalizePassword(dto.confirmPassword());
        String maskedConfirmPassword = maskingUtil.maskPassword(confirmPassword);

        log.debug("checking if password {} matches other {}", maskedPassword, maskedConfirmPassword);

        if (!Objects.equals(password, confirmPassword)) {
            
            log.warn("Password {} does not match other {}", maskedPassword, maskedConfirmPassword);

            throw new PasswordMismatchException("Passwords do not match");
        }

        log.debug("create user with email={}", email);

        if (!email.matches("^[^@]+@[^@]+\\.[^@]+$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        repository.findByEmail(email).ifPresent(u -> {

            log.warn("User creation failed — email already exists: {}", maskedEmail);

            throw new EmailNotUniqueException("User with email " + email + " already exists.");
        });
        User user = converter.dtoToEntity(dto);
        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
        user.setPhoneNumber(dto.phoneNumber());
        user.setEmail(email);
        user.setPassword(encoder.encode(password));
        user.setRole(dto.role());
        if (user.getRole() == RoleType.USER) {
            user.setOwnerProfile(new OwnerProfile());
        } else if (user.getRole() == RoleType.SITTER) {
            user.setSitterProfile(new SitterProfile());
        }
        try {
            User saved = repository.save(user);

            log.info("User created id={}, email={}", maskingUtil.maskId(String.valueOf(saved.getId())),
                    maskingUtil.maskEmail(saved.getEmail()));

            return converter.entityToDto(saved);
        } catch (DataIntegrityViolationException e) {
            throw new EmailNotUniqueException("User with email " + maskedEmail + " already exists.");
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
        validationUtil.validateId(id);
        String maskedId = maskingUtil.maskId(String.valueOf(id));

        log.debug("findById id={}", maskedId);

        return repository.findById(id)
                .map(user -> {

                    log.info("User found id={}", maskedId);

                    return converter.entityToDto(user);
                })
                .orElseThrow(() -> {

                    log.warn("User not found id={}", maskedId);

                    return new UserNotFoundException(id);
                });
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @Transactional
    public UserDTO update(long id, RoleType newRole) {
        validationUtil.validateId(id);
        String maskedId = maskingUtil.maskId(String.valueOf(id));
        Objects.requireNonNull(newRole, "Role must not be null");

        log.debug("update user id={}, newRole={}", maskedId, newRole);

        User user = repository.findById(id)
                .orElseThrow(() -> {

                    log.warn("User update failed — not found id={}", maskedId);

                    return new UserNotFoundException(id);
                });
        user.setRole(newRole);
        User saved = repository.save(user);

        log.info("User updated id={}, newRole={}", maskedId, newRole);

        return converter.entityToDto(saved);
    }

    @Override
    @PreAuthorize("hasAnyAuthority('USER', 'SITTER', 'ADMIN')")
    @Transactional
    public void delete(long id) {
        validationUtil.validateId(id);
        String maskedId = maskingUtil.maskId(String.valueOf(id));
        User current = getAuthenicatedCurrentUser();
        String maskedCurrentId = maskingUtil.maskId(String.valueOf(current.getId()));

        log.debug("delete user id={} by user={}", maskedId, maskedCurrentId);

        User userToDelete = repository.findById(id)
                .orElseThrow(() -> {

                    log.warn("User delete failed — not found id={}", maskedId);

                    return new UserNotFoundException(id);
                });
        if ((current.getRole() == RoleType.USER || current.getRole() == RoleType.SITTER)
                && !Objects.equals(current.getId(), id)) {

            log.warn("User {} attempted to delete another user {}", maskedCurrentId, maskedId);

            throw new AccessDeniedException("You do not have permission to delete another user.");
        }
        repository.delete(userToDelete);

        log.info("User deleted id={}", id);
    }

    @Override
    @PreAuthorize("hasAnyAuthority('USER', 'SITTER', 'ADMIN')")
    @Transactional(readOnly = true)
    public UserDTO findByEmail(String email) {
        String normalized = validationUtil.normalizeEmail(email);
        String maskedEmail = maskingUtil.maskEmail(normalized);
        User current = getAuthenicatedCurrentUser();
        String maskedCurrentId = maskingUtil.maskId(String.valueOf(current.getId()));

        log.debug("findByEmail email={} by user={}", normalized, current.getId());

        User user = repository.findByEmail(normalized)
                .orElseThrow(() -> {

                    log.warn("User not found by email={}", maskedEmail);

                    return new UserNotFoundException(normalized);
                });
        if ((current.getRole() == RoleType.USER || current.getRole() == RoleType.SITTER)
                && !Objects.equals(current.getEmail(), normalized)) {

            log.warn("User {} attempted to access another user's data {}", maskedCurrentId, maskedEmail);

            throw new AccessDeniedException("You do not have permission to view this user.");
        }

        log.info("User found by email={}", maskedEmail);

        return converter.entityToDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User getAuthenicatedCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {

            log.warn("getCurrentUser failed — no authenticated user");

            throw new SecurityException("User is not authenticated");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails details) {
            String email = validationUtil.normalizeEmail(details.getUsername());
            String maskedEmail = maskingUtil.maskEmail(email);

            log.debug("getCurrentUser principal email={}", maskedEmail);

            return repository.findByEmail(email)
                    .orElseThrow(() -> {

                        log.error("Authenticated user not found in DB email={}", maskedEmail);

                        return new UserNotFoundException(email);
                    });
        }

        log.error("Invalid authentication principal type={}", principal.getClass().getName());

        throw new IllegalStateException("Cannot obtain user from authentication principal");
    }
}