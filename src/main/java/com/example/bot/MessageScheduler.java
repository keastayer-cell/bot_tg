package com.example.bot;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MessageScheduler {
    private final MyTelegramBot bot;
    public MessageScheduler(MyTelegramBot bot) { this.bot = bot; }

    @Scheduled(fixedRate = 60000)
    public void check() { bot.checkAndSend(); }
}
