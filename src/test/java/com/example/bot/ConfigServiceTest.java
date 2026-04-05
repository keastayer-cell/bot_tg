package com.example.bot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConfigServiceTest {

    private ConfigService configService;

    @Mock
    private RecipientRepository recipientRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        configService = new ConfigService();
        configService.recipientRepository = recipientRepository;
    }

    @Test
    void testAddRecipient() {
        when(recipientRepository.findByRecipientId("123")).thenReturn(Optional.empty());
        configService.addRecipient("123");
        verify(recipientRepository, times(1)).save(any(Recipient.class));
    }

    @Test
    void testGetRecipients() {
        List<Recipient> recipients = new ArrayList<>();
        Recipient r1 = new Recipient();
        r1.setRecipientId("123");
        recipients.add(r1);
        
        when(recipientRepository.findAll()).thenReturn(recipients);
        List<String> result = configService.getRecipients();
        
        assertEquals(1, result.size());
        assertEquals("123", result.get(0));
    }

    @Test
    void testRemoveRecipient() {
        Recipient r = new Recipient();
        r.setRecipientId("123");
        when(recipientRepository.findByRecipientId("123")).thenReturn(Optional.of(r));
        
        configService.removeRecipient("123");
        verify(recipientRepository, times(1)).delete(r);
    }

    @Test
    void testIsRecipient() {
        Recipient r = new Recipient();
        r.setRecipientId("123");
        when(recipientRepository.findByRecipientId("123")).thenReturn(Optional.of(r));
        when(recipientRepository.findByRecipientId("456")).thenReturn(Optional.empty());
        
        assertTrue(configService.isRecipient("123"));
        assertFalse(configService.isRecipient("456"));
    }
}
