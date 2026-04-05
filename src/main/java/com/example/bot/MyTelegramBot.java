package com.example.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.example.bot.QueueItem.Type;

@Component
public class MyTelegramBot extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(MyTelegramBot.class);

    @Value("${bot.token}")
    private String token;

    @Value("${bot.username}")
    private String username;

    @Value("${admin-chat-id:}")
    private String adminChatId;


    private final ConfigService config;
    private final MessageService messageService;
    private final ImageService imageService;
    private final QueueService queueService;
    private final Random random = new Random();

    public MyTelegramBot(ConfigService config, MessageService messageService, ImageService imageService, QueueService queueService) {
        this.config = config;
        this.messageService = messageService;
        this.imageService = imageService;
        this.queueService = queueService;
    }

    @PostConstruct
    public void init() {
        log.info("Бот инициализирован");
    }


    private String getAdminId() {
        String fromConfig = config.get("admin-id", "");
        if (fromConfig != null && !fromConfig.isEmpty()) return fromConfig;
        return adminChatId != null ? adminChatId : "";
    }
    private String getTime() { return config.get("time", "09:00"); }
    private void setTime(String val) { config.set("time", val); }
    private void setLastSent(String val) { config.set("last_sent", val); }
    private String getLastSent() { return config.get("last_sent", ""); }
    private List<String> getRecipients() { return config.getRecipients(); }

    @Override
    public void onUpdateReceived(Update update) {
        Message msg = update.getMessage();
        if (msg == null) return;

        Long chatId = msg.getChatId();
        String chatIdStr = String.valueOf(chatId);
        String userName = msg.getFrom().getUserName();
        String userNameWithAt = userName != null ? "@" + userName : null;

        String adminId = getAdminId();
        boolean isAdmin = chatIdStr.equals(adminId);

        // Обработка /start - для админа показываем команды, для обычных - подписка
        if (msg.hasText() && (msg.getText().equals("/start") || msg.getText().equals("🚀 Старт"))) {
            log.info("ПОЛЬЗОВАТЕЛЬ {} (@{}) нажал /start (isAdmin={})", chatIdStr, userNameWithAt, isAdmin);
            if (isAdmin) {
                // Админ - показываем все команды
                sendMessageWithKeyboard(chatIdStr, "Привет, админ!\n\n" +
                    "📋 *Получатели:*\n" +
                    "/recipients - список\n" +
                    "/addrecipient 123456789 - добавить по ID\n" +
                    "/delrecipient 123456789 - удалить\n\n" +
                    "⏰ *Рассылка:*\n" +
                    "/time - время отправки\n" +
                    "/settime 14:00\n" +
                    "/now - отправить сейчас\n\n" +
                    "📝 *Очередь:*\n" +
                    "/add текст - добавить текст\n" +
                    "/queue - показать очередь\n" +
                    "/fillqueue - заполнить\n" +
                    "/stats - статистика\n\n" +
                    "🖼 *Картинки:*\n" +
                    "Отправь фото боту - добавить\n" +
                    "/images - список\n" +
                    "/sendimage N - отправить №N\n" +
                    "/sendimageall - всем\n\n" +
                    "📜 *Прочее:*\n" +
                    "/messages - тексты\n" +
                    "/logs");
                log.info("Админу {} отправлен список команд", chatIdStr);
            } else {
                // Обычный пользователь - подписываем по chatId
                config.addRecipient(chatIdStr);
                sendMessageWithKeyboard(chatIdStr, "✨ *Добро пожаловать!*\n\nВы подписаны на рассылку.\nЖдите новые сообщения 📬");
                log.info("Пользователь {} (@{}) подписался на рассылку", chatIdStr, userNameWithAt);
            }
            return;
        }

        boolean isRecipient = getRecipients().contains(chatIdStr);

        if (!isAdmin && !isRecipient) {
            if (msg.hasText()) {
                log.info("ОТКАЗ: пользователь {} (@{}) не в списке получателей", chatIdStr, userNameWithAt);
                sendMessageWithKeyboard(chatIdStr, "Извините, вы не участник бота.\nСвяжитесь с администратором.");
            }
            return;
        }

        // Обработка картинок от админа
        if (isAdmin && msg.hasPhoto()) {
            handleAdminPhoto(msg);
            return;
        }

        if (!msg.hasText()) return;

        String text = msg.getText();

        if (isRecipient && !isAdmin) {
            log.info("ПОЛУЧАТЕЛЬ {} отправил: {}", chatIdStr, text);
            handleRecipient(chatIdStr, userNameWithAt, text, adminId);
        } else if (isAdmin) {
            log.info("АДМИН {} отправил команду: {}", chatIdStr, text);
            handleAdmin(chatIdStr, text, isAdmin, userNameWithAt);
        }
    }

    private void handleAdminPhoto(Message msg) {
        String chatIdStr = String.valueOf(msg.getChatId());
        List<PhotoSize> photos = msg.getPhoto();
        if (photos == null || photos.isEmpty()) {
            sendMessage(chatIdStr, "Не удалось получить фото");
            return;
        }

        PhotoSize photo = photos.get(photos.size() - 1); // Самое большое фото
        String fileId = photo.getFileId();

        // Сохраняем ссылку на фото
        String fileName = imageService.saveImage(msg, fileId, ".jpg");

        // Добавляем в очередь
        queueService.addImage(fileName);

        int queueSize = queueService.getQueueSize();
        sendMessage(chatIdStr, "Фото добавлено в очередь! В очереди: " + queueSize);
    }

    private void handleRecipient(String chatId, String userName, String text, String adminId) {
        // /start теперь обрабатывается в onUpdateReceived до вызова handleRecipient
        if (text.equals("📬 Написать админу") || text.equals("📬 Админу")) {
            sendMessage(chatId, "Введите сообщение для админа:", "recipient-prompt-admin");
        } else {
            String from = userName != null ? userName : chatId;
            log.info("От {} админу: {}", from, text);
            sendMessage(adminId, "📬 От " + from + ":\n" + text, "forward-to-admin");
            sendMessage(chatId, "Отправлено админу!", "recipient-confirm-forward");
        }
    }

    private void sendMessageWithKeyboard(String chatId, String text) {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(false);

        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add(KeyboardButton.builder().text("🚀 Старт").build());
        row.add(KeyboardButton.builder().text("📬 Админу").build());
        rows.add(row);

        keyboard.setKeyboard(rows);

        try {
            execute(SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(keyboard)
                .parseMode("Markdown")
                .build());
            // Клавиатура отправлена
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки клавиатуры: {}", e.getMessage());
        }
    }

    private void handleAdmin(String chatId, String text, boolean isAdmin, String userNameWithAt) {
        try {
            if (text.equals("/admin")) {
                sendMessage(chatId, isAdmin ? "Вы админ" : "Вы не админ");
            } else if (text.equals("/recipients")) {
                handleRecipients(chatId);
            } else if (text.equals("/time")) {
                handleTime(chatId);
            } else if (text.equals("/messages")) {
                handleMessages(chatId);
            } else if (text.equals("/now")) {
                sendNow(chatId);
            } else if (text.equals("/logs")) {
                sendLogs(chatId);
            } else if (text.equals("/images")) {
                handleImages(chatId);
            } else if (text.equals("/sendimageall")) {
                handleSendImageAll(chatId);
            } else if (text.equals("/queue")) {
                handleQueue(chatId);
            } else if (text.equals("/fillqueue")) {
                handleFillQueue(chatId);
            } else if (text.equals("/stats")) {
                handleStats(chatId);
            } else if (text.startsWith("/sendimage ")) {
                handleSendImage(chatId, text);
            } else if (text.equals("📬 Админу")) {
                sendMessage(chatId, "Вы админ! Используйте команды для рассылки.\n/add текст - добавить сообщение\n/now - отправить сейчас");
            } else if (text.startsWith("/settime ")) {
                handleSetTime(chatId, text);
            } else if (text.startsWith("/add ")) {
                handleAddText(chatId, text);
            } else if (text.startsWith("/addrecipient ")) {
                handleAddRecipient(chatId, text);
            } else if (text.equals("/chatid")) {
                handleChatId(chatId, userNameWithAt);
            } else if (text.startsWith("/settimezone ")) {
                handleSetTimezone(chatId, text);
            }
        } catch (Exception e) {
            log.error("Ошибка обработки команды админа: {}", e.getMessage());
        }
    }

    private void sendMessage(String chatId, String text) {
        sendMessage(chatId, text, "default");
    }

    private void sendMessage(String chatId, String text, String context) {
        try {
            execute(SendMessage.builder().chatId(chatId).text(text).build());
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения {} в {}: {}", context, chatId, e.getMessage());
        }
    }

    private void handleRecipients(String chatId) {
        if (getRecipients().isEmpty()) {
            sendMessage(chatId, "Нет получателей!\n/addrecipient ID или @nick", "admin-recipients-empty");
        } else {
            StringBuilder sb = new StringBuilder("Получатели:\n");
            for (int i = 0; i < getRecipients().size(); i++) {
                sb.append(i + 1).append(". ").append(getRecipients().get(i)).append("\n");
            }
            sendMessage(chatId, sb.toString(), "admin-recipients-list");
        }
    }

    private void handleTime(String chatId) {
        sendMessage(chatId, "Время отправки: " + getTime(), "admin-time");
    }

    private void handleMessages(String chatId) {
        List<String> msgs = messageService.loadMessages();
        if (msgs.isEmpty()) {
            sendMessage(chatId, "Пусто");
        } else {
            StringBuilder sb = new StringBuilder("Сообщений: ").append(msgs.size()).append("\n");
            for (int i = 0; i < Math.min(10, msgs.size()); i++) {
                sb.append(i + 1).append(". ").append(msgs.get(i)).append("\n");
            }
            sendMessage(chatId, sb.toString());
        }
    }

    private void handleImages(String chatId) {
        List<String> images = imageService.getImageNames();
        if (images.isEmpty()) {
            sendMessage(chatId, "Нет сохранённых картинок.\nОтправьте фото боту, чтобы сохранить.");
        } else {
            int count = images.size();
            sendMessage(chatId, "Сохранённые картинки (" + count + "):\n" +
                "Чтобы отправить: /sendimage 1\n(или /sendimageall для всем)");
        }
    }

    private void handleSendImageAll(String chatId) {
        List<String> imgs = imageService.getImageNames();
        if (imgs.isEmpty()) {
            sendMessage(chatId, "Нет картинок для отправки");
        } else if (getRecipients().isEmpty()) {
            sendMessage(chatId, "Нет получателей");
        } else {
            for (String imgName : imgs) {
                InputFile img = imageService.getImageInputFile(imgName);
                if (img != null) {
                    for (String r : getRecipients()) {
                        sendPhoto(r, img);
                    }
                }
            }
            sendMessage(chatId, "Отправлено " + imgs.size() + " картинок всем получателям");
        }
    }

    private void handleQueue(String chatId) {
        sendMessage(chatId, queueService.getQueueList());
    }

    private void handleFillQueue(String chatId) {
        queueService.addFromTextsAndImages();
        int qSize = queueService.getQueueSize();
        sendMessage(chatId, "Очередь заполнена! Всего в очереди: " + qSize);
    }

    private void handleStats(String chatId) {
        int txtCount = queueService.getTextCount();
        int imgCount = queueService.getImageCount();
        int queueCount = queueService.getQueueSize();
        sendMessage(chatId, "Статистика:\n" +
            "Текстов: " + txtCount + "\n" +
            "Картинок: " + imgCount + "\n" +
            "В очереди: " + queueCount + "\n\n" +
            "Отправь /fillqueue чтобы заполнить очередь из текстов и картинок");
    }

    private void handleSendImage(String chatId, String text) {
        int idx;
        try {
            idx = Integer.parseInt(text.substring(11)) - 1;
        } catch (NumberFormatException e) {
            sendMessage(chatId, "Используйте: /sendimage 1");
            return;
        }
        List<String> allImages = imageService.getImageNames();
        if (idx < 0 || idx >= allImages.size()) {
            sendMessage(chatId, "Нет такой картинки. Всего: " + allImages.size());
            return;
        }
        String imgName = allImages.get(idx);
        InputFile img = imageService.getImageInputFile(imgName);
        if (getRecipients().isEmpty()) {
            sendMessage(chatId, "Нет получателей");
            return;
        }
        for (String r : getRecipients()) {
            sendPhoto(r, img);
        }
        sendMessage(chatId, "Отправлено: " + imgName);
    }

    private void handleSetTime(String chatId, String text) {
        String[] parts = text.split("\\s+", 2);
        String arg = parts.length > 1 ? parts[1] : "";
        arg = arg.replaceAll("@\\w+", "").trim();
        if (!arg.matches("^\\d{1,2}:\\d{2}$")) {
            sendMessage(chatId, "Неверный формат времени. Используйте HH:MM, например /settime 14:00");
            return;
        }
        setTime(arg);
        sendMessage(chatId, "Время: " + getTime());
    }

    private void handleAddText(String chatId, String text) {
        String[] parts = text.split("\\s+", 2);
        String arg = parts.length > 1 ? parts[1] : "";
        arg = arg.replaceAll("@\\w+", "").trim();
        if (arg.isEmpty()) {
            sendMessage(chatId, "Текст не может быть пустым");
            return;
        }
        queueService.addText(arg);
        sendMessage(chatId, "Добавлено в очередь! Всего: " + queueService.getQueueSize());
    }

    private void handleAddRecipient(String chatId, String text) {
        log.info("Обработка /addrecipient: {}", text);
        String[] parts = text.split("\\s+", 2);
        String arg = parts.length > 1 ? parts[1] : "";
        arg = arg.replaceAll("@\\w+", "").trim(); // убрать @botname
        log.info("Аргумент после обработки: '{}'", arg);
        if (!arg.matches("^\\d+$")) {
            sendMessage(chatId, "Введите chat ID: /addrecipient 123456789");
            return;
        }
        config.addRecipient(arg);
        sendMessage(chatId, "Получатель добавлен: " + arg);
    }

    private void handleDelRecipient(String chatId, String text) {
        String[] parts = text.split("\\s+", 2);
        String arg = parts.length > 1 ? parts[1] : "";
        arg = arg.replaceAll("@\\w+", "").trim();
        config.removeRecipient(arg);
        sendMessage(chatId, "Получатель удалён: " + arg);
    }

    private void handleChatId(String chatId, String userName) {
        sendMessage(chatId, "Chat ID: " + chatId + "\nUsername: " + userName);
    }

    private void handleSetTimezone(String chatId, String text) {
        String[] parts = text.split("\\s+", 2);
        String arg = parts.length > 1 ? parts[1] : "";
        arg = arg.replaceAll("@\\w+", "").trim();
        if (arg.isEmpty()) {
            sendMessage(chatId, "Укажите часовой пояс, например /settimezone Europe/Moscow");
            return;
        }
        config.setTimezone(arg);
        sendMessage(chatId, "Часовой пояс установлен: " + arg + ". Перезапустите бота для применения.");
    }

    private void sendPhoto(String chatId, InputFile photo) {
        try {
            execute(SendPhoto.builder().chatId(chatId).photo(photo).build());
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки фото в {}: {}", chatId, e.getMessage());
        }
    }

    private String shorten(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replace("\n", "\\n");
        return normalized.length() > 120 ? normalized.substring(0, 120) + "..." : normalized;
    }

    private void sendNow(String chatId) {
        
        if (getRecipients().isEmpty()) {
            sendMessage(chatId, "Нет получателей! Добавьте: /addrecipient ID или @nick");
            return;
        }

        // Пытаемся взять из очереди
        QueueItem item = queueService.popFirst();
        if (item == null) {
            // Если очередь пуста, пробуем старый способ
            List<String> msgs = messageService.loadMessages();
            if (msgs.isEmpty()) {
                sendMessage(chatId, "Очередь пуста! Добавьте /add текст или отправьте картинку");
                return;
            }
            String msg = msgs.get(random.nextInt(msgs.size()));
            for (String r : getRecipients()) {
                sendMessage(r, msg);
            }
            msgs.remove(msg);
            messageService.saveMessages(msgs);
            sendMessage(chatId, "Отправлено: " + msg);
        } else {
            // Отправляем из очереди
            for (String r : getRecipients()) {
                if (item.type == QueueItem.Type.TEXT) {
                    sendMessage(r, item.content);
                } else if (item.type == QueueItem.Type.IMAGE) {
                    InputFile img = queueService.getImageInputFile(item.content);
                    if (img != null) sendPhoto(r, img);
                }
            }
            sendMessage(chatId, "Отправлено из очереди: " + item.type + " - " +
                (item.type == QueueItem.Type.TEXT ? item.content.substring(0, Math.min(30, item.content.length())) : item.content));
        }
    }

    private void sendLogs(String chatId) {
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
        LocalDateTime now = LocalDateTime.now();
        String curTime = now.format(DateTimeFormatter.ofPattern("HH:mm"));
        String curMinute = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String targetTime = getTime();
        String lastSent = getLastSent();
        

        if (curTime.equals(targetTime) && !curMinute.equals(lastSent) && !getRecipients().isEmpty()) {
            // Пытаемся взять из очереди
            QueueItem item = queueService.popFirst();

            if (item == null) {
                // Если очередь пуста, пробуем старый способ
                List<String> msgs = messageService.loadMessages();
                if (!msgs.isEmpty()) {
                    String msg = msgs.get(random.nextInt(msgs.size()));
                    for (String r : getRecipients()) {
                        sendMessage(r, msg);
                    }
                    msgs.remove(msg);
                    messageService.saveMessages(msgs);
                    setLastSent(curMinute);
                    log.info("Автоотправка: {}", msg.substring(0, Math.min(50, msg.length())));
                }
            } else {
                // Отправляем из очереди (текст или картинку)
                String itemDesc = item.type == QueueItem.Type.TEXT ? "текст" : "картинку";
                for (String r : getRecipients()) {
                    if (item.type == QueueItem.Type.TEXT) {
                        sendMessage(r, item.content);
                    } else if (item.type == QueueItem.Type.IMAGE) {
                        InputFile img = queueService.getImageInputFile(item.content);
                        if (img != null) sendPhoto(r, img);
                    }
                }
                setLastSent(curMinute);
                log.info("Автоотправка: {}", itemDesc);
            }
        }
    }

    @Override public String getBotUsername() { return username; }
    @Override public String getBotToken() { return token; }
}
