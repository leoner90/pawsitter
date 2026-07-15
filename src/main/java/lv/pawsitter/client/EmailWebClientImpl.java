package lv.pawsitter.client;

import lombok.extern.slf4j.Slf4j;
import lv.pawsitter.configuration.MailerSendProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Component
@Slf4j
public class EmailWebClientImpl implements EmailWebClient {

    private final WebClient webClient;

    public EmailWebClientImpl(WebClient.Builder builder, MailerSendProperties properties) {
        this.webClient = builder
                .baseUrl(properties.baseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.token())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public Mono<String> sendEmail(String email, String subject, String text) {
        String body = String.format("""
        {
          "from": {
            "email": "noreply@test-86org8ej9pegew13.mlsender.net"
          },
          "to": [
            { "email": "%s" }
          ],
          "subject": "%s",
          "text": "%s"
        }
        """, email, subject, text);

        return webClient.post()
                .uri("/email")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .retryWhen(
                        Retry.backoff(3, Duration.ofSeconds(2))
                                .filter(ex -> ex instanceof WebClientResponseException.TooManyRequests ||
                                        ex instanceof WebClientResponseException.ServiceUnavailable ||
                                        ex instanceof WebClientRequestException)
                )
                .onErrorResume(ex -> {
                    log.error("Email sending failed: {}", ex.getMessage());
                    return Mono.error(new RuntimeException("Email sending failed: " + ex.getMessage(), ex));
                });
    }
}