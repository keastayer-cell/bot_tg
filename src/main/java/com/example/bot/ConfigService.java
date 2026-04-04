package com.example.bot;

import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Properties;

@Component
public class ConfigService {
    private final String configFile = "config.properties";
    private Properties props = new Properties();

    public ConfigService() {
        load();
    }

    public void load() {
        try (InputStream in = new FileInputStream(configFile)) {
            props.load(in);
        } catch (IOException e) {
            // Файла нет, используем значения по умолчанию
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
