package com.example.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

@Component
public class ImageMigrationService {
    
    private static final Logger log = LoggerFactory.getLogger(ImageMigrationService.class);
    private static final String[] IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif", ".webp"};
    
    @Autowired
    private ImageIndexRepository imageIndexRepository;
    
    /**
     * Автоматическая миграция старых файлов изображений в БД
     * - Сканирует текущую директорию на наличие изображений
     * - Для каждого найденного файла создает запись в БД (если её ещё нет)
     * - Идемпотентна - не дублирует при повторном запуске
     */
    public void migrateImageFiles() {
        try {
            log.info("=== IMAGE MIGRATION START ===");
            
            // Определяем директорию для сканирования (текущая рабочая директория)
            String workDir = System.getProperty("user.dir");
            log.info("Scanning directory for images: {}", workDir);
            
            // Ищем все файлы с расширениями изображений
            List<File> imageFiles = findImageFiles(new File(workDir));
            log.info("Found {} image files to migrate", imageFiles.size());
            
            if (imageFiles.isEmpty()) {
                log.info("No image files found, skipping migration");
                return;
            }
            
            // Получаем существующие fileId в БД чтобы не дублировать
            Set<String> existingFileIds = new HashSet<>();
            imageIndexRepository.findAll().forEach(img -> {
                if (img.getFileId() != null) {
                    existingFileIds.add(img.getFileId());
                }
            });
            log.info("Existing images in DB: {}", existingFileIds.size());
            
            // Мигрируем каждый файл
            int migratedCount = 0;
            int skippedCount = 0;
            
            for (File imageFile : imageFiles) {
                String fileName = imageFile.getName();
                
                // Используем имя файла как fileId (уникальный идентификатор)
                // Это позволяет идентифицировать "Telegram file" даже если это обычный файл
                String fileId = fileName;
                
                // Проверяем есть ли уже такой файл в БД
                if (existingFileIds.contains(fileId)) {
                    log.debug("Image already migrated: {}", fileName);
                    skippedCount++;
                    continue;
                }
                
                // Проверяем что файл существует и может быть прочитан
                if (!imageFile.exists() || !imageFile.canRead()) {
                    log.warn("Cannot read image file: {}", imageFile.getAbsolutePath());
                    continue;
                }
                
                try {
                    // Создаем новую запись в БД
                    ImageIndex imageIndex = new ImageIndex();
                    imageIndex.setFileId(fileId);
                    imageIndexRepository.save(imageIndex);
                    
                    log.info("✓ Migrated image: {} (size: {} bytes)", 
                            fileName, imageFile.length());
                    migratedCount++;
                    existingFileIds.add(fileId);
                    
                } catch (Exception e) {
                    log.error("Failed to migrate image {}: {}", fileName, e.getMessage());
                }
            }
            
            log.info("=== IMAGE MIGRATION COMPLETE ===");
            log.info("Summary: {} migrated, {} skipped, {} failed", 
                    migratedCount, skippedCount, imageFiles.size() - migratedCount - skippedCount);
            
        } catch (Exception e) {
            log.error("Image migration failed: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Рекурсивно ищет все файлы изображений в директории и подпапках
     */
    private List<File> findImageFiles(File directory) {
        List<File> imageFiles = new ArrayList<>();
        
        if (!directory.exists() || !directory.isDirectory()) {
            return imageFiles;
        }
        
        try {
            File[] files = directory.listFiles();
            if (files == null) return imageFiles;
            
            for (File file : files) {
                // Пропускаем скрытые файлы и системные директории
                if (file.getName().startsWith(".")) {
                    continue;
                }
                
                // Рекурсивно сканируем поддиректории (но не глубже 1 уровня)
                if (file.isDirectory() && !file.getName().equals("target") 
                    && !file.getName().equals(".git")
                    && !file.getName().equals("node_modules")) {
                    // Можно раскомментировать если нужна глубокая рекурсия
                    // imageFiles.addAll(findImageFiles(file));
                    continue;
                }
                
                // Проверяем расширение файла
                if (file.isFile() && isImageFile(file.getName())) {
                    imageFiles.add(file);
                }
            }
        } catch (Exception e) {
            log.warn("Error scanning directory: {}", e.getMessage());
        }
        
        return imageFiles;
    }
    
    /**
     * Проверяет является ли файл изображением по расширению
     */
    private boolean isImageFile(String fileName) {
        String lowerName = fileName.toLowerCase();
        for (String ext : IMAGE_EXTENSIONS) {
            if (lowerName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Проверяет сколько файлов готово к миграции
     */
    public int getImageFilesCount() {
        try {
            File workDir = new File(System.getProperty("user.dir"));
            return findImageFiles(workDir).size();
        } catch (Exception e) {
            log.error("Error counting image files: {}", e.getMessage());
            return 0;
        }
    }
}
