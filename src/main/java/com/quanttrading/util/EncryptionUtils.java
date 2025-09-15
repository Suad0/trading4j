package com.quanttrading.util;

import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Utility class for encryption operations
 */
@Component
public class EncryptionUtils {

    private final StringEncryptor stringEncryptor;

    @Autowired
    public EncryptionUtils(@Qualifier("jasyptStringEncryptor") StringEncryptor stringEncryptor) {
        this.stringEncryptor = stringEncryptor;
    }

    /**
     * Encrypts a plain text string
     * @param plainText the text to encrypt
     * @return encrypted text in ENC() format
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.trim().isEmpty()) {
            return plainText;
        }
        return "ENC(" + stringEncryptor.encrypt(plainText) + ")";
    }

    /**
     * Decrypts an encrypted string
     * @param encryptedText the encrypted text (with or without ENC() wrapper)
     * @return decrypted plain text
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.trim().isEmpty()) {
            return encryptedText;
        }
        
        // Remove ENC() wrapper if present
        String textToDecrypt = encryptedText;
        if (encryptedText.startsWith("ENC(") && encryptedText.endsWith(")")) {
            textToDecrypt = encryptedText.substring(4, encryptedText.length() - 1);
        }
        
        return stringEncryptor.decrypt(textToDecrypt);
    }

    /**
     * Checks if a string is encrypted (wrapped with ENC())
     * @param text the text to check
     * @return true if the text appears to be encrypted
     */
    public boolean isEncrypted(String text) {
        return text != null && text.startsWith("ENC(") && text.endsWith(")");
    }
}