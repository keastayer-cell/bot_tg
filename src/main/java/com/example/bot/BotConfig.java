package com.example.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Configuration
public class BotConfig {

    private static final Logger log = LoggerFactory.getLogger(BotConfig.class);

    @Bean
    public TelegramBotsApi telegramBotsApi(MyTelegramBot bot) {
        try {
            TelegramBotsApi api = new TelegramBotsApi();
            api.registerBot(bot);
            return api;
        } catch (TelegramApiException e) {
            log.error("Ошибка регистрации бота: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
