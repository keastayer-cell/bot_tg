package com.example.bot;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Component
public class MessageService {
    private String messagesFile;

    public MessageService() {
        String dataDir = "/app/data";
        if (new File(dataDir).exists()) {
            messagesFile = dataDir + "/messages.txt";
        } else {
            messagesFile = "messages.txt";
        }
    }

    public List<String> loadMessages() {
        List<String> messages = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(messagesFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) messages.add(line.trim());
            }
        } catch (IOException e) {
            // Файл может не существовать
        }
        return messages;
    }

    public void saveMessages(List<String> messages) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(messagesFile))) {
            for (String msg : messages) writer.println(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addMessage(String text) {
        List<String> messages = loadMessages();
        messages.add(text);
        saveMessages(messages);
    }
}
