package lv.pawsitter.service.recoveryservice;

/**
 * Service responsible for handling the full password recovery flow.
 * <p>
 * Provides functionality for:
 * <ul>
 *     <li>Generating a password recovery token and sending it to the user's email.</li>
 *     <li>Validating the recovery token and updating the user's password.</li>
 * </ul>
 * <p>
 * This service does not perform any HTTP or UI logic. It is used by
 * the MVC controller layer and throws domain-specific exceptions
 * handled by {@code @ControllerAdvice}.
 */
public interface RecoveryService {

    /**
     * Generates a password recovery token for the user with the given email
     * and sends a recovery link to that email address.
     * <p>
     * The method performs the following steps:
     * <ol>
     *     <li>Validates that a user with the provided email exists.</li>
     *     <li>Generates a recovery token (raw UUID).</li>
     *     <li>Hashes and stores the token together with an expiration timestamp.</li>
     *     <li>Sends an email containing a recovery link with the raw token.</li>
     * </ol>
     *
     * @param email the email address of the user requesting password recovery
     *
     * @throws lv.pawsitter.exception.UserNotFoundException
     *         if no user exists with the provided email
     *
     * @throws IllegalArgumentException
     *         if the email format is invalid
     */
    void generateAndEmail(String email);

    /**
     * Validates the provided recovery token and updates the user's password.
     * <p>
     * The method performs the following steps:
     * <ol>
     *     <li>Validates that the recovery token exists.</li>
     *     <li>Checks that the token has not expired.</li>
     *     <li>Compares the raw token with the stored hashed token.</li>
     *     <li>Validates that the new password and confirmation match.</li>
     *     <li>Updates the user's password and invalidates the recovery token.</li>
     * </ol>
     *
     * @param rawToken the raw recovery token extracted from the URL
     * @param newPassword the new password chosen by the user
     * @param confirmNewPassword confirmation of the new password
     *
     * @throws lv.pawsitter.exception.recoveryexception.RecoveryNotFoundException
     *         if the token does not exist or does not match any recovery entry
     *
     * @throws lv.pawsitter.exception.recoveryexception.RecoveryExpiredException
     *         if the token has expired
     *
     * @throws lv.pawsitter.exception.PasswordMismatchException
     *         if {@code newPassword} and {@code confirmNewPassword} do not match
     *
     * @throws IllegalArgumentException
     *         if any of the provided values are invalid
     */
    void changePassword(String rawToken, String newPassword, String confirmNewPassword);
}