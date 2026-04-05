package com.example.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ImageService {

    private static final Logger log = LoggerFactory.getLogger(ImageService.class);

    @Autowired
    public ImageIndexRepository imageIndexRepository;

    public String saveImage(Message message, String fileId, String fileExtension) {
        try {
            ImageIndex imageIndex = new ImageIndex();
            imageIndex.setFileId(fileId);
            imageIndexRepository.save(imageIndex);
            log.info("Картинка сохранена: {}", fileId);
            return fileId;
        } catch (Exception e) {
            log.error("Ошибка сохранения картинки: {}", e.getMessage());
            return null;
        }
    }

    public String getFileId(String fileName) {
        return fileName;
    }

    public List<String> getImageNames() {
        return imageIndexRepository.findAll().stream()
                .map(ImageIndex::getFileId)
                .collect(Collectors.toList());
    }

    public InputFile getImageInputFile(String fileName) {
        String fileId = getFileId(fileName);
        if (fileId != null) {
            return new InputFile(fileId);
        }
        return null;
    }

    public int getImageCount() {
        return getImageNames().size();
    }
}
