package lv.pawsitter.controller;

import lv.pawsitter.exception.PasswordMismatchException;
import lv.pawsitter.exception.UserNotFoundException;
import lv.pawsitter.exception.recoveryexception.RecoveryExpiredException;
import lv.pawsitter.exception.recoveryexception.RecoveryNotFoundException;
import lv.pawsitter.service.recoveryservice.RecoveryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RecoveryController.class)
@AutoConfigureMockMvc(addFilters = false)
public class RecoveryControllerUnitTests {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecoveryService recoveryService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void recoveryPage_returnsRecoveryTokenView() throws Exception {
        mockMvc.perform(get("/recovery"))
                .andExpect(status().isOk())
                .andExpect(view().name("recovery/getRecoveryToken"));
    }

    @Test
    void sendRecoveryEmail_returnsEmailSentView_whenSuccessful() throws Exception {
        doNothing().when(recoveryService).generateAndEmail("jane@example.com");

        mockMvc.perform(post("/recovery").with(csrf()).param("email", "jane@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("recovery/emailSent"));
    }

    @Test
    void sendRecoveryEmail_returnsFormWithError_whenUserNotFound() throws Exception {
        doThrow(new UserNotFoundException("not found")).when(recoveryService).generateAndEmail("missing@example.com");

        mockMvc.perform(post("/recovery").with(csrf()).param("email", "missing@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("recovery/getRecoveryToken"))
                .andExpect(model().attributeExists("recoveryError"));
    }

    @Test
    void updatePasswordPage_returnsUpdatePasswordView_withTokenAndDto() throws Exception {
        mockMvc.perform(get("/recovery/updatePassword").param("recoveryToken", "abc123"))
                .andExpect(status().isOk())
                .andExpect(view().name("recovery/updatePassword"))
                .andExpect(model().attribute("recoveryToken", "abc123"))
                .andExpect(model().attributeExists("recoveryRequestDTO"));
    }

    @Test
    void updatePassword_redirectsToLogin_whenSuccessful() throws Exception {
        doNothing().when(recoveryService).changePassword(anyString(), anyString(), anyString());

        mockMvc.perform(post("/recovery/updatePassword")
                        .with(csrf())
                        .param("recoveryToken", "abc123")
                        .param("newPassword", "newPass123")
                        .param("confirmNewPassword", "newPass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/authentication/login"));
    }

    @Test
    void updatePassword_returnsFormView_whenValidationFails() throws Exception {
        mockMvc.perform(post("/recovery/updatePassword")
                        .with(csrf())
                        .param("recoveryToken", "abc123")
                        .param("newPassword", "")
                        .param("confirmNewPassword", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("recovery/updatePassword"));

        verify(recoveryService, org.mockito.Mockito.never()).changePassword(anyString(), anyString(), anyString());
    }

    @Test
    void updatePassword_returnsFormWithFieldError_whenPasswordsMismatch() throws Exception {
        doThrow(new PasswordMismatchException("Passwords do not match"))
                .when(recoveryService).changePassword(anyString(), anyString(), anyString());

        mockMvc.perform(post("/recovery/updatePassword")
                        .with(csrf())
                        .param("recoveryToken", "abc123")
                        .param("newPassword", "newPass123")
                        .param("confirmNewPassword", "different123"))
                .andExpect(status().isOk())
                .andExpect(view().name("recovery/updatePassword"))
                .andExpect(model().attributeHasFieldErrors("recoveryRequestDTO", "confirmNewPassword"));
    }

    @Test
    void updatePassword_returnsFormWithError_whenTokenExpired() throws Exception {
        doThrow(new RecoveryExpiredException("expired"))
                .when(recoveryService).changePassword(anyString(), anyString(), anyString());

        mockMvc.perform(post("/recovery/updatePassword")
                        .with(csrf())
                        .param("recoveryToken", "abc123")
                        .param("newPassword", "newPass123")
                        .param("confirmNewPassword", "newPass123"))
                .andExpect(status().isOk())
                .andExpect(view().name("recovery/updatePassword"))
                .andExpect(model().attributeExists("recoveryError"));
    }

    @Test
    void updatePassword_returnsFormWithError_whenTokenNotFound() throws Exception {
        doThrow(new RecoveryNotFoundException("not found"))
                .when(recoveryService).changePassword(anyString(), anyString(), anyString());

        mockMvc.perform(post("/recovery/updatePassword")
                        .with(csrf())
                        .param("recoveryToken", "abc123")
                        .param("newPassword", "newPass123")
                        .param("confirmNewPassword", "newPass123"))
                .andExpect(status().isOk())
                .andExpect(view().name("recovery/updatePassword"))
                .andExpect(model().attributeExists("recoveryError"));
    }
}
