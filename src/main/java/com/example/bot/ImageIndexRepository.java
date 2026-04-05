package com.example.bot;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ImageIndexRepository extends JpaRepository<ImageIndex, Long> {
    List<ImageIndex> findAll();
    Optional<ImageIndex> findByFileId(String fileId);
    void deleteByFileId(String fileId);
}