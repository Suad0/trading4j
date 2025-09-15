package com.quanttrading.util;

import org.jasypt.encryption.StringEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilsTest {

    @Mock
    private StringEncryptor stringEncryptor;

    private EncryptionUtils encryptionUtils;

    @BeforeEach
    void setUp() {
        encryptionUtils = new EncryptionUtils(stringEncryptor);
    }

    @Test
    void encrypt_WithValidPlainText_ShouldReturnEncryptedText() {
        // Given
        String plainText = "secret-password";
        String encryptedValue = "encrypted-value-123";
        when(stringEncryptor.encrypt(plainText)).thenReturn(encryptedValue);

        // When
        String result = encryptionUtils.encrypt(plainText);

        // Then
        assertEquals("ENC(" + encryptedValue + ")", result);
    }

    @Test
    void encrypt_WithNullText_ShouldReturnNull() {
        // When
        String result = encryptionUtils.encrypt(null);

        // Then
        assertNull(result);
    }

    @Test
    void encrypt_WithEmptyText_ShouldReturnEmptyText() {
        // When
        String result = encryptionUtils.encrypt("");

        // Then
        assertEquals("", result);
    }

    @Test
    void decrypt_WithEncryptedText_ShouldReturnPlainText() {
        // Given
        String encryptedValue = "encrypted-value-123";
        String encryptedText = "ENC(" + encryptedValue + ")";
        String plainText = "secret-password";
        when(stringEncryptor.decrypt(encryptedValue)).thenReturn(plainText);

        // When
        String result = encryptionUtils.decrypt(encryptedText);

        // Then
        assertEquals(plainText, result);
    }

    @Test
    void decrypt_WithoutEncWrapper_ShouldDecryptDirectly() {
        // Given
        String encryptedValue = "encrypted-value-123";
        String plainText = "secret-password";
        when(stringEncryptor.decrypt(encryptedValue)).thenReturn(plainText);

        // When
        String result = encryptionUtils.decrypt(encryptedValue);

        // Then
        assertEquals(plainText, result);
    }

    @Test
    void decrypt_WithNullText_ShouldReturnNull() {
        // When
        String result = encryptionUtils.decrypt(null);

        // Then
        assertNull(result);
    }

    @Test
    void decrypt_WithEmptyText_ShouldReturnEmptyText() {
        // When
        String result = encryptionUtils.decrypt("");

        // Then
        assertEquals("", result);
    }

    @Test
    void isEncrypted_WithEncryptedText_ShouldReturnTrue() {
        // Given
        String encryptedText = "ENC(encrypted-value-123)";

        // When
        boolean result = encryptionUtils.isEncrypted(encryptedText);

        // Then
        assertTrue(result);
    }

    @Test
    void isEncrypted_WithPlainText_ShouldReturnFalse() {
        // Given
        String plainText = "plain-text";

        // When
        boolean result = encryptionUtils.isEncrypted(plainText);

        // Then
        assertFalse(result);
    }

    @Test
    void isEncrypted_WithNullText_ShouldReturnFalse() {
        // When
        boolean result = encryptionUtils.isEncrypted(null);

        // Then
        assertFalse(result);
    }

    @Test
    void isEncrypted_WithIncompleteEncWrapper_ShouldReturnFalse() {
        // Given
        String incompleteText = "ENC(incomplete";

        // When
        boolean result = encryptionUtils.isEncrypted(incompleteText);

        // Then
        assertFalse(result);
    }
}