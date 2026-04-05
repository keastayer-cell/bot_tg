package com.example.bot;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface QueueItemRepository extends JpaRepository<QueueItemEntity, Long> {
    List<QueueItemEntity> findAll();
    Optional<QueueItemEntity> findFirstByOrderByIdAsc();
}