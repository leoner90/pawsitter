package lv.pawsitter.client;

import lombok.extern.slf4j.Slf4j;
import lv.pawsitter.dto.emaildto.MailerSendProperties;
import lv.pawsitter.dto.emaildto.EmailRequest;
import lv.pawsitter.dto.emaildto.From;
import lv.pawsitter.dto.emaildto.Recipient;
import lv.pawsitter.exception.ClientException;
import lv.pawsitter.utility.MaskingUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Component
@Slf4j
public class EmailWebClientImpl implements EmailWebClient {

    private final WebClient webClient;

    private final MaskingUtil maskingUtil;

    public EmailWebClientImpl(WebClient.Builder builder,
                              MailerSendProperties properties,
                              MaskingUtil maskingUtil) {
        this.webClient = builder
                .baseUrl(properties.baseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.token())
                .build();

        this.maskingUtil = maskingUtil;

        log.info("EmailWebClient initialized with baseUrl={}", properties.baseUrl());

    }

    @Override
    public Mono<String> sendEmail(String email, String subject, String text) {

        String maskedEmail = maskingUtil.maskEmail(email);

        log.info("Preparing to send email to {}", maskedEmail);

        log.debug("Email subject={}, bodyLength={}", subject, text.length());

        EmailRequest request = new EmailRequest(
                new From("noreply@test-86org8ej9pegew13.mlsender.net", "PawSitter"),
                List.of(new Recipient(email)),
                subject,
                text
        );

        return webClient.post()
                .uri("/email")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .doOnSubscribe(sub ->

                        log.info("Sending email request to external API for {}", maskedEmail))

                .doOnSuccess(response ->

                        log.info("Email successfully sent to {}", maskedEmail))

                .retryWhen(
                        Retry.backoff(3, Duration.ofSeconds(2))
                                .filter(ex -> {
                                    boolean retry = ex instanceof WebClientResponseException.TooManyRequests ||
                                            ex instanceof WebClientResponseException.ServiceUnavailable ||
                                            ex instanceof WebClientRequestException;
                                    if (retry) {

                                        log.warn("Retrying email send to {} due to transient error: {}", maskedEmail, ex.getMessage());

                                    } else {

                                        log.error("Non-retryable error sending email to {}: {}", maskedEmail, ex.getMessage());

                                    }
                                    return retry;
                                })
                )
                .onErrorResume(WebClientResponseException.class, ex -> {

                    log.error("External API returned error {} for {}", ex.getStatusCode(), maskedEmail);

                    return Mono.error(new ClientException("External API returned error: " + ex.getStatusCode(), ex));
                })
                .onErrorResume(WebClientRequestException.class, ex -> {

                    log.error("External API unreachable when sending email to {}: {}", maskedEmail, ex.getMessage());

                    return Mono.error(new ClientException("External API unreachable: " + ex.getMessage(), ex));
                });
    }
}