package lv.pawsitter.dto.emaildto;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the MailerSend email delivery service.
 * <p>
 * Values are loaded from the application configuration using the prefix
 * {@code mailersend}. This record provides the base URL of the MailerSend API
 * and the authentication token required for authorized requests.
 * <p>
 * These properties are typically used by {@code EmailWebClientImpl} to
 * initialize a {@link org.springframework.web.reactive.function.client.WebClient}
 * instance for sending emails.
 */
@ConfigurationProperties(prefix = "mailersend")
public record MailerSendProperties(

        /**
         * Base URL of the MailerSend API, e.g. {@code https://api.mailersend.com/v1}.
         */
        String baseUrl,

        /**
         * API authentication token used for Bearer authorization when sending requests.
         */
        String token
) {}
