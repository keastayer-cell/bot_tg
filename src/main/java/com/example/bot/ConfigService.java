package com.example.bot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

@Component
public class ConfigService {
    private Properties props = new Properties();

    @Value("${admin-chat-id:}")
    private String adminChatIdFromEnv;

    @Autowired
    private RecipientRepository recipientRepository;

    public void init() {
        if (adminChatIdFromEnv != null && !adminChatIdFromEnv.isEmpty()) {
            if (props.getProperty("admin-id") == null || props.getProperty("admin-id").isEmpty()) {
                props.setProperty("admin-id", adminChatIdFromEnv);
            }
        }
    }

    public String getAdminId() {
        return props.getProperty("admin-id");
    }

    public void setAdminId(String adminId) {
        props.setProperty("admin-id", adminId);
    }

    public String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public void set(String key, String value) {
        props.setProperty(key, value);
    }

    public void setTimezone(String timezone) {
        props.setProperty("timezone", timezone);
    }

    public List<String> getRecipients() {
        return recipientRepository.findAll().stream()
                .map(Recipient::getRecipientId)
                .collect(Collectors.toList());
    }

    public void addRecipient(String recipientId) {
        if (recipientRepository.findByRecipientId(recipientId).isEmpty()) {
            Recipient recipient = new Recipient();
            recipient.setRecipientId(recipientId);
            recipientRepository.save(recipient);
        }
    }

    public void removeRecipient(String recipientId) {
        Optional<Recipient> recipient = recipientRepository.findByRecipientId(recipientId);
        recipient.ifPresent(recipientRepository::delete);
    }

    public boolean isRecipient(String recipientId) {
        return recipientRepository.findByRecipientId(recipientId).isPresent();
    }

    public String getTimezone() {
        return "Europe/Moscow"; // Default
    }
}
