package com.example.bot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Properties;

@Component
public class ConfigService {
    private final String configFile = "config.properties";
    private Properties props = new Properties();

    @Value("${admin-chat-id:}")
    private String adminChatIdFromEnv;

    public ConfigService() {
        load();
    }

    public void load() {
        try (InputStream in = new FileInputStream(configFile)) {
            props.load(in);
        } catch (IOException e) {
            // Файла нет
        }
    }

    public void init() {
        if (adminChatIdFromEnv != null && !adminChatIdFromEnv.isEmpty()) {
            if (props.getProperty("admin-id") == null || props.getProperty("admin-id").isEmpty()) {
                props.setProperty("admin-id", adminChatIdFromEnv);
                save();
            }
        }
    }

    public void save() {
        try (OutputStream out = new FileOutputStream(configFile)) {
            props.store(out, "Bot Config");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public void set(String key, String value) {
        props.setProperty(key, value);
        save();
    }
}
