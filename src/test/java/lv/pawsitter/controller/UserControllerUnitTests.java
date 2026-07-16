package lv.pawsitter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lv.pawsitter.dto.UserCreateDTO;
import lv.pawsitter.dto.UserDTO;
import lv.pawsitter.exception.EmailNotUniqueException;
import lv.pawsitter.exception.UserNotFoundException;
import lv.pawsitter.model.RoleType;
import lv.pawsitter.security.AuthenticationService;
import lv.pawsitter.security.JwtAuthenticationResponse;
import lv.pawsitter.security.JwtService;
import lv.pawsitter.security.SignInRequest;
import lv.pawsitter.service.UserService;
import lv.pawsitter.utility.MaskingUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)

public class UserControllerUnitTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private MaskingUtil maskingUtil;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private UserDTO buildDto(long id) {
        return new UserDTO(id, "+37120000001", "jane@example.com", RoleType.USER, LocalDateTime.now());
    }

    private UserCreateDTO buildCreateDto() {
        return new UserCreateDTO(
                "Jane", "Doe", "+37120000001",
                "jane@example.com", "jane@example.com",
                "password123", "password123", RoleType.USER);
    }


    private void stubMasking() {
        when(maskingUtil.maskEmail(anyString())).thenReturn("a****@example.com");
        when(maskingUtil.maskId(anyString())).thenReturn("***");
        when(maskingUtil.maskPassword(anyString())).thenReturn("pas***");
    }


    @Test
    @WithMockUser
    void registerUser_returnsCreated_whenValid() throws Exception {
        stubMasking();
        when(userService.create(any(UserCreateDTO.class))).thenReturn(buildDto(1L));

        mockMvc.perform(post("/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(buildCreateDto())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("jane@example.com"));
    }

    @Test
    @WithMockUser
    void registerUser_returnsConflict_whenEmailAlreadyExists() throws Exception {
        stubMasking();
        when(userService.create(any(UserCreateDTO.class)))
                .thenThrow(new EmailNotUniqueException("User with email jane@example.com already exists."));

        mockMvc.perform(post("/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(buildCreateDto())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("User with email jane@example.com already exists."));
    }

    @Test
    @WithMockUser
    void registerUser_returnsBadRequest_whenValidationFails() throws Exception {
        UserCreateDTO invalidDto = new UserCreateDTO(
                "", "", "", "not-an-email", "not-an-email", "123", "123", null);

        mockMvc.perform(post("/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));

        verify(userService, never()).create(any());
    }



    @Test
    @WithMockUser(authorities = "ADMIN")
    void getAllUsers_returnsOkWithList() throws Exception {
        when(userService.findAll()).thenReturn(List.of(buildDto(1L), buildDto(2L)));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)));
    }

    @Test
    @WithMockUser
    void getAllUsers_returnsEmptyList_whenNoUsers() throws Exception {
        when(userService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
    }



    @Test
    @WithMockUser
    void getUserById_returnsOk_whenUserExists() throws Exception {
        stubMasking();
        when(userService.findById(1L)).thenReturn(buildDto(1L));

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser
    void getUserById_returnsNotFound_whenUserDoesNotExist() throws Exception {
        stubMasking();
        when(userService.findById(999L)).thenThrow(new UserNotFoundException("User with id 999 is not found."));

        mockMvc.perform(get("/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User with id 999 is not found."));
    }

    @Test
    @WithMockUser
    void getUserById_returnsBadRequest_whenIdIsNotPositive() throws Exception {
        mockMvc.perform(get("/users/0"))
                .andExpect(status().isBadRequest());

        verify(userService, never()).findById(anyLong());
    }



    @Test
    @WithMockUser(authorities = "ADMIN")
    void setUserRole_returnsOk_whenSuccessful() throws Exception {
        stubMasking();
        UserDTO updated = new UserDTO(1L, "+37120000001", "jane@example.com", RoleType.ADMIN, LocalDateTime.now());
        when(userService.update(eq(1L), eq(RoleType.ADMIN))).thenReturn(updated);

        mockMvc.perform(patch("/users/1/role").param("newRole", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void setUserRole_returnsNotFound_whenUserDoesNotExist() throws Exception {
        stubMasking();
        when(userService.update(eq(999L), any(RoleType.class)))
                .thenThrow(new UserNotFoundException("User with id 999 is not found."));

        mockMvc.perform(patch("/users/999/role").param("newRole", "ADMIN"))
                .andExpect(status().isNotFound());
    }

 

    @Test
    @WithMockUser
    void removeUserById_returnsNoContent_whenSuccessful() throws Exception {
        stubMasking();

        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isNoContent());

        verify(userService).delete(1L);
    }

    @Test
    @WithMockUser
    void removeUserById_returnsNotFound_whenUserDoesNotExist() throws Exception {
        stubMasking();
        doThrow(new UserNotFoundException("User with id 999 is not found."))
                .when(userService).delete(999L);

        mockMvc.perform(delete("/users/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void removeUserById_returnsForbidden_whenAccessDenied() throws Exception {
        stubMasking();
        doThrow(new AccessDeniedException("You do not have permission to delete another user."))
                .when(userService).delete(2L);

        mockMvc.perform(delete("/users/2"))
                .andExpect(status().isForbidden());
    }

    

    @Test
    @WithMockUser
    void getUserByEmail_returnsOk_whenFound() throws Exception {
        stubMasking();
        when(userService.findByEmail("jane@example.com")).thenReturn(buildDto(1L));

        mockMvc.perform(get("/users/by-email").param("email", "jane@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jane@example.com"));
    }

    @Test
    @WithMockUser
    void getUserByEmail_returnsNotFound_whenEmailDoesNotExist() throws Exception {
        stubMasking();
        when(userService.findByEmail("missing@example.com"))
                .thenThrow(new UserNotFoundException("User with email missing@example.com is not found."));

        mockMvc.perform(get("/users/by-email").param("email", "missing@example.com"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void getUserByEmail_returnsBadRequest_whenEmailIsInvalid() throws Exception {
        mockMvc.perform(get("/users/by-email").param("email", "not-an-email"))
                .andExpect(status().isBadRequest());

        verify(userService, never()).findByEmail(anyString());
    }

    

    @Test
    @WithMockUser
    void login_returnsToken_whenCredentialsAreValid() throws Exception {
        stubMasking();
        SignInRequest signInRequest = new SignInRequest("jane@example.com", "password123");
        when(authenticationService.authenticate(any(SignInRequest.class)))
                .thenReturn(new JwtAuthenticationResponse("jwt-token-value"));

        mockMvc.perform(post("/users/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(signInRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-value"));
    }

    @Test
    @WithMockUser
    void login_returnsBadRequest_whenRequestIsInvalid() throws Exception {
        SignInRequest invalidRequest = new SignInRequest("", "");

        mockMvc.perform(post("/users/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).authenticate(any());
    }
    
    
}
