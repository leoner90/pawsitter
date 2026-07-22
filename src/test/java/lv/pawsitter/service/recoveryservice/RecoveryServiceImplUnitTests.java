package lv.pawsitter.service.recoveryservice;
import lv.pawsitter.client.EmailWebClientImpl;
import lv.pawsitter.entity.Recovery;
import lv.pawsitter.entity.User;
import lv.pawsitter.exception.PasswordMismatchException;
import lv.pawsitter.exception.UserNotFoundException;
import lv.pawsitter.exception.recoveryexception.RecoveryExpiredException;
import lv.pawsitter.exception.recoveryexception.RecoveryNotFoundException;
import lv.pawsitter.repository.RecoveryRepository;
import lv.pawsitter.repository.UserRepository;
import lv.pawsitter.utility.MaskingUtil;
import lv.pawsitter.utility.ValidationUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class RecoveryServiceImplUnitTests {
    @Mock
    private RecoveryRepository recoveryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private EmailWebClientImpl webClient;

    private final MaskingUtil maskingUtil = new MaskingUtil();
    private final ValidationUtil validationUtil = new ValidationUtil();

    private RecoveryServiceImpl recoveryService;

    private User user;

    @BeforeEach
    void setUp() {
        recoveryService = new RecoveryServiceImpl(
                recoveryRepository, userRepository, encoder, webClient, maskingUtil, validationUtil
        );
        ReflectionTestUtils.setField(recoveryService, "frontendBaseUrl", "http://localhost:3400");

        user = new User();
        user.setId(1L);
        user.setEmail("jane@example.com");
        user.setFirstName("Jane");
    }

    @Test
    void generateAndEmail_createsNewRecovery_whenNoneExists() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(recoveryRepository.findByUser(user)).thenReturn(Optional.empty());
        when(encoder.encode(anyString())).thenReturn("hashedToken");
        when(webClient.sendEmail(anyString(), anyString(), anyString())).thenReturn(Mono.just("ok"));

        recoveryService.generateAndEmail("Jane@Example.com");

        verify(recoveryRepository).save(any(Recovery.class));
        verify(webClient).sendEmail(eq("jane@example.com"), anyString(), anyString());
    }

    @Test
    void generateAndEmail_reusesExistingRecovery_whenOneExists() {
        Recovery existing = new Recovery();
        existing.setUser(user);
        existing.setRecoveryToken("oldHash");

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(recoveryRepository.findByUser(user)).thenReturn(Optional.of(existing));
        when(encoder.encode(anyString())).thenReturn("newHash");
        when(webClient.sendEmail(anyString(), anyString(), anyString())).thenReturn(Mono.just("ok"));

        recoveryService.generateAndEmail("jane@example.com");

        verify(recoveryRepository).save(existing);
        assertThat(existing.getRecoveryToken()).isEqualTo("newHash");
    }

    @Test
    void generateAndEmail_throwsUserNotFoundException_whenEmailDoesNotExist() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recoveryService.generateAndEmail("missing@example.com"))
                .isInstanceOf(UserNotFoundException.class);

        verify(recoveryRepository, never()).save(any());
    }

    @Test
    void changePassword_updatesPassword_whenTokenValidAndPasswordsMatch() {
        Recovery recovery = new Recovery();
        recovery.setUser(user);
        recovery.setRecoveryToken("hashedToken");
        recovery.setEndOfLifeCycle(LocalDateTime.now().plusMinutes(10));

        when(recoveryRepository.findAll()).thenReturn(List.of(recovery));
        when(encoder.matches("rawToken", "hashedToken")).thenReturn(true);
        when(encoder.encode("newPass123")).thenReturn("encodedNewPass");

        recoveryService.changePassword("rawToken", "newPass123", "newPass123");

        verify(userRepository).save(user);
        assertThat(user.getPassword()).isEqualTo("encodedNewPass");
        verify(recoveryRepository).delete(recovery);
    }

    @Test
    void changePassword_throwsRecoveryNotFoundException_whenTokenDoesNotMatchAny() {
        when(recoveryRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> recoveryService.changePassword("badToken", "newPass123", "newPass123"))
                .isInstanceOf(RecoveryNotFoundException.class);
    }

    @Test
    void changePassword_throwsRecoveryExpiredException_whenTokenExpired() {
        Recovery recovery = new Recovery();
        recovery.setUser(user);
        recovery.setRecoveryToken("hashedToken");
        recovery.setEndOfLifeCycle(LocalDateTime.now().minusMinutes(1));

        when(recoveryRepository.findAll()).thenReturn(List.of(recovery));
        when(encoder.matches("rawToken", "hashedToken")).thenReturn(true);

        assertThatThrownBy(() -> recoveryService.changePassword("rawToken", "newPass123", "newPass123"))
                .isInstanceOf(RecoveryExpiredException.class);

        verify(recoveryRepository, never()).delete(any());
    }

    @Test
    void changePassword_throwsPasswordMismatchException_whenPasswordsDoNotMatch() {
        Recovery recovery = new Recovery();
        recovery.setUser(user);
        recovery.setRecoveryToken("hashedToken");
        recovery.setEndOfLifeCycle(LocalDateTime.now().plusMinutes(10));

        when(recoveryRepository.findAll()).thenReturn(List.of(recovery));
        when(encoder.matches("rawToken", "hashedToken")).thenReturn(true);

        assertThatThrownBy(() -> recoveryService.changePassword("rawToken", "newPass123", "different123"))
                .isInstanceOf(PasswordMismatchException.class);

        verify(userRepository, never()).save(any());
        verify(recoveryRepository, never()).delete(any());
    }

}
