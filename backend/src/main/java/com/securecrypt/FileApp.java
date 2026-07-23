package com.securecrypt;

import com.securecrypt.crypto.SecureCryptCrypto;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Verification application for Phase 2: Local File Encryption.
 */
public class FileApp {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   SecureCrypt - Milestone 2 File Crypto Demo   ");
        System.out.println("=================================================");
        System.out.println();

        String dummyPath = "dummy.bin";
        String pngPath = "C:/Users/dudup/.gemini/antigravity/brain/3178b2c3-21be-4c36-be64-818070341e92/.user_uploaded/media__1784640004002.png";

        try {
            // Create directories if not exists (outputs will go in workspace root
            // h:\project 2)
            System.out.println("Setting up test files...");
            createDummyBinaryFile(dummyPath, 5000); // 5000 bytes dummy file
            System.out.println("Dummy binary file generated: " + dummyPath + " (5000 bytes)");
            System.out.println();

            // -------------------------------------------------------------
            // TEST 1: Dummy Binary File Test
            // -------------------------------------------------------------
            System.out.println("-------------------------------------------------");
            System.out.println(" [TEST 1] Testing Dummy Binary File: " + dummyPath);
            System.out.println("-------------------------------------------------");
            runFileTests(dummyPath);
            System.out.println();

            // -------------------------------------------------------------
            // TEST 2: User-Uploaded PNG File Test
            // -------------------------------------------------------------
            System.out.println("-------------------------------------------------");
            System.out.println(" [TEST 2] Testing User Uploaded PNG: " + pngPath);
            System.out.println("-------------------------------------------------");
            if (Files.exists(Paths.get(pngPath))) {
                runFileTests(pngPath);
            } else {
                System.out.println("WARNING: PNG file not found at " + pngPath);
            }
            System.out.println();

            // Cleanup generated temporary outputs (optional but we can keep them for user
            // inspection)
            System.out.println(
                    "Test execution completed. Output encrypted files (.scf) and decrypted files are available in the workspace.");

        } catch (Exception e) {
            System.err.println("File crypto demonstration encountered an error:");
            e.printStackTrace();
        }

        System.out.println();
        System.out.println("=================================================");
        System.out.println("             File Demonstration End              ");
        System.out.println("=================================================");
    }

    private static void runFileTests(String inputFilePath) throws Exception {
        String filename = Paths.get(inputFilePath).getFileName().toString();

        // Compute original file hash
        String originalHash = SecureCryptCrypto.calculateSHA256(inputFilePath);
        long originalSize = Files.size(Paths.get(inputFilePath));
        System.out.println("Input File Name : " + filename);
        System.out.println("Original Size   : " + originalSize + " bytes");
        System.out.println("Original SHA256 : " + originalHash);
        System.out.println();

        char[] password = "StrongPassword@789!".toCharArray();
        String encPasswordPath = filename + ".password.scf";
        String decPasswordPath = "decrypted_pw_" + filename;

        // --- PASSWORD-BASED (V1) ---
        System.out.println("--> Running V1 Password-based File Flow...");
        System.out.println("    Encrypting to: " + encPasswordPath);
        SecureCryptCrypto.encryptFilePasswordBased(inputFilePath, encPasswordPath, password);
        System.out.println("    Encrypted Size: " + Files.size(Paths.get(encPasswordPath)) + " bytes");

        System.out.println("    Decrypting to: " + decPasswordPath);
        SecureCryptCrypto.decryptFilePasswordBased(encPasswordPath, decPasswordPath, password);

        String decPasswordHash = SecureCryptCrypto.calculateSHA256(decPasswordPath);
        System.out.println("    Decrypted SHA256: " + decPasswordHash);

        if (originalHash.equals(decPasswordHash)) {
            System.out.println("    [V1 PASS] Decrypted file integrity verified (SHA-256 matches)!");
        } else {
            System.out.println("    [V1 FAIL] Decrypted file integrity verification failed!");
        }

        // Test wrong password
        try {
            System.out.println("    Testing decryption with invalid password...");
            SecureCryptCrypto.decryptFilePasswordBased(encPasswordPath, "failed_pw_" + filename,
                    "WrongPassword".toCharArray());
            System.out.println("    [V1 FAIL] Decrypted successfully using a WRONG password!");
        } catch (Exception e) {
            System.out.println("    [V1 PASS] Decryption failed with wrong password as expected: " + e.getMessage());
        }
        System.out.println();

        // --- KEY-BASED (V2) ---
        System.out.println("--> Running V2 Key-based File Flow...");
        byte[] rawKey = SecureCryptCrypto.generateRandomKey();
        String keyFilePath = filename + ".sck";
        Files.write(Paths.get(keyFilePath), rawKey);
        System.out.println("    Generated AES Key saved to: " + keyFilePath);

        String encKeyPath = filename + ".key.scf";
        String decKeyPath = "decrypted_key_" + filename;

        System.out.println("    Encrypting to: " + encKeyPath);
        SecureCryptCrypto.encryptFileKeyBased(inputFilePath, encKeyPath, rawKey);
        System.out.println("    Encrypted Size: " + Files.size(Paths.get(encKeyPath)) + " bytes");

        System.out.println("    Decrypting to: " + decKeyPath);
        // Read key from key file to simulate real usage
        byte[] loadedKeyBytes = Files.readAllBytes(Paths.get(keyFilePath));
        SecureCryptCrypto.decryptFileKeyBased(encKeyPath, decKeyPath, loadedKeyBytes);

        String decKeyHash = SecureCryptCrypto.calculateSHA256(decKeyPath);
        System.out.println("    Decrypted SHA256: " + decKeyHash);

        if (originalHash.equals(decKeyHash)) {
            System.out.println("    [V2 PASS] Decrypted file integrity verified (SHA-256 matches)!");
        } else {
            System.out.println("    [V2 FAIL] Decrypted file integrity verification failed!");
        }

        // Test wrong key
        try {
            System.out.println("    Testing decryption with invalid key...");
            byte[] badKey = SecureCryptCrypto.generateRandomKey();
            SecureCryptCrypto.decryptFileKeyBased(encKeyPath, "failed_key_" + filename, badKey);
            System.out.println("    [V2 FAIL] Decrypted successfully using a WRONG key!");
        } catch (Exception e) {
            System.out.println("    [V2 PASS] Decryption failed with wrong key as expected: " + e.getMessage());
        }
    }

    private static void createDummyBinaryFile(String path, int size) throws IOException {
        byte[] dummyBytes = new byte[size];
        for (int i = 0; i < size; i++) {
            dummyBytes[i] = (byte) (i % 256);
        }
        Files.write(Paths.get(path), dummyBytes);
    }
}
