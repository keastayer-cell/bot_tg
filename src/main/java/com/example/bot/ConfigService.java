package com.example.bot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;

@Component
public class ConfigService {
    private final String configFile = "config.properties";
    private final String recipientsFile = "recipients.txt";
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

    // Работа с получателями
    public List<String> getRecipients() {
        List<String> list = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new FileReader(recipientsFile))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (!line.trim().isEmpty()) list.add(line.trim());
            }
        } catch (IOException e) {
            // Файла нет
        }
        return list;
    }

    public void addRecipient(String id) {
        List<String> list = getRecipients();
        if (!list.contains(id)) {
            list.add(id);
            saveRecipients(list);
        }
    }

    public void removeRecipient(String id) {
        List<String> list = getRecipients();
        list.remove(id);
        saveRecipients(list);
    }

    private void saveRecipients(List<String> list) {
        try (PrintWriter w = new PrintWriter(new FileWriter(recipientsFile))) {
            for (String s : list) w.println(s);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
