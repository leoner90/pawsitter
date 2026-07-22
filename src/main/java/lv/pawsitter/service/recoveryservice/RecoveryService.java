package lv.pawsitter.service.recoveryservice;

/**
 * Service responsible for handling the full password recovery flow.
 *
 * <p>This service provides two main operations:</p>
 * <ul>
 *     <li>Generating a password recovery token and emailing a recovery link to the user.</li>
 *     <li>Validating a recovery token and updating the user's password.</li>
 * </ul>
 *
 * <p>This service contains no HTTP or UI logic. It is intended to be used by
 * MVC controllers and throws domain‑specific exceptions handled by
 * {@code lv.pawsitter.advice.RecoveryExceptionHandler}.</p>
 */
public interface RecoveryService {

    /**
     * Generates a password recovery token for the user with the given email
     * and sends a recovery link to that email address.
     *
     * <p>Processing steps:</p>
     * <ol>
     *     <li>Normalizes and validates the email format.</li>
     *     <li>Ensures a user with the provided email exists.</li>
     *     <li>Generates a raw UUID token and stores its hashed form.</li>
     *     <li>Sets a 15‑minute expiration time for the token.</li>
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
     *
     * <p>Processing steps:</p>
     * <ol>
     *     <li>Locates a recovery entry whose hashed token matches the raw token.</li>
     *     <li>Checks that the token has not expired.</li>
     *     <li>Normalizes and validates the new password values.</li>
     *     <li>Ensures the new password and confirmation match.</li>
     *     <li>Updates the user's password and deletes the used recovery entry.</li>
     * </ol>
     *
     * @param rawToken the raw recovery token extracted from the URL
     * @param newPassword the new password chosen by the user
     * @param confirmNewPassword confirmation of the new password
     *
     * @throws lv.pawsitter.exception.recoveryexception.RecoveryNotFoundException
     *         if the token does not match any stored recovery entry
     *
     * @throws lv.pawsitter.exception.recoveryexception.RecoveryExpiredException
     *         if the token has expired
     *
     * @throws lv.pawsitter.exception.PasswordMismatchException
     *         if {@code newPassword} and {@code confirmNewPassword} do not match
     *
     * @throws IllegalArgumentException
     *         if any provided values are invalid
     */
    void changePassword(String rawToken, String newPassword, String confirmNewPassword);
}