package com.example.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BotApplication {

    private static final Logger log = LoggerFactory.getLogger(BotApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(BotApplication.class, args);
    }

    @Bean
    public CommandLineRunner initConfig(ConfigService config) {
        return args -> {
            try {
                config.init();
                String timezone = config.getTimezone();
                System.setProperty("user.timezone", timezone);
                log.info("✓ Часовой пояс установлен: {}", timezone);
                log.info("✓ Конфиг инициализирован, admin-id = {}", config.getAdminId());
            } catch (Exception e) {
                log.error("✗ Ошибка инициализации конфига", e);
            }
        };
    }
}
