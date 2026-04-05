package com.example.bot;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class ImageService {
    private static final String IMAGES_DIR = "images";
    private static final String IMAGES_INDEX = "images_index.txt";

    public ImageService() {
        // Создаём папку для картинок
        new File(IMAGES_DIR).mkdirs();
    }

    public String saveImage(Message message, String fileId, String fileExtension) {
        try {
            String uniqueName = UUID.randomUUID().toString().substring(0, 8) + fileExtension;
            String filePath = IMAGES_DIR + "/" + uniqueName;
            
            // Сохраняем ID файла для дальнейшего использования
            // (Telegram хранит file_id, который можно использовать повторно)
            saveToIndex(uniqueName, fileId);
            
            return uniqueName;
        } catch (Exception e) {
            e.printStackTrace();
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
        File dir = new File(IMAGES_DIR);
        File[] files = dir.listFiles((d, name) -> !name.startsWith("."));
        if (files != null) {
            for (File f : files) {
                names.add(f.getName());
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
