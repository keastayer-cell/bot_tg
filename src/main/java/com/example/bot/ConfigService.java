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
                .map(Recipient::getUsername)
                .collect(Collectors.toList());
    }

    public void addRecipient(String username) {
        if (recipientRepository.findByUsername(username).isEmpty()) {
            Recipient recipient = new Recipient();
            recipient.setUsername(username);
            recipientRepository.save(recipient);
        }
    }

    public void removeRecipient(String username) {
        Optional<Recipient> recipient = recipientRepository.findByUsername(username);
        recipient.ifPresent(recipientRepository::delete);
    }

    public boolean isRecipient(String username) {
        return recipientRepository.findByUsername(username).isPresent();
    }

    public String getTimezone() {
        return "Europe/Moscow"; // Default
    }
}
