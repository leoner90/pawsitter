package lv.pawsitter.utility;

import org.springframework.stereotype.Component;

/**
 * Utility class for masking sensitive user information such as emails and logins.
 * Used primarily for secure logging to avoid exposing personal data.
 */
@Component
public class MaskingUtil {
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
     * Masks a login string by hiding all characters after the first three.
     *
     * @param login the login to mask
     * @return the masked login
     */
    public String maskLogin(String login) {
        if (login == null) return null;
        return login.length() <= 3 ? "***" : login.substring(0, 3) + "***";
    }
}