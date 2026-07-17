package lv.pawsitter.service;

import lv.pawsitter.dto.UserCreateDTO;
import lv.pawsitter.dto.UserDTO;
import lv.pawsitter.entity.OwnerProfile;
import lv.pawsitter.entity.SitterProfile;
import lv.pawsitter.entity.User;
import lv.pawsitter.exception.EmailNotUniqueException;
import lv.pawsitter.exception.UserNotFoundException;
import lv.pawsitter.mapper.Converter;
import lv.pawsitter.model.RoleType;
import lv.pawsitter.repository.UserRepository;
import lv.pawsitter.utility.MaskingUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplementationUnitTests {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Converter<User, UserCreateDTO, UserDTO> converter;

    private MaskingUtil maskingUtil;

    private UserServiceImplementation userService;

    private User user;
    private UserCreateDTO userCreateDTO;

    @BeforeEach
    void setUp() {
        maskingUtil = new MaskingUtil();
        userService = new UserServiceImplementation(userRepository, passwordEncoder, converter, maskingUtil);

        user = new User();
        user.setId(1L);
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setEmail("jane@example.com");
        user.setPassword("encodedPassword");
        user.setPhoneNumber("+37120000001");
        user.setRole(RoleType.USER);
        user.setCreatedAt(LocalDateTime.now());

        userCreateDTO = new UserCreateDTO(
                "Jane",
                "Doe",
                "+37120000001",
                "jane@example.com",
                "jane@example.com",
                "password123",
                "password123",
                RoleType.USER
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setCurrentUser(User currentUser) {
        UserDetails principal = org.springframework.security.core.userdetails.User
                .withUsername(currentUser.getEmail())
                .password(currentUser.getPassword())
                .authorities(AuthorityUtils.createAuthorityList(currentUser.getRole().name()))
                .build();

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(userRepository.findByEmail(currentUser.getEmail())).thenReturn(Optional.of(currentUser));
    }

    private UserDTO toDto(User u) {
        return new UserDTO(u.getId(), u.getPhoneNumber(), u.getEmail(), u.getRole(), u.getCreatedAt());
    }



    @Test
    void create_savesAndReturnsDto_forOwnerRole() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.empty());
        when(converter.dtoToEntity(userCreateDTO)).thenReturn(new User());
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(1L);
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });
        when(converter.entityToDto(any(User.class))).thenAnswer(invocation -> toDto(invocation.getArgument(0)));

        UserDTO result = userService.create(userCreateDTO);

        assertThat(result.email()).isEqualTo("jane@example.com");
        assertThat(result.role()).isEqualTo(RoleType.USER);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void create_setsOwnerProfile_whenRoleIsUser() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(converter.dtoToEntity(userCreateDTO)).thenReturn(new User());
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(converter.entityToDto(any(User.class))).thenAnswer(invocation -> toDto(invocation.getArgument(0)));

        userService.create(userCreateDTO);

        verify(userRepository).save(argThat(u -> u.getOwnerProfile() != null && u.getSitterProfile() == null));
    }

    @Test
    void create_setsSitterProfile_whenRoleIsSitter() {
        UserCreateDTO sitterDto = new UserCreateDTO(
                "John", "Smith", "+37120000002",
                "john@example.com", "john@example.com",
                "password123", "password123", RoleType.SITTER);

        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(converter.dtoToEntity(sitterDto)).thenReturn(new User());
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(converter.entityToDto(any(User.class))).thenAnswer(invocation -> toDto(invocation.getArgument(0)));

        userService.create(sitterDto);

        verify(userRepository).save(argThat(u -> u.getSitterProfile() != null && u.getOwnerProfile() == null));
    }

    @Test
    void create_throwsNullPointerException_whenDtoIsNull() {
        assertThatThrownBy(() -> userService.create(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void create_throwsIllegalArgumentException_whenEmailsDoNotMatch() {
        UserCreateDTO mismatched = new UserCreateDTO(
                "Jane", "Doe", "+37120000001",
                "jane@example.com", "different@example.com",
                "password123", "password123", RoleType.USER);

        assertThatThrownBy(() -> userService.create(mismatched))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Emails do not match");

        verify(userRepository, never()).save(any());
    }

    @Test
    void create_throwsIllegalArgumentException_whenPasswordsDoNotMatch() {
        UserCreateDTO mismatched = new UserCreateDTO(
                "Jane", "Doe", "+37120000001",
                "jane@example.com", "jane@example.com",
                "password123", "otherPassword", RoleType.USER);

        assertThatThrownBy(() -> userService.create(mismatched))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Passwords do not match");

        verify(userRepository, never()).save(any());
    }

    @Test
    void create_throwsIllegalArgumentException_whenEmailFormatIsInvalid() {
        UserCreateDTO invalidEmail = new UserCreateDTO(
                "Jane", "Doe", "+37120000001",
                "not-an-email", "not-an-email",
                "password123", "password123", RoleType.USER);

        assertThatThrownBy(() -> userService.create(invalidEmail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email format");

        verify(userRepository, never()).save(any());
    }

    @Test
    void create_throwsEmailNotUniqueException_whenEmailAlreadyExists() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.create(userCreateDTO))
                .isInstanceOf(EmailNotUniqueException.class)
                .hasMessageContaining("jane@example.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    void create_throwsEmailNotUniqueException_whenSaveViolatesDataIntegrity() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.empty());
        when(converter.dtoToEntity(userCreateDTO)).thenReturn(new User());
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> userService.create(userCreateDTO))
                .isInstanceOf(EmailNotUniqueException.class);
    }



    @Test
    void findAll_returnsMappedDtos() {
        when(userRepository.findAll()).thenReturn(List.of(user));
        when(converter.entityToDto(user)).thenReturn(toDto(user));

        List<UserDTO> result = userService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).email()).isEqualTo("jane@example.com");
        verify(userRepository).findAll();
    }

    @Test
    void findAll_returnsEmptyList_whenNoUsersExist() {
        when(userRepository.findAll()).thenReturn(List.of());

        List<UserDTO> result = userService.findAll();

        assertThat(result).isEmpty();
    }



    @Test
    void findById_returnsDto_forExistingUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(converter.entityToDto(user)).thenReturn(toDto(user));

        UserDTO result = userService.findById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.email()).isEqualTo("jane@example.com");
    }

    @Test
    void findById_throwsUserNotFoundException_whenUserDoesNotExist() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(999L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void findById_throwsIllegalArgumentException_whenIdIsZeroOrNegative() {
        assertThatThrownBy(() -> userService.findById(0))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> userService.findById(-5))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRepository, never()).findById(any());
    }



    @Test
    void update_updatesRoleAndReturnsDto_forExistingUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(converter.entityToDto(any(User.class))).thenAnswer(invocation -> toDto(invocation.getArgument(0)));

        UserDTO result = userService.update(1L, RoleType.ADMIN);

        assertThat(result.role()).isEqualTo(RoleType.ADMIN);
        verify(userRepository).save(user);
    }

    @Test
    void update_throwsUserNotFoundException_whenUserDoesNotExist() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(999L, RoleType.ADMIN))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void update_throwsNullPointerException_whenRoleIsNull() {
        assertThatThrownBy(() -> userService.update(1L, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void update_throwsIllegalArgumentException_whenIdIsNotPositive() {
        assertThatThrownBy(() -> userService.update(0, RoleType.ADMIN))
                .isInstanceOf(IllegalArgumentException.class);
    }



    @Test
    void delete_deletesUser_whenSelfDeleting() {
        setCurrentUser(user);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.delete(1L);

        verify(userRepository).delete(user);
    }

    @Test
    void delete_throwsUserNotFoundException_whenUserDoesNotExist() {
        setCurrentUser(user);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.delete(999L))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository, never()).delete(any());
    }

    @Test
    void delete_throwsAccessDeniedException_whenUserTriesToDeleteAnotherUser() {
        User anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setEmail("john@example.com");
        anotherUser.setPassword("encoded");
        anotherUser.setRole(RoleType.USER);

        setCurrentUser(user);
        when(userRepository.findById(2L)).thenReturn(Optional.of(anotherUser));

        assertThatThrownBy(() -> userService.delete(2L))
                .isInstanceOf(AccessDeniedException.class);

        verify(userRepository, never()).delete(any());
    }

    @Test
    void delete_allowsAdmin_toDeleteAnotherUser() {
        User admin = new User();
        admin.setId(3L);
        admin.setEmail("admin@example.com");
        admin.setPassword("encoded");
        admin.setRole(RoleType.ADMIN);

        setCurrentUser(admin);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.delete(1L);

        verify(userRepository).delete(user);
    }

    @Test
    void delete_throwsIllegalArgumentException_whenIdIsNotPositive() {
        assertThatThrownBy(() -> userService.delete(0))
                .isInstanceOf(IllegalArgumentException.class);
    }



    @Test
    void findByEmail_returnsDto_whenSelfLookup() {
        setCurrentUser(user);
        when(converter.entityToDto(user)).thenReturn(toDto(user));

        UserDTO result = userService.findByEmail("jane@example.com");

        assertThat(result.email()).isEqualTo("jane@example.com");
    }

    @Test
    void findByEmail_throwsUserNotFoundException_whenEmailDoesNotExist() {
        setCurrentUser(user);
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByEmail("missing@example.com"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void findByEmail_throwsAccessDeniedException_whenUserLooksUpAnotherEmail() {
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("john@example.com");
        otherUser.setPassword("encoded");
        otherUser.setRole(RoleType.USER);

        setCurrentUser(user);
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(otherUser));

        assertThatThrownBy(() -> userService.findByEmail("john@example.com"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void findByEmail_throwsNullPointerException_whenEmailIsNull() {
        assertThatThrownBy(() -> userService.findByEmail(null))
                .isInstanceOf(NullPointerException.class);
    }



    @Test
    void getCurrentUser_returnsUser_whenAuthenticated() {
        setCurrentUser(user);

        User result = userService.getCurrentUser();

        assertThat(result.getEmail()).isEqualTo("jane@example.com");
    }

    @Test
    void getCurrentUser_throwsSecurityException_whenNoAuthentication() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> userService.getCurrentUser())
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void getCurrentUser_throwsSecurityException_whenAnonymous() {
        Authentication anonymous = new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
        SecurityContextHolder.getContext().setAuthentication(anonymous);

        assertThatThrownBy(() -> userService.getCurrentUser())
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void getCurrentUser_throwsUserNotFoundException_whenPrincipalNotInRepository() {
        UserDetails principal = org.springframework.security.core.userdetails.User
                .withUsername("ghost@example.com")
                .password("x")
                .authorities(AuthorityUtils.createAuthorityList("USER"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentUser())
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getCurrentUser_throwsIllegalStateException_whenPrincipalTypeIsInvalid() {
        Authentication authentication = new TestingAuthenticationToken("stringPrincipal", "credentials");
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThatThrownBy(() -> userService.getCurrentUser())
                .isInstanceOf(IllegalStateException.class);
    }
}
