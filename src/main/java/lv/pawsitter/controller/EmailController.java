package lv.pawsitter.controller;

import lv.pawsitter.client.EmailWebClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class EmailController {

    private final EmailWebClient emailWebClient;

    public EmailController(EmailWebClient emailWebClient) {
        this.emailWebClient = emailWebClient;
    }

    @GetMapping("/email/test")
    public Mono<String> sendTestEmail() {
        return emailWebClient.sendEmail("klaatu", "barada", "nikto");
    }
//    This test domain can only be used with 2 unique emails. Once they are registered, can not be changed.
//    100 emails only for testing purposes. Here is an example https://developers.mailersend.com/api/v1/email
//    {
//        "from": {
//        "email": "hello@mailersend.com",
//                "name": "MailerSend"
//    },
//        "to": [
//        {
//            "email": "john@mailersend.com",
//                "name": "John Mailer"
//        }
//    ],
//        "subject": "Hello from {{company}}!",
//            "text": "This is just a friendly hello from your friends at {{company}}.",
//            "html": "<b>This is just a friendly hello from your friends at {{company}}.</b>",
//            "personalization": [
//        {
//            "email": "john@mailersend.com",
//                "data": {
//            "company": "MailerSend"
//        }
//        }
//    ]
//    }
}
