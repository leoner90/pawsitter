package lv.pawsitter.service.userservice;

import lv.pawsitter.dto.userdto.UserCreateDTO;
import lv.pawsitter.dto.userdto.UserDTO;
import lv.pawsitter.entity.User;
import lv.pawsitter.model.RoleType;

import java.util.List;

/**
 * Service responsible for managing user accounts, including creation,
 * retrieval, update, deletion, and access to the currently authenticated user.
 *
 * <p>This service contains no HTTP or UI logic. It is intended to be used by
 * MVC controllers and throws domain‑specific exceptions handled by
 * {@code lv.pawsitter.advice.GlobalExceptionHandler}.</p>
 *
 * <p>All modifying operations are transactional. Read-only operations are
 * explicitly marked as such for performance and clarity.</p>
 */
public interface UserService {

    /**
     * Creates a new user account based on the provided DTO.
     *
     * <p>Processing steps:</p>
     * <ol>
     *     <li>Validates that the DTO is not null.</li>
     *     <li>Normalizes and validates the email and confirmation email.</li>
     *     <li>Ensures the email values match.</li>
     *     <li>Normalizes and validates the password and confirmation password.</li>
     *     <li>Ensures the password values match.</li>
     *     <li>Checks that the email is unique.</li>
     *     <li>Converts the DTO to an entity and assigns role-specific profiles.</li>
     *     <li>Encodes the password and persists the user.</li>
     * </ol>
     *
     * @param dto the DTO containing user creation data
     *
     * @return the created user as a DTO
     *
     * @throws IllegalArgumentException
     *         if the DTO or any required field is invalid
     *
     * @throws lv.pawsitter.exception.EmailMismatchException
     *         if email and confirmation email do not match
     *
     * @throws lv.pawsitter.exception.PasswordMismatchException
     *         if password and confirmation password do not match
     *
     * @throws lv.pawsitter.exception.EmailNotUniqueException
     *         if a user with the given email already exists
     */
    UserDTO create(UserCreateDTO dto);

    /**
     * Retrieves all users.
     *
     * <p>Accessible only to administrators.</p>
     *
     * @return list of all users as DTOs
     */
    List<UserDTO> findAll();

    /**
     * Retrieves a user by ID.
     *
     * <p>Accessible only to administrators.</p>
     *
     * @param id the user ID
     *
     * @return the user as a DTO
     *
     * @throws IllegalArgumentException
     *         if the ID is invalid
     *
     * @throws lv.pawsitter.exception.UserNotFoundException
     *         if no user exists with the given ID
     */
    UserDTO findById(long id);

    /**
     * Updates the role of a user.
     *
     * <p>Accessible only to administrators.</p>
     *
     * @param id the user ID
     * @param newRole the new role to assign
     *
     * @return the updated user as a DTO
     *
     * @throws IllegalArgumentException
     *         if the ID or role is invalid
     *
     * @throws lv.pawsitter.exception.UserNotFoundException
     *         if no user exists with the given ID
     */
    UserDTO update(long id, RoleType newRole);

    /**
     * Deletes a user.
     *
     * <p>Accessible to:</p>
     * <ul>
     *     <li>ADMIN — may delete any user</li>
     *     <li>USER/SITTER — may delete only their own account</li>
     * </ul>
     *
     * @param id the user ID
     *
     * @throws IllegalArgumentException
     *         if the ID is invalid
     *
     * @throws lv.pawsitter.exception.UserNotFoundException
     *         if no user exists with the given ID
     *
     * @throws org.springframework.security.access.AccessDeniedException
     *         if a non-admin attempts to delete another user's account
     */
    void delete(long id);

    /**
     * Retrieves a user by email.
     *
     * <p>Access rules:</p>
     * <ul>
     *     <li>ADMIN — may access any user</li>
     *     <li>USER/SITTER — may access only their own data</li>
     * </ul>
     *
     * @param email the email to search for
     *
     * @return the user as a DTO
     *
     * @throws lv.pawsitter.exception.UserNotFoundException
     *         if no user exists with the given email
     *
     * @throws org.springframework.security.access.AccessDeniedException
     *         if a non-admin attempts to access another user's data
     */
    UserDTO findByEmail(String email);

    /**
     * Returns the currently authenticated user entity.
     *
     * <p>Used internally by service methods that require identity checks.</p>
     *
     * @return the authenticated user entity
     *
     * @throws SecurityException
     *         if no authenticated user exists
     *
     * @throws lv.pawsitter.exception.UserNotFoundException
     *         if the authenticated user's email does not exist in the database
     *
     * @throws IllegalStateException
     *         if the authentication principal is of an unexpected type
     */
    User getAuthenicatedCurrentUser();
}
