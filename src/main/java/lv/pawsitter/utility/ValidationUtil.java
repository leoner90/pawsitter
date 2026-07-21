package lv.pawsitter.utility;

import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Utility class for validating and normalizing user input such as IDs,
 * email addresses and passwords. Intended for use in service-layer logic
 * to ensure consistent and safe preprocessing of incoming data.
 */
@Component
public class ValidationUtil {

    /**
     * Validates that the provided ID is positive.
     * Throws an IllegalArgumentException if the ID is zero or negative.
     *
     * @param id the ID value to validate
     * @throws IllegalArgumentException if the ID is not positive
     */
    public void validateId(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Id must be positive");
        }
    }

    /**
     * Normalizes an email address by trimming whitespace and converting
     * all characters to lowercase. Ensures the email is not null.
     *
     * @param email the email to normalize
     * @return normalized email string
     * @throws NullPointerException if the email is null
     */
    public String normalizeEmail(String email) {
        return Objects.requireNonNull(email, "email must not be null")
                .trim()
                .toLowerCase();
    }

    /**
     * Normalizes a password by trimming leading and trailing whitespace.
     * Ensures the password is not null.
     *
     * @param password the password to normalize
     * @return normalized password string
     * @throws NullPointerException if the password is null
     */
    public String normalizePassword(String password) {
        return Objects.requireNonNull(password, "password must not be null")
                .trim();
    }
}