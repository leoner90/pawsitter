package lv.pawsitter.dto;

import jakarta.validation.constraints.*;
import lv.pawsitter.model.RoleType;

/**
 * Data Transfer Object used for creating a new user account.
 * Contains all required registration fields such as personal information,
 * contact details, email confirmation, password confirmation, and role selection.
 *
 * <p>This DTO is used exclusively in user registration requests. All fields
 * are validated using Jakarta Bean Validation annotations to ensure that
 * incoming data meets formatting, length, and consistency requirements.</p>
 *
 * <p>The DTO does not contain any security‑sensitive fields such as encoded
 * passwords or internal identifiers. The raw password is validated here but
 * encoded later in the service layer before persistence.</p>
 */
public record UserCreateDTO(
                /**
                 * First name of the user.
                 * <p>Must contain only alphabetic characters and be between 2 and 50 characters long.</p>
                 */
                @NotBlank
                @Size(min = 2, max = 50)
                @Pattern(regexp = "^[A-Za-zÀ-ž\\s'-]+$", message = "Must contain only letters")
                String firstName,
                /**
                 * Last name of the user.
                 * <p>Must contain only alphabetic characters and be between 2 and 50 characters long.</p>
                 */
                @NotBlank
                @Size(min = 2, max = 50)
                @Pattern(regexp = "^[A-Za-zÀ-ž\\s'-]+$", message = "Must contain only letters")
                String lastName,
                /**
                 * Phone number provided during registration.
                 * <p>Must follow a valid international format (E.164-like), allowing optional leading '+'
                 * and containing 7 to 15 digits.</p>
                 */
                @NotBlank
                @Pattern(
                        regexp = "^\\+?[0-9]{7,15}$",
                        message = "Phone number must be valid international format"
                )
                String phoneNumber,
                /**
                 * Primary email address of the new user.
                 * <p>Must be a valid email format, cannot be blank, and must not exceed 100 characters.</p>
                 * <p>The email is normalized to lowercase before storage.</p>
                 */
                @NotBlank
                @Email
                @Size(max = 100)
                String email,
                /**
                 * Confirmation of the primary email address.
                 * <p>Must match the {@code email} field. Validation of equality is performed
                 * at the service or controller layer.</p>
                 */
                @NotBlank
                @Email
                @Size(max = 100)
                String confirmEmail,
                /**
                 * Raw password provided during registration.
                 * <p>Must be between 6 and 100 characters long. Additional password strength
                 * requirements (e.g., symbols, digits) may be enforced at the service layer.</p>
                 * <p>The password is encoded before being stored in the database.</p>
                 */
                @NotBlank
                @Size(min = 6, max = 100)
                String password,
                /**
                 * Confirmation of the raw password.
                 * <p>Must match the {@code password} field. Equality validation is performed
                 * outside the DTO.</p>
                 */
                @NotBlank
                @Size(min = 6, max = 100)
                String confirmPassword,
                /**
                 * Role assigned to the user during registration.
                 * <p>Defines the type of account being created (e.g., USER, SITTER, ADMIN).
                 * Must not be null.</p>
                 */
                @NotNull
                RoleType role
        ) {
}