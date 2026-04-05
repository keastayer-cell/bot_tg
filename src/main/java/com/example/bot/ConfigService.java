package com.example.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

@Component
public class ConfigService {
    private static final Logger log = LoggerFactory.getLogger(ConfigService.class);
    
    private Properties props = new Properties();
    private Path configPath;

    @Value("${admin-chat-id:}")
    private String adminChatIdFromEnv;

    @Autowired
    RecipientRepository recipientRepository;

    public ConfigService() {
        // Определяем безопасную директорию для конфига
        String appDir = System.getProperty("app.dir", System.getProperty("user.dir"));
        configPath = Paths.get(appDir, "application.conf");
        log.info("Файл конфига будет сохраняться в: {}", configPath.toAbsolutePath());
        loadConfig();
    }

    private void loadConfig() {
        try {
            if (Files.exists(configPath)) {
                try (InputStream is = new FileInputStream(configPath.toFile())) {
                    props.load(is);
                    log.info("✓ Конфиг загружен из {}", configPath.toAbsolutePath());
                }
            } else {
                log.info("Файл конфига не существует, будет создан при сохранении");
            }
        } catch (IOException e) {
            log.warn("⚠ Ошибка загрузки config: {}", e.getMessage());
        }
    }

    private void saveConfig() {
        try {
            // Убеждаемся что директория существует
            Files.createDirectories(configPath.getParent());
            
            try (OutputStream os = new FileOutputStream(configPath.toFile())) {
                props.store(os, "Telegram Bot Config - Do not edit manually");
                log.debug("✓ Конфиг сохранён в {}", configPath.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("✗ Ошибка сохранения config: {}", e.getMessage(), e);
        }
    }

    public void init() {
        log.info("=== ConfigService.init() START ===");
        log.info("adminChatIdFromEnv={}", adminChatIdFromEnv);
        log.info("Текущий admin-id в конфиге: {}", props.getProperty("admin-id"));
        
        if (adminChatIdFromEnv != null && !adminChatIdFromEnv.isEmpty()) {
            if (props.getProperty("admin-id") == null || props.getProperty("admin-id").isEmpty()) {
                props.setProperty("admin-id", adminChatIdFromEnv);
                saveConfig();
                log.info("✓ Установлен admin-id из переменной окружения: {}", adminChatIdFromEnv);
            } else {
                log.info("✓ Admin-id уже установлен в конфиге: {}", props.getProperty("admin-id"));
            }
        } else {
            log.warn("⚠ adminChatIdFromEnv переменная пуста!");
        }
        
        log.info("=== ConfigService.init() DONE, admin-id now = {} ===", props.getProperty("admin-id"));
    }

    public String getAdminId() {
        return props.getProperty("admin-id", "");
    }

    public void setAdminId(String adminId) {
        props.setProperty("admin-id", adminId);
        saveConfig();
    }

    public String getTime() {
        return props.getProperty("time", "09:00");
    }

    public String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public void set(String key, String value) {
        props.setProperty(key, value);
        saveConfig();
    }

    public void setTimezone(String timezone) {
        props.setProperty("timezone", timezone);
        saveConfig();
    }

    public List<String> getRecipients() {
        try {
            if (recipientRepository == null) {
                log.warn("recipientRepository is null!");
                return List.of();
            }
            return recipientRepository.findAll().stream()
                    .map(Recipient::getRecipientId)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Ошибка получения получателей: {}", e.getMessage());
            return List.of();
        }
    }

    public void addRecipient(String recipientId) {
        try {
            if (recipientRepository == null) {
                log.warn("recipientRepository is null, cannot add recipient");
                return;
            }
            if (recipientRepository.findByRecipientId(recipientId).isEmpty()) {
                Recipient recipient = new Recipient();
                recipient.setRecipientId(recipientId);
                recipientRepository.save(recipient);
                log.info("Получатель добавлен: {}", recipientId);
            }
        } catch (Exception e) {
            log.error("Ошибка добавления получателя: {}", e.getMessage());
        }
    }

    public void removeRecipient(String recipientId) {
        try {
            if (recipientRepository == null) {
                log.warn("recipientRepository is null, cannot remove recipient");
                return;
            }
            Optional<Recipient> recipient = recipientRepository.findByRecipientId(recipientId);
            recipient.ifPresent(recipientRepository::delete);
        } catch (Exception e) {
            log.error("Ошибка удаления получателя: {}", e.getMessage());
        }
    }

    public boolean isRecipient(String recipientId) {
        try {
            if (recipientRepository == null) {
                log.warn("recipientRepository is null");
                return false;
            }
            return recipientRepository.findByRecipientId(recipientId).isPresent();
        } catch (Exception e) {
            log.error("Ошибка проверки получателя: {}", e.getMessage());
            return false;
        }
    }

    public String getTimezone() {
        return props.getProperty("timezone", "Europe/Moscow");
    }
}

