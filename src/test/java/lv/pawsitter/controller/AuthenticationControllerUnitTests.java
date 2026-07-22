package lv.pawsitter.controller;

import lv.pawsitter.dto.userdto.UserCreateDTO;
import lv.pawsitter.dto.userdto.UserDTO;
import lv.pawsitter.exception.EmailNotUniqueException;
import lv.pawsitter.model.RoleType;
import lv.pawsitter.security.SecurityConfig;
import lv.pawsitter.security.sessionless.jwttoken.JwtService;
import lv.pawsitter.service.userservice.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthenticationController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
public class AuthenticationControllerUnitTests {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private SecurityContextRepository securityContextRepository;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void loginPage_returnsLoginView_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("authentication/login"));
    }

    @Test
    void loginPage_redirectsHome_whenAlreadyAuthenticated() throws Exception {
        mockMvc.perform(get("/login").with(user("jane@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void registrationPage_returnsRegistrationView_withEmptyDto_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/registration"))
                .andExpect(status().isOk())
                .andExpect(view().name("authentication/registration"))
                .andExpect(model().attributeExists("registrationRequest"));
    }

    @Test
    void registrationPage_redirectsHome_whenAlreadyAuthenticated() throws Exception {
        mockMvc.perform(get("/registration").with(user("jane@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void registerUser_returnsFormView_whenPasswordsDoNotMatch() throws Exception {
        mockMvc.perform(post("/registration")
                        .with(csrf())
                        .param("firstName", "Jane")
                        .param("lastName", "Doe")
                        .param("email", "jane@example.com")
                        .param("confirmEmail", "jane@example.com")
                        .param("password", "password123")
                        .param("confirmPassword", "different123")
                        .param("phoneNumber", "+37120000001")
                        .param("role", "USER"))
                .andExpect(status().isOk())
                .andExpect(view().name("authentication/registration"))
                .andExpect(model().attributeHasFieldErrors("registrationRequest", "confirmPassword"));

        verify(userService, never()).create(any());
    }

    @Test
    void registerUser_returnsFormView_whenValidationFails() throws Exception {
        mockMvc.perform(post("/registration")
                        .with(csrf())
                        .param("firstName", "")
                        .param("lastName", "")
                        .param("email", "not-an-email")
                        .param("confirmEmail", "")
                        .param("password", "")
                        .param("confirmPassword", "")
                        .param("phoneNumber", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("authentication/registration"));
    }

    @Test
    void registerUser_returnsFormWithFieldError_whenEmailNotUnique() throws Exception {
        when(userService.create(any(UserCreateDTO.class)))
                .thenThrow(new EmailNotUniqueException("Email already exists"));

        mockMvc.perform(post("/registration")
                        .with(csrf())
                        .param("firstName", "Jane")
                        .param("lastName", "Doe")
                        .param("email", "jane@example.com")
                        .param("confirmEmail", "jane@example.com")
                        .param("password", "password123")
                        .param("confirmPassword", "password123")
                        .param("phoneNumber", "+37120000001")
                        .param("role", "USER"))
                .andExpect(status().isOk())
                .andExpect(view().name("authentication/registration"))
                .andExpect(model().attributeHasFieldErrors("registrationRequest", "email"));
    }

    @Test
    void registerUser_redirectsToOwnerDashboard_whenSuccessfulAndNotSitter() throws Exception {
        when(userService.create(any(UserCreateDTO.class)))
                .thenReturn(new UserDTO(1L, "Jane", "Doe", "+37120000001", "jane@example.com",
                        RoleType.USER, LocalDateTime.now()));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new TestingAuthenticationToken("jane@example.com", "password123",
                        AuthorityUtils.createAuthorityList("USER")));

        mockMvc.perform(post("/registration")
                        .with(csrf())
                        .param("firstName", "Jane")
                        .param("lastName", "Doe")
                        .param("email", "jane@example.com")
                        .param("confirmEmail", "jane@example.com")
                        .param("password", "password123")
                        .param("confirmPassword", "password123")
                        .param("phoneNumber", "+37120000001")
                        .param("role", "USER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/owner/dashboard"));

        verify(securityContextRepository).saveContext(any(), any(), any());
    }

    @Test
    void registerUser_redirectsToSitterDashboard_whenSuccessfulAndIsSitter() throws Exception {
        when(userService.create(any(UserCreateDTO.class)))
                .thenReturn(new UserDTO(2L, "John", "Smith", "+37120000002", "john@example.com",
                        RoleType.SITTER, LocalDateTime.now()));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new TestingAuthenticationToken("john@example.com", "password123",
                        AuthorityUtils.createAuthorityList("SITTER")));

        mockMvc.perform(post("/registration")
                        .with(csrf())
                        .param("firstName", "John")
                        .param("lastName", "Smith")
                        .param("email", "john@example.com")
                        .param("confirmEmail", "john@example.com")
                        .param("password", "password123")
                        .param("confirmPassword", "password123")
                        .param("phoneNumber", "+37120000002")
                        .param("role", "SITTER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sitter/dashboard"));
    }
}
