package com.example.bot;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class DiagramService {

    /**
     * Генерирует PNG изображение диаграммы из текста в формате PlantUML
     * @param plantUmlText текст диаграммы в формате PlantUML
     * @return массив байтов PNG изображения
     * @throws IOException если произошла ошибка генерации
     */
    public byte[] generateDiagramPng(String plantUmlText) throws IOException {
        // Добавляем @startuml если его нет
        if (!plantUmlText.trim().startsWith("@startuml")) {
            plantUmlText = "@startuml\n" + plantUmlText + "\n@enduml";
        }

        SourceStringReader reader = new SourceStringReader(plantUmlText);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            reader.generateImage(output, new FileFormatOption(FileFormat.PNG));
            return output.toByteArray();
        } catch (Exception e) {
            throw new IOException("Ошибка генерации диаграммы: " + e.getMessage(), e);
        }
    }

    /**
     * Проверяет, является ли текст валидным описанием диаграммы PlantUML
     * @param text текст для проверки
     * @return true если текст содержит элементы диаграммы
     */
    public boolean isValidDiagramText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        String lowerText = text.toLowerCase();
        return lowerText.contains("start") ||
               lowerText.contains("->") ||
               lowerText.contains("-->") ||
               lowerText.contains("note") ||
               lowerText.contains("participant") ||
               lowerText.contains("actor") ||
               lowerText.contains("class") ||
               lowerText.contains("rectangle") ||
               lowerText.contains("circle");
    }

    /**
     * Возвращает примеры использования диаграмм
     * @return строка с примерами
     */
    public String getExamples() {
        return """
                📊 Примеры диаграмм:

                🔄 Процесс работы бота:
                ```
                @startuml
                Админ -> Бот: /add Привет!
                Бот -> QueueService: addText()
                QueueService -> БД: INSERT в queue
                Бот -> Админ: ✓ Добавлено
                @enduml
                ```

                📈 Последовательность:
                ```
                @startuml
                participant Админ
                participant Бот
                participant БД

                Админ -> Бот: /now
                Бот -> QueueService: popFirst()
                QueueService -> БД: SELECT + DELETE
                Бот -> Подписчики: Отправка
                @enduml
                ```

                🏗️ Архитектура:
                ```
                @startuml
                rectangle "Telegram API" as TG
                rectangle "MyTelegramBot" as Bot
                rectangle "Services" as Services {
                  rectangle ConfigService
                  rectangle QueueService
                  rectangle ImageService
                }
                rectangle "PostgreSQL" as DB

                TG --> Bot
                Bot --> Services
                Services --> DB
                @enduml
                ```
                """;
    }
}