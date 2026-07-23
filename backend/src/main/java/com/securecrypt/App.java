package com.securecrypt;

import com.securecrypt.crypto.SecureCryptCrypto;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Verification and demonstration application for Milestone 1.
 */
public class App {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   SecureCrypt - Milestone 1 Cryptography Demo   ");
        System.out.println("=================================================");
        System.out.println();

        String originalText = "Hello World";
        byte[] plaintextBytes = originalText.getBytes(StandardCharsets.UTF_8);

        System.out.println("Original String : " + originalText);
        System.out.println("Plaintext Hex   : " + bytesToHex(plaintextBytes));
        System.out.println();

        // -------------------------------------------------------------
        // TEST 1: Password-Based Encryption / Decryption
        // -------------------------------------------------------------
        System.out.println("--- [TEST 1] Password-Based Encryption (PBKDF2 + AES-256-GCM) ---");
        char[] password = "MySuperSecretPassword@123".toCharArray();
        System.out.println("Password        : " + new String(password));

        try {
            // Encrypt
            byte[] encryptedPayload = SecureCryptCrypto.encryptPasswordBased(plaintextBytes, password);
            System.out.println("Payload Length  : " + encryptedPayload.length + " bytes");
            System.out.println("Full Payload Hex: " + bytesToHex(encryptedPayload));
            
            // Print breakdown
            breakdownPasswordPayload(encryptedPayload);

            // Decrypt with correct password
            byte[] decryptedBytes = SecureCryptCrypto.decryptPasswordBased(encryptedPayload, password);
            String decryptedText = new String(decryptedBytes, StandardCharsets.UTF_8);
            System.out.println("Decrypted Text  : " + decryptedText);
            
            if (originalText.equals(decryptedText)) {
                System.out.println("Result          : SUCCESS (Decrypted text matches original!)");
            } else {
                System.out.println("Result          : FAILURE (Decrypted text does NOT match!)");
            }

            // Test decryption with WRONG password
            System.out.println("\nTesting wrong password decryption...");
            char[] wrongPassword = "WrongPassword".toCharArray();
            try {
                SecureCryptCrypto.decryptPasswordBased(encryptedPayload, wrongPassword);
                System.out.println("Result          : FAILURE (Decryption succeeded with WRONG password!)");
            } catch (Exception e) {
                System.out.println("Result          : SUCCESS (Decryption failed with WRONG password as expected: " + e.getMessage() + ")");
            }

        } catch (Exception e) {
            System.err.println("Test 1 encountered an unexpected error:");
            e.printStackTrace();
        }

        System.out.println();

        // -------------------------------------------------------------
        // TEST 2: Key-Based Encryption / Decryption
        // -------------------------------------------------------------
        System.out.println("--- [TEST 2] Key-Based Encryption (Raw 256-bit Key + AES-256-GCM) ---");
        
        try {
            // Generate a random key
            byte[] rawKey = SecureCryptCrypto.generateRandomKey();
            System.out.println("Generated Key   : " + bytesToHex(rawKey));

            // Encrypt
            byte[] encryptedPayload = SecureCryptCrypto.encryptKeyBased(plaintextBytes, rawKey);
            System.out.println("Payload Length  : " + encryptedPayload.length + " bytes");
            System.out.println("Full Payload Hex: " + bytesToHex(encryptedPayload));

            // Print breakdown
            breakdownKeyPayload(encryptedPayload);

            // Decrypt with correct key
            byte[] decryptedBytes = SecureCryptCrypto.decryptKeyBased(encryptedPayload, rawKey);
            String decryptedText = new String(decryptedBytes, StandardCharsets.UTF_8);
            System.out.println("Decrypted Text  : " + decryptedText);

            if (originalText.equals(decryptedText)) {
                System.out.println("Result          : SUCCESS (Decrypted text matches original!)");
            } else {
                System.out.println("Result          : FAILURE (Decrypted text does NOT match!)");
            }

            // Test decryption with WRONG key
            System.out.println("\nTesting wrong key decryption...");
            byte[] wrongKey = SecureCryptCrypto.generateRandomKey();
            try {
                SecureCryptCrypto.decryptKeyBased(encryptedPayload, wrongKey);
                System.out.println("Result          : FAILURE (Decryption succeeded with WRONG key!)");
            } catch (Exception e) {
                System.out.println("Result          : SUCCESS (Decryption failed with WRONG key as expected: " + e.getMessage() + ")");
            }

        } catch (Exception e) {
            System.err.println("Test 2 encountered an unexpected error:");
            e.printStackTrace();
        }

        System.out.println();
        System.out.println("=================================================");
        System.out.println("               Demonstration End                 ");
        System.out.println("=================================================");
    }

    private static void breakdownPasswordPayload(byte[] payload) {
        if (payload.length < 35) return;
        
        byte[] magic = Arrays.copyOfRange(payload, 0, 6);
        byte version = payload[6];
        byte[] salt = Arrays.copyOfRange(payload, 7, 23);
        byte[] iv = Arrays.copyOfRange(payload, 23, 35);
        byte[] ciphertext = Arrays.copyOfRange(payload, 35, payload.length);
        
        System.out.println("\nPayload Breakdown (V1 Password-based):");
        System.out.println("  - Magic Header (6B) : " + new String(magic, StandardCharsets.US_ASCII) + " (" + bytesToHex(magic) + ")");
        System.out.println("  - Version / Mode (1B): 0x" + String.format("%02X", version));
        System.out.println("  - Salt (16B)        : " + bytesToHex(salt));
        System.out.println("  - IV (12B)          : " + bytesToHex(iv));
        System.out.println("  - Ciphertext + Tag  : " + bytesToHex(ciphertext));
        System.out.println();
    }

    private static void breakdownKeyPayload(byte[] payload) {
        if (payload.length < 19) return;

        byte[] magic = Arrays.copyOfRange(payload, 0, 6);
        byte version = payload[6];
        byte[] iv = Arrays.copyOfRange(payload, 7, 19);
        byte[] ciphertext = Arrays.copyOfRange(payload, 19, payload.length);

        System.out.println("\nPayload Breakdown (V2 Key-based):");
        System.out.println("  - Magic Header (6B) : " + new String(magic, StandardCharsets.US_ASCII) + " (" + bytesToHex(magic) + ")");
        System.out.println("  - Version / Mode (1B): 0x" + String.format("%02X", version));
        System.out.println("  - IV (12B)          : " + bytesToHex(iv));
        System.out.println("  - Ciphertext + Tag  : " + bytesToHex(ciphertext));
        System.out.println();
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    
    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}
