package com.example.bot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class MessageService {
    @Autowired
    public MessageRepository messageRepository;

    public List<String> loadMessages() {
        return messageRepository.findAll().stream()
                .map(Message::getText)
                .collect(Collectors.toList());
    }

    public void addMessage(String text) {
        Message message = new Message();
        message.setText(text);
        messageRepository.save(message);
    }

    public void saveMessages(List<String> messages) {
        messageRepository.deleteAll();
        for (String text : messages) {
            Message message = new Message();
            message.setText(text);
            messageRepository.save(message);
        }
    }
}
