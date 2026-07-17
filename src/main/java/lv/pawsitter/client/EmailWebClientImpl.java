package lv.pawsitter.client;

import lombok.extern.slf4j.Slf4j;
import lv.pawsitter.configuration.MailerSendProperties;
import lv.pawsitter.exception.ClientException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

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
        Map<String, Object> body = Map.of(
                "from", Map.of(
                        "email", "noreply@test-86org8ej9pegew13.mlsender.net",
                        "name", "PawSitter"
                ),
                "to", List.of(
                        Map.of("email", email)
                ),
                "subject", subject,
                "text", text
        );

        return webClient.post()
                .uri("/email")
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
                .onErrorResume(WebClientResponseException.class, ex ->
                        Mono.error(new ClientException("External API returned error: " + ex.getStatusCode(), ex))
                )
                .onErrorResume(WebClientRequestException.class, ex ->
                        Mono.error(new ClientException("External API unreachable: " + ex.getMessage(), ex))
                );
    }
}