package lv.pawsitter.dto.emaildto;

/**
 * Represents the sender information used in an outbound email message.
 * <p>
 * This record defines the email address and the display name that appear
 * in the "From" field when submitting an email request to the external
 * MailerSend API. It is part of the {@link EmailRequest} payload and is
 * serialized as JSON during HTTP transmission.
 */
public record From(

        /**
         * Sender's email address used in the "From" header of the email.
         */
        String email,

        /**
         * Human‑readable display name shown to the recipient.
         */
        String name
) {}
