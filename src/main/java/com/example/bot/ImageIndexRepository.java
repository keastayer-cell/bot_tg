package com.example.bot;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ImageIndexRepository extends JpaRepository<ImageIndex, Long> {
    List<ImageIndex> findAll();
}