package com.example.bot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.util.List;
import java.util.Optional;

@Component
public class QueueService {
    @Autowired
    private QueueItemRepository queueItemRepository;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private ImageIndexRepository imageIndexRepository;
    @Autowired
    private ImageService imageService;

    public void addText(String text) {
        QueueItemEntity item = new QueueItemEntity();
        item.setRecipient("");
        item.setFileId(text);
        item.setType(QueueItem.Type.TEXT.name());
        queueItemRepository.save(item);
    }

    public void addImage(String imageName) {
        QueueItemEntity item = new QueueItemEntity();
        item.setRecipient("");
        item.setFileId(imageName);
        item.setType(QueueItem.Type.IMAGE.name());
        queueItemRepository.save(item);
    }

    public int getQueueSize() {
        return (int) queueItemRepository.count();
    }

    public int getTextCount() {
        return (int) messageRepository.count();
    }

    public int getImageCount() {
        return (int) imageIndexRepository.count();
    }

    public void addFromTextsAndImages() {
        List<Message> texts = messageRepository.findAll();
        for (Message msg : texts) {
            addText(msg.getText());
        }

        List<ImageIndex> images = imageIndexRepository.findAll();
        for (ImageIndex img : images) {
            addImage(img.getFileId());
        }
    }

    public QueueItem popFirst() {
        Optional<QueueItemEntity> itemOpt = queueItemRepository.findFirstByOrderByIdAsc();
        if (itemOpt.isPresent()) {
            QueueItemEntity item = itemOpt.get();
            queueItemRepository.delete(item);
            QueueItem.Type type = QueueItem.Type.valueOf(item.getType());
            return new QueueItem(type, item.getFileId());
        }
        return null;
    }

    public String getQueueList() {
        StringBuilder sb = new StringBuilder();
        List<QueueItemEntity> items = queueItemRepository.findAll();
        for (int i = 0; i < items.size(); i++) {
            QueueItemEntity entity = items.get(i);
            sb.append(i + 1)
              .append(". ")
              .append(entity.getType())
              .append(" - ")
              .append(entity.getFileId())
              .append("\n");
        }
        return sb.length() == 0 ? "Очередь пуста" : sb.toString();
    }

    public InputFile getImageInputFile(String imageName) {
        return imageService.getImageInputFile(imageName);
    }
}

