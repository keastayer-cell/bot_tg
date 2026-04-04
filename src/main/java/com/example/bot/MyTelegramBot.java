package com.example.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.annotation.PostConstruct;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Component
public class MyTelegramBot extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(MyTelegramBot.class);

    @Value("${bot.token}")
    private String token;

    @Value("${bot.username}")
    private String username;

    private final ConfigService config;
    private final MessageService messageService;
    private final Random random = new Random();

    public MyTelegramBot(ConfigService config, MessageService messageService) {
        this.config = config;
        this.messageService = messageService;
    }

    @PostConstruct
    public void init() {
        log.info("Бот запущен. Получатели: {}, Время: {}", getRecipients(), getTime());
    }

    private String getAdminId() { return config.get("admin-id", ""); }
    private String getTime() { return config.get("time", "09:00"); }
    private String getLastSent() { return config.get("last_sent", ""); }
    private void setTime(String val) { config.set("time", val); }
    private void setLastSent(String val) { config.set("last_sent", val); }
    private List<String> getRecipients() { return config.getRecipients(); }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        Message msg = update.getMessage();
        Long chatId = msg.getChatId();
        String text = msg.getText();
        String user = msg.getFrom().getUserName();

        log.info("[{}] {}", user != null ? "@" + user : chatId, text);

        String adminId = getAdminId();
        boolean isAdmin = String.valueOf(chatId).equals(adminId);
        boolean isRecipient = getRecipients().contains(String.valueOf(chatId));

        if (!isAdmin && !isRecipient) {
            log.warn("Неизвестный пользователь: {}", chatId);
            return;
        }

        if (isRecipient && !isAdmin) {
            handleRecipient(chatId, text, adminId);
        } else if (isAdmin) {
            handleAdmin(chatId, text);
        }
    }

    private void handleRecipient(Long chatId, String text, String adminId) {
        if (text.equals("/start")) {
            sendMessage(chatId, "Спасибо что активировали бота :) Ожидайте сообщений.");
        } else {
            sendMessage(Long.parseLong(adminId), "📬 От " + chatId + ":\n" + text);
            sendMessage(chatId, "Отправлено админу!");
        }
    }

    private void handleAdmin(Long chatId, String text) {
        try {
            switch (text) {
                case "/start":
                    sendMessage(chatId, "Привет, админ!\n\nКоманды:\n" +
                        "/recipients - список получателей\n" +
                        "/addrecipient ID - добавить\n" +
                        "/delrecipient ID - удалить\n" +
                        "/time - время отправки\n" +
                        "/settime 14:00\n" +
                        "/messages - список сообщений\n" +
                        "/add текст\n" +
                        "/now - отправить сейчас\n" +
                        "/logs");
                    break;

                case "/recipients":
                    List<String> recs = getRecipients();
                    if (recs.isEmpty()) {
                        sendMessage(chatId, "Нет получателей!\n/addrecipient ID");
                    } else {
                        StringBuilder sb = new StringBuilder("Получатели:\n");
                        for (int i = 0; i < recs.size(); i++) {
                            sb.append(i + 1).append(". ").append(recs.get(i)).append("\n");
                        }
                        sendMessage(chatId, sb.toString());
                    }
                    break;

                case "/time":
                    sendMessage(chatId, "Время отправки: " + getTime());
                    break;

                case "/messages":
                    List<String> msgs = messageService.loadMessages();
                    if (msgs.isEmpty()) sendMessage(chatId, "Пусто");
                    else {
                        StringBuilder sb = new StringBuilder("Сообщений: ").append(msgs.size()).append("\n");
                        for (int i = 0; i < Math.min(10, msgs.size()); i++) sb.append(i + 1).append(". ").append(msgs.get(i)).append("\n");
                        sendMessage(chatId, sb.toString());
                    }
                    break;

                case "/now":
                    sendNow(chatId);
                    break;

                case "/logs":
                    sendLogs(chatId);
                    break;

                default:
                    if (text.startsWith("/settime ")) {
                        setTime(text.substring(9));
                        sendMessage(chatId, "Время: " + getTime());
                    } else if (text.startsWith("/add ")) {
                        messageService.addMessage(text.substring(5));
                        sendMessage(chatId, "Добавлено");
                    } else if (text.startsWith("/addrecipient ")) {
                        config.addRecipient(text.substring(14));
                        sendMessage(chatId, "Получатель добавлен");
                    } else if (text.startsWith("/delrecipient ")) {
                        config.removeRecipient(text.substring(14));
                        sendMessage(chatId, "Получатель удалён");
                    } else if (text.startsWith("/msg ")) {
                        for (String r : getRecipients()) {
                            sendMessage(Long.parseLong(r), text.substring(5));
                        }
                        sendMessage(chatId, "Отправлено всем");
                    }
            }
        } catch (Exception e) {
            log.error("Ошибка: {}", e.getMessage());
            sendMessage(chatId, "Ошибка: " + e.getMessage());
        }
    }

    private void sendMessage(Long chatId, String text) {
        try {
            execute(SendMessage.builder().chatId(chatId.toString()).text(text).build());
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки: {}", e.getMessage());
        }
    }

    private void sendNow(Long chatId) {
        List<String> recs = getRecipients();
        if (recs.isEmpty()) {
            sendMessage(chatId, "Нет получателей! Добавьте: /addrecipient ID");
            return;
        }
        List<String> msgs = messageService.loadMessages();
        if (msgs.isEmpty()) {
            sendMessage(chatId, "Список пуст!");
            return;
        }
        String msg = msgs.get(random.nextInt(msgs.size()));
        for (String r : recs) {
            sendMessage(Long.parseLong(r), msg);
        }
        msgs.remove(msg);
        messageService.saveMessages(msgs);
        sendMessage(chatId, "Отправлено: " + msg);
    }

    private void sendLogs(Long chatId) {
        try (BufferedReader r = new BufferedReader(new FileReader("bot.log"))) {
            StringBuilder sb = new StringBuilder();
            String line; int n = 0;
            while ((line = r.readLine()) != null && n++ < 30) sb.append(line).append("\n");
            sendMessage(chatId, sb.toString().substring(Math.max(0, sb.length() - 3000)));
        } catch (IOException e) {
            sendMessage(chatId, "Нет логов");
        }
    }

    public void checkAndSend() {
        LocalTime now = LocalTime.now().plusHours(3);
        String curTime = now.format(DateTimeFormatter.ofPattern("HH:mm"));
        String curDate = LocalDate.now().toString();
        String targetTime = getTime();
        String lastSent = getLastSent();
        List<String> recs = getRecipients();

        if (curTime.equals(targetTime) && !curDate.equals(lastSent) && !recs.isEmpty()) {
            List<String> msgs = messageService.loadMessages();
            if (!msgs.isEmpty()) {
                String msg = msgs.get(random.nextInt(msgs.size()));
                for (String r : recs) {
                    sendMessage(Long.parseLong(r), msg);
                }
                msgs.remove(msg);
                messageService.saveMessages(msgs);
                setLastSent(curDate);
                log.info("[АВТООТПРАВКА] {}", msg);
            }
        }
    }

    @Override public String getBotUsername() { return username; }
    @Override public String getBotToken() { return token; }
}
