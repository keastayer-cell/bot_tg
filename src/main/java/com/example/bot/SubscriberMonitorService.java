package com.example.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.ChatMember;
import org.telegram.telegrambots.meta.api.objects.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис для мониторинга активности подписчиков
 * Отслеживает статусы онлайн/оффлайн и действия пользователей
 */
@Service
public class SubscriberMonitorService {

    private static final Logger log = LoggerFactory.getLogger(SubscriberMonitorService.class);

    // Хранение последнего известного статуса каждого пользователя
    private final Map<String, UserStatus> userStatuses = new ConcurrentHashMap<>();

    // Включен ли мониторинг
    private boolean monitoringEnabled = false;

    private static class UserStatus {
        String status; // "online", "offline", "typing", "recording", etc.
        LocalDateTime lastSeen;
        String userName;

        UserStatus(String status, String userName) {
            this.status = status;
            this.userName = userName;
            this.lastSeen = LocalDateTime.now();
        }

        void updateStatus(String newStatus) {
            this.status = newStatus;
            this.lastSeen = LocalDateTime.now();
        }
    }

    /**
     * Включить/выключить мониторинг подписчиков
     */
    public void setMonitoringEnabled(boolean enabled) {
        this.monitoringEnabled = enabled;
        log.info("📊 Мониторинг подписчиков: {}", enabled ? "ВКЛЮЧЕН" : "ОТКЛЮЧЕН");
    }

    /**
     * Проверить включен ли мониторинг
     */
    public boolean isMonitoringEnabled() {
        return monitoringEnabled;
    }

    /**
     * Обработать изменение статуса пользователя
     * @return сообщение для админа или null если не нужно уведомлять
     */
    public String processUserStatusChange(String chatId, String userName, String newStatus) {
        if (!monitoringEnabled) {
            return null;
        }

        String userKey = chatId + ":" + userName;
        UserStatus currentStatus = userStatuses.get(userKey);

        // Если статус не изменился, не уведомляем
        if (currentStatus != null && currentStatus.status.equals(newStatus)) {
            currentStatus.lastSeen = LocalDateTime.now(); // обновляем время
            return null;
        }

        // Создаем или обновляем статус
        if (currentStatus == null) {
            currentStatus = new UserStatus(newStatus, userName);
            userStatuses.put(userKey, currentStatus);
        } else {
            currentStatus.updateStatus(newStatus);
        }

        // Формируем уведомление для админа
        String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String emoji = getStatusEmoji(newStatus);

        String notification = String.format("%s 👤 %s (%s) - %s [%s]",
            emoji, userName, chatId, getStatusDescription(newStatus), timeStr);

        log.info("📊 Статус пользователя изменен: {}", notification);
        return notification;
    }

    /**
     * Обработать действие пользователя (печатает, записывает и т.д.)
     */
    public String processUserAction(String chatId, String userName, String action) {
        if (!monitoringEnabled) {
            return null;
        }

        // Для действий типа "печатает" создаем временный статус
        String statusKey = "action:" + action.toLowerCase();
        return processUserStatusChange(chatId, userName, statusKey);
    }

    /**
     * Получить текущую статистику мониторинга
     */
    public String getMonitoringStats() {
        if (!monitoringEnabled) {
            return "📊 Мониторинг подписчиков: ОТКЛЮЧЕН\n\nВключите командой /monitor on";
        }

        int totalUsers = userStatuses.size();
        long onlineNow = userStatuses.values().stream()
            .filter(s -> "online".equals(s.status))
            .count();

        StringBuilder sb = new StringBuilder();
        sb.append("📊 Мониторинг подписчиков: ВКЛЮЧЕН\n\n");
        sb.append(String.format("👥 Всего отслеживаемых: %d\n", totalUsers));
        sb.append(String.format("🟢 Сейчас онлайн: %d\n\n", onlineNow));

        if (!userStatuses.isEmpty()) {
            sb.append("📋 Последние активности:\n");
            userStatuses.values().stream()
                .sorted((a, b) -> b.lastSeen.compareTo(a.lastSeen))
                .limit(10)
                .forEach(status -> {
                    String timeStr = status.lastSeen.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                    sb.append(String.format("%s %s - %s [%s]\n",
                        getStatusEmoji(status.status),
                        status.userName,
                        getStatusDescription(status.status),
                        timeStr));
                });
        }

        return sb.toString();
    }

    /**
     * Очистить историю статусов
     */
    public void clearHistory() {
        userStatuses.clear();
        log.info("🧹 История статусов подписчиков очищена");
    }

    private String getStatusEmoji(String status) {
        return switch (status.toLowerCase()) {
            case "online" -> "🟢";
            case "offline" -> "🔴";
            case "action:typing" -> "✍️";
            case "action:recording_voice" -> "🎤";
            case "action:recording_video" -> "🎥";
            case "action:uploading_photo" -> "📷";
            case "action:uploading_video" -> "🎬";
            case "action:uploading_document" -> "📄";
            default -> "👤";
        };
    }

    private String getStatusDescription(String status) {
        return switch (status.toLowerCase()) {
            case "online" -> "онлайн";
            case "offline" -> "оффлайн";
            case "action:typing" -> "печатает";
            case "action:recording_voice" -> "записывает голосовое";
            case "action:recording_video" -> "записывает видео";
            case "action:uploading_photo" -> "отправляет фото";
            case "action:uploading_video" -> "отправляет видео";
            case "action:uploading_document" -> "отправляет файл";
            default -> status;
        };
    }
}