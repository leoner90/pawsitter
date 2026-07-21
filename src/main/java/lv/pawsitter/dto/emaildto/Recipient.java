package lv.pawsitter.dto.emaildto;

/**
 * Represents a single email recipient in an outbound email request.
 * <p>
 * This record defines the email address of the target recipient and is used
 * as part of the {@link EmailRequest} payload when submitting messages to
 * the external MailerSend API. It is serialized as JSON during HTTP transmission.
 */
public record Recipient(

        /**
         * Email address of the recipient who will receive the message.
         */
        String email
) {}
