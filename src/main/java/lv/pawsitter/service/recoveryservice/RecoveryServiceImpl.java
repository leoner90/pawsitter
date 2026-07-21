package lv.pawsitter.service.recoveryservice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lv.pawsitter.client.EmailWebClientImpl;
import lv.pawsitter.entity.Recovery;
import lv.pawsitter.entity.User;
import lv.pawsitter.exception.PasswordMismatchException;
import lv.pawsitter.exception.recoveryexception.RecoveryExpiredException;
import lv.pawsitter.exception.recoveryexception.RecoveryNotFoundException;
import lv.pawsitter.exception.UserNotFoundException;
import lv.pawsitter.repository.RecoveryRepository;

import lv.pawsitter.repository.UserRepository;
import lv.pawsitter.utility.MaskingUtil;
import lv.pawsitter.utility.ValidationUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecoveryServiceImpl implements RecoveryService {
    private final RecoveryRepository repository;

    private final UserRepository userRepository;

    private final PasswordEncoder encoder;

    private final EmailWebClientImpl webClient;

    private final MaskingUtil maskingUtil;

    private final ValidationUtil validationUtil;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Override
    public void generateAndEmail(String email) {
        log.info("Starting password recovery process for email={}", maskingUtil.maskEmail(email));

        String normalized = validationUtil.normalizeEmail(email);
        String maskedEmail = maskingUtil.maskEmail(normalized);

        log.debug("Normalized email={}", maskedEmail);

        User user = userRepository.findByEmail(normalized)
                .orElseThrow(() -> {

                    log.warn("User not found by email={}", maskedEmail);

                    return new UserNotFoundException("User with email " + normalized + " is not found.");
                });
        String maskedId = maskingUtil.maskId(String.valueOf(user.getId()));

        log.info("User found: id={}, email={}", maskedId, maskedEmail);

        log.debug("Deleting old recovery tokens for user id={}", maskedId);

        repository.deleteByUser(user);
        Recovery recovery = new Recovery();
        String rawToken = UUID.randomUUID().toString();
        String hashedToken = encoder.encode(rawToken);

        log.debug("Generated new recovery token for user id={}", maskedId);

        recovery.setRecoveryToken(hashedToken);
        recovery.setUser(user);
        repository.save(recovery);

        log.info("Saved new recovery token for user id={}", maskedId);

        log.info("Sending recovery email to {}", maskedEmail);

        webClient.sendEmail(user.getEmail(), "PawSitter Recovery Email",
                "Follow this link to change your password:\n" + frontendBaseUrl
                        + "/recovery/updatePassword?recoveryToken=" + rawToken);

        log.info("Recovery email successfully sent to {}", maskedEmail);
    }

    @Override
    @Transactional
    public void changePassword(String rawToken, String newPassword, String confirmNewPassword) {
        String maskedToken = maskingUtil.maskToken(rawToken);
        log.info("Starting password change using recovery token={}", maskedToken);

        List<Recovery> recoveries = repository.findAll();

        log.debug("Loaded {} recovery entries from database", recoveries.size());
        Recovery recovery = recoveries.stream()
                .filter(r -> encoder.matches(rawToken, r.getRecoveryToken()))
                .findFirst()
                .orElseThrow(() -> {

                    log.warn("Recovery token {} not found in database", maskedToken);

                    return new RecoveryNotFoundException("Recovery with token " + rawToken + " not found");
                });

        log.info("Recovery token matched for user id={}",
                maskingUtil.maskId(String.valueOf(recovery.getUser().getId())));

        if (recovery.getEndOfLifeCycle().isBefore(LocalDateTime.now())) {

            log.warn("Recovery token {} expired at {}", maskedToken, recovery.getEndOfLifeCycle());

            throw new RecoveryExpiredException("Recovery token " + rawToken + " has expired");
        }
        User user = recovery.getUser();
        String maskedId = maskingUtil.maskId(String.valueOf(user.getId()));

        log.debug("Changing password for user id={}", maskedId);

        String password = validationUtil.normalizePassword(newPassword);
        String maskedPassword = maskingUtil.maskPassword(password);
        String confirmPassword = validationUtil.normalizePassword(confirmNewPassword);
        String maskedConfirmPassword = maskingUtil.maskPassword(confirmPassword);

        log.debug("Checking password match: {} vs {}", maskedPassword, maskedConfirmPassword);

        if (!Objects.equals(password, confirmPassword)) {

            log.warn("Password mismatch for user id={}: {} vs {}", maskedId, maskedPassword, maskedConfirmPassword);

            throw new PasswordMismatchException("Passwords do not match");
        }
        user.setPassword(encoder.encode(password));
        userRepository.save(user);

        log.info("Password successfully updated for user id={}", maskedId);

        repository.delete(recovery);
        log.debug("Deleted used recovery token for user id={}", maskedId);
    }
}