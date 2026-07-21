package lv.pawsitter.dto.emaildto;

import java.util.List;

/**
 * Data transfer object representing an email message submitted to the external
 * MailerSend API. This record encapsulates all required fields for constructing
 * an outbound email request, including sender information, recipients, subject,
 * and plain‑text content.
 * <p>
 * Instances of this record are typically created by {@link lv.pawsitter.client.EmailWebClient}
 * implementations and serialized as JSON when performing HTTP POST requests to
 * the remote email delivery service.
 */
public record EmailRequest(

        /**
         * Sender information containing the email address and display name
         * used in the "From" field of the outgoing message.
         */
        From from,

        /**
         * List of recipients who will receive the email. Each recipient contains
         * an email address and optional name metadata.
         */
        List<Recipient> to,

        /**
         * Subject line of the email message.
         */
        String subject,

        /**
         * Plain‑text body of the email message.
         */
        String text
) {}
