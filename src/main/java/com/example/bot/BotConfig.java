package com.example.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.SetWebhook;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class BotConfig {

    private static final Logger log = LoggerFactory.getLogger(BotConfig.class);

    @Bean
    public CommandLineRunner registerBot(MyTelegramBot bot, 
            @Value("${railway.public-url:}") String railwayUrl) {
        return args -> {
            try {
                TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);

                // Если есть Railway URL - устанавливаем webhook
                if (railwayUrl != null && !railwayUrl.isEmpty()) {
                    SetWebhook webhook = SetWebhook.builder()
                        .url(railwayUrl + "/webhook")
                        .build();
                    bot.execute(webhook);
                    log.info("Webhook установлен: {}/webhook", railwayUrl);
                }

                // Регистрируем бота (запустится Long Polling, но webhook перехватит)
                api.registerBot(bot);
                log.info("Бот зарегистрирован");

            } catch (TelegramApiException e) {
                log.error("Ошибка регистрации бота: {}", e.getMessage());
                // Не вызываем исключение - пусть работает с webhook
            }
        };
    }
}
