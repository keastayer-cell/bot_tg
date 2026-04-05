package com.example.bot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigServiceTest {

    private ConfigService configService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Создаём временные файлы
        File configFileTemp = tempDir.resolve("config.properties").toFile();
        File recipientsFileTemp = tempDir.resolve("recipients.txt").toFile();

        configService = new ConfigService() {
            {
                this.configFile = configFileTemp.getAbsolutePath();
                this.recipientsFile = recipientsFileTemp.getAbsolutePath();
            }
        };
    }

    @Test
    void testGetAndSet() {
        configService.set("testKey", "testValue");
        assertEquals("testValue", configService.get("testKey", "default"));
        assertEquals("default", configService.get("nonExistent", "default"));
    }

    @Test
    void testRecipients() {
        assertTrue(configService.getRecipients().isEmpty());

        configService.addRecipient("123");
        configService.addRecipient("456");

        List<String> recipients = configService.getRecipients();
        assertEquals(2, recipients.size());
        assertTrue(recipients.contains("123"));
        assertTrue(recipients.contains("456"));

        configService.removeRecipient("123");
        recipients = configService.getRecipients();
        assertEquals(1, recipients.size());
        assertFalse(recipients.contains("123"));
        assertTrue(recipients.contains("456"));
    }

    @Test
    void testTimezone() {
        assertEquals("Europe/Moscow", configService.getTimezone()); // default

        configService.setTimezone("UTC");
        assertEquals("UTC", configService.getTimezone());
    }
}