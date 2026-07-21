package lv.pawsitter.client;

import reactor.core.publisher.Mono;

/**
 * Client interface for sending emails through an external email delivery API.
 * <p>
 * Provides a single asynchronous operation for submitting an email message
 * to the remote service. Implementations are expected to handle request
 * construction, error mapping, retry logic, and logging.
 * <p>
 * The method returns a {@link Mono} that completes when the external API
 * responds, either successfully or with an error. The caller may subscribe
 * to the returned {@code Mono} to trigger the actual HTTP request.
 */
public interface EmailWebClient {

    /**
     * Sends an email message to the specified recipient using the external email API.
     * <p>
     * The operation is asynchronous and does not block the calling thread.
     * Implementations may apply timeouts, retries, and error translation.
     *
     * @param email   the recipient's email address
     * @param subject the subject line of the email
     * @param text    the plain-text body of the email
     *
     * @return a {@link Mono} emitting the API response empty body on success,
     *         or an error signal if the request fails
     */
    Mono<String> sendEmail(String email, String subject, String text);
}

