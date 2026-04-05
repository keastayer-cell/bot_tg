package com.example.bot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;

@Component
public class ConfigService {
    protected String configFile = "/app/data/config.properties";
    protected String recipientsFile = "/app/data/recipients.txt";
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
        saveProperties();
    }

    public String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public void set(String key, String value) {
        props.setProperty(key, value);
        saveProperties();
    }

    private synchronized void saveProperties() {
        try (PrintWriter w = new PrintWriter(new FileWriter(configFile))) {
            w.println("#Bot Config");
            for (String k : props.stringPropertyNames()) {
                w.println(k + "=" + props.getProperty(k));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public void replaceRecipient(String oldValue, String newValue) {
        List<String> list = getRecipients();
        boolean changed = false;

        if (list.remove(oldValue)) {
            changed = true;
        }
        if (!list.contains(newValue)) {
            list.add(newValue);
            changed = true;
        }

        if (changed) {
            saveRecipients(list);
        }
    }

    public String getTimezone() {
        return get("timezone", "Europe/Moscow");
    }

    public void setTimezone(String timezone) {
        set("timezone", timezone);
    }

    private synchronized void saveRecipients(List<String> list) {
        try (PrintWriter w = new PrintWriter(new FileWriter(recipientsFile))) {
            for (String s : list) w.println(s);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
