package com.example.bot;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.io.*;
import java.util.*;

@Component
public class QueueService {
    private static final String QUEUE_FILE = "queue.txt";
    private final MessageService messageService;
    private final ImageService imageService;
    private final Random random = new Random();

    public QueueService(MessageService messageService, ImageService imageService) {
        this.messageService = messageService;
        this.imageService = imageService;
    }

    public List<QueueItem> loadQueue() {
        List<QueueItem> queue = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new FileReader(QUEUE_FILE))) {
            String line;
            while ((line = r.readLine()) != null) {
                QueueItem item = QueueItem.fromString(line.trim());
                if (item != null) queue.add(item);
            }
        } catch (IOException e) {
            // Файла нет
        }
        return queue;
    }

    public void saveQueue(List<QueueItem> queue) {
        try (PrintWriter w = new PrintWriter(new FileWriter(QUEUE_FILE))) {
            for (QueueItem item : queue) {
                w.println(item.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addText(String text) {
        List<QueueItem> queue = loadQueue();
        queue.add(new QueueItem(QueueItem.Type.TEXT, text));
        saveQueue(queue);
    }

    public void addImage(String imageName) {
        List<QueueItem> queue = loadQueue();
        queue.add(new QueueItem(QueueItem.Type.IMAGE, imageName));
        saveQueue(queue);
    }

    public int getQueueSize() {
        return loadQueue().size();
    }

    public int getTextCount() {
        return messageService.loadMessages().size();
    }

    public int getImageCount() {
        return imageService.getImageCount();
    }

    public void addFromTextsAndImages() {
        // Добавляем все тексты в очередь
        List<String> texts = messageService.loadMessages();
        List<QueueItem> queue = loadQueue();
        for (String t : texts) {
            queue.add(new QueueItem(QueueItem.Type.TEXT, t));
        }
        
        // Добавляем все картинки в очередь
        List<String> images = imageService.getImageNames();
        for (String img : images) {
            queue.add(new QueueItem(QueueItem.Type.IMAGE, img));
        }
        
        saveQueue(queue);
    }

    public QueueItem popFirst() {
        List<QueueItem> queue = loadQueue();
        if (queue.isEmpty()) return null;
        
        QueueItem item = queue.remove(0);
        saveQueue(queue);
        return item;
    }

    public QueueItem popRandom() {
        List<QueueItem> queue = loadQueue();
        if (queue.isEmpty()) return null;
        
        int idx = random.nextInt(queue.size());
        QueueItem item = queue.remove(idx);
        saveQueue(queue);
        return item;
    }

    public InputFile getImageInputFile(String imageName) {
        return imageService.getImageInputFile(imageName);
    }

    public String getQueueList() {
        List<QueueItem> queue = loadQueue();
        if (queue.isEmpty()) return "Очередь пуста";
        
        StringBuilder sb = new StringBuilder("Очередь (" + queue.size() + "):\n");
        for (int i = 0; i < Math.min(10, queue.size()); i++) {
            QueueItem item = queue.get(i);
            String preview = item.type == QueueItem.Type.TEXT 
                ? (item.content.length() > 30 ? item.content.substring(0, 30) + "..." : item.content)
                : "[Картинка: " + item.content + "]";
            sb.append(i + 1).append(". ").append(item.type).append(" - ").append(preview).append("\n");
        }
        if (queue.size() > 10) sb.append("... и ещё ").append(queue.size() - 10);
        return sb.toString();
    }
}
