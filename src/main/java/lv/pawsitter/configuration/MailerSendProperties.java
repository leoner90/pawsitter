package lv.pawsitter.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mailersend")
public record MailerSendProperties(String baseUrl, String token) {
}