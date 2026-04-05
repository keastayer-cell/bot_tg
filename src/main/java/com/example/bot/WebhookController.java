package com.example.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.objects.Update;

@RestController
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);
    private final MyTelegramBot bot;

    public WebhookController(MyTelegramBot bot) {
        this.bot = bot;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> onWebhookUpdate(@RequestBody Update update) {
        try {
            bot.onUpdateReceived(update);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Ошибка webhook: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
