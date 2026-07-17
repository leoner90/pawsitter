package lv.pawsitter.client;

import reactor.core.publisher.Mono;

public interface EmailWebClient {
    Mono<String> sendEmail(String email, String subject, String text);
}
