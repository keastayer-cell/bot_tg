package com.example.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class MessageScheduler {
    private static final Logger log = LoggerFactory.getLogger(MessageScheduler.class);
    private final MyTelegramBot bot;

    @Value("${bot.scheduler.interval:60000}")
    private long schedulerInterval;

    public MessageScheduler(MyTelegramBot bot) {
        this.bot = bot;
    }

    @PostConstruct
    public void init() {
        log.info("Планировщик запущен с интервалом {} мс ({} секунд)",
            schedulerInterval, schedulerInterval / 1000);
    }

    @Scheduled(fixedRateString = "${bot.scheduler.interval:60000}")
    public void check() {
        bot.checkAndSend();
    }
}
