package com.example.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class ImageService {

    private static final Logger log = LoggerFactory.getLogger(ImageService.class);
    private static final String IMAGES_DIR = "images";
    private static final String IMAGES_INDEX = "images_index.txt";

    public ImageService() {
        new File(IMAGES_DIR).mkdirs();
    }

    public String saveImage(Message message, String fileId, String fileExtension) {
        try {
            String uniqueName = UUID.randomUUID().toString().substring(0, 8) + fileExtension;

            // Сохраняем fileId в индексный файл
            saveToIndex(uniqueName, fileId);
            log.info("Картинка сохранена: {} -> {}", uniqueName, fileId);

            return uniqueName;
        } catch (Exception e) {
            log.error("Ошибка сохранения картинки: {}", e.getMessage());
            return null;
        }
    }

    public void saveToIndex(String fileName, String fileId) {
        try (PrintWriter w = new PrintWriter(new FileWriter(IMAGES_INDEX, true))) {
            w.println(fileName + "=" + fileId);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getFileId(String fileName) {
        try (BufferedReader r = new BufferedReader(new FileReader(IMAGES_INDEX))) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] parts = line.split("=", 2);
                if (parts.length == 2 && parts[0].equals(fileName)) {
                    return parts[1];
                }
            }
        } catch (IOException e) {
            // Файла нет
        }
        return null;
    }

    public List<String> getImageNames() {
        List<String> names = new ArrayList<>();

        // Сначала проверяем индексный файл (где хранятся fileId)
        File indexFile = new File(IMAGES_INDEX);
        if (indexFile.exists()) {
            try (BufferedReader r = new BufferedReader(new FileReader(IMAGES_INDEX))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.contains("=")) {
                        names.add(line.split("=")[0]);
                    }
                }
            } catch (IOException e) {
                log.warn("Ошибка чтения индекса: {}", e.getMessage());
            }
        }

        // Также проверяем папку с файлами
        File dir = new File(IMAGES_DIR);
        File[] files = dir.listFiles((d, name) -> !name.startsWith("."));
        if (files != null) {
            for (File f : files) {
                if (!names.contains(f.getName())) {
                    names.add(f.getName());
                }
            }
        }
        return names;
    }

    public InputFile getImageInputFile(String fileName) {
        String fileId = getFileId(fileName);
        if (fileId != null) {
            // Используем file_id от Telegram (не нужно скачивать файл)
            return new InputFile(fileId);
        }
        
        // Fallback: читаем из файла
        File file = new File(IMAGES_DIR + "/" + fileName);
        if (file.exists()) {
            InputFile inputFile = new InputFile();
            inputFile.setMedia(file, fileName);
            return inputFile;
        }
        return null;
    }

    public int getImageCount() {
        return getImageNames().size();
    }
}
