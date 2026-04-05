package com.example.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class BotConfig {

    private static final Logger log = LoggerFactory.getLogger(BotConfig.class);

    @Bean
    public TelegramBotsApi telegramBotsApi(MyTelegramBot bot) {
        try {
            log.warn("🔵 === REGISTERING BOT WITH TELEGRAM API ===");
            log.warn("Bot username: {}", bot.getBotUsername());
            log.warn("Bot token preview: {}...{}", 
                bot.getBotToken().substring(0, Math.min(5, bot.getBotToken().length())),
                bot.getBotToken().substring(Math.max(0, bot.getBotToken().length() - 5)));
            
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(bot);
            log.info("✓✓✓ БОТ УСПЕШНО ЗАРЕГИСТРИРОВАН И ГОТОВ ПОЛУЧАТЬ КОМАНДЫ ✓✓✓");
            return api;
        } catch (TelegramApiException e) {
            log.error("🔴🔴🔴 CRITICAL: ОШИБКА РЕГИСТРАЦИИ БОТА! 🔴🔴🔴");
            log.error("Error: {}", e.getMessage());
            log.error("Cause: {}", e.getCause());
            e.printStackTrace();
            throw new RuntimeException("Failed to register bot: " + e.getMessage());
        }
    }
}
