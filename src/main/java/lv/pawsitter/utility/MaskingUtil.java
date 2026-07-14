package lv.pawsitter.utility;

import org.springframework.stereotype.Component;

/**
 * Utility class for masking sensitive user information such as emails and passwords.
 * Used primarily for secure logging to avoid exposing personal data.
 */
@Component
public class MaskingUtil {
    /**
     * Masks an ID by keeping the first 3 and last 2 characters.
     * If ID is too short, masks fully.
     *
     * @param id the ID to mask
     * @return masked ID
     */
    public String maskId(String id) {
        if (id == null) return null;
        if (id.length() <= 5) return "***";

        String start = id.substring(0, 3);
        String end = id.substring(id.length() - 2);

        return start + "***" + end;
    }

    /**
     * Masks an email address by hiding characters between the first letter and the '@' symbol.
     *
     * @param email the email to mask
     * @return the masked email, or "****" if the email is too short
     */
    public String maskEmail(String email) {
        if (email == null) return null;
        int at = email.indexOf('@');
        if (at <= 1) return "****";
        return email.substring(0, 1) + "****" + email.substring(at);
    }

    /**
     * Masks a password string by hiding all characters after the first three.
     *
     * @param password the password to mask
     * @return the masked password
     */
    public String maskPassword(String password) {
        if (password == null) return null;
        return password.length() <= 3 ? "***" : password.substring(0, 3) + "***";
    }
}