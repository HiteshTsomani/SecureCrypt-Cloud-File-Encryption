package com.securecrypt.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Core cryptographic operations utility for SecureCrypt zero-knowledge platform.
 * Supports AES-256-GCM encryption and decryption.
 */
public class SecureCryptCrypto {

    private static final String ALGORITHM = "AES";
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    
    // GCM requirements
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    
    // PBKDF2 requirements
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int PBKDF2_ITERATIONS = 65536;
    private static final int PBKDF2_KEY_LENGTH_BITS = 256;
    private static final int PBKDF2_SALT_LENGTH_BYTES = 16;

    // Custom format magic bytes: "SCRYPT"
    private static final byte[] MAGIC_HEADER = "SCRYPT".getBytes(StandardCharsets.US_ASCII);
    private static final byte VERSION_PASSWORD_BASED = 0x01;
    private static final byte VERSION_KEY_BASED = 0x02;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Generates a cryptographically secure random 256-bit AES key.
     */
    public static byte[] generateRandomKey() {
        byte[] keyBytes = new byte[32]; // 256 bits
        SECURE_RANDOM.nextBytes(keyBytes);
        return keyBytes;
    }

    /**
     * Derives a 256-bit AES key from a password and salt using PBKDF2 with HMAC-SHA256.
     */
    public static byte[] deriveKeyFromPassword(char[] password, byte[] salt) throws Exception {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            KeySpec spec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH_BITS);
            SecretKey tmp = factory.generateSecret(spec);
            return tmp.getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Failed to derive key from password", e);
        }
    }

    /**
     * Encrypts plaintext bytes using a password-derived key.
     * Output format (SCRYPT V1): SCRYPT (6 bytes) | 0x01 (1 byte) | Salt (16 bytes) | IV (12 bytes) | Ciphertext (variable)
     */
    public static byte[] encryptPasswordBased(byte[] plaintext, char[] password) throws Exception {
        // Compress plaintext before encryption
        byte[] compressedPlaintext = compress(plaintext);

        // 1. Generate salt and IV
        byte[] salt = new byte[PBKDF2_SALT_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(salt);

        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(iv);

        // 2. Derive key from password
        byte[] derivedKeyBytes = deriveKeyFromPassword(password, salt);
        SecretKey key = new SecretKeySpec(derivedKeyBytes, ALGORITHM);
        
        // Clear derived key bytes from memory if possible
        Arrays.fill(derivedKeyBytes, (byte) 0);

        // 3. Perform encryption
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        byte[] ciphertext = cipher.doFinal(compressedPlaintext);

        // 4. Assemble the packet
        ByteBuffer buffer = ByteBuffer.allocate(MAGIC_HEADER.length + 1 + salt.length + iv.length + ciphertext.length);
        buffer.put(MAGIC_HEADER);
        buffer.put(VERSION_PASSWORD_BASED);
        buffer.put(salt);
        buffer.put(iv);
        buffer.put(ciphertext);

        return buffer.array();
    }

    /**
     * Decrypts an SCRYPT V1 password-based packet.
     */
    public static byte[] decryptPasswordBased(byte[] payload, char[] password) throws Exception {
        // 1. Validate payload length (magic + version + salt + iv + authTag)
        int minLength = MAGIC_HEADER.length + 1 + PBKDF2_SALT_LENGTH_BYTES + GCM_IV_LENGTH_BYTES + (GCM_TAG_LENGTH_BITS / 8);
        if (payload == null || payload.length < minLength) {
            throw new IllegalArgumentException("Payload is too short to be a valid SCRYPT password-based format.");
        }

        ByteBuffer buffer = ByteBuffer.wrap(payload);

        // 2. Validate magic header
        byte[] magic = new byte[MAGIC_HEADER.length];
        buffer.get(magic);
        if (!Arrays.equals(magic, MAGIC_HEADER)) {
            throw new IllegalArgumentException("Invalid magic header.");
        }

        // 3. Validate version
        byte version = buffer.get();
        if (version != VERSION_PASSWORD_BASED) {
            throw new IllegalArgumentException("Invalid version/mode for password-based decryption.");
        }

        // 4. Extract salt and IV
        byte[] salt = new byte[PBKDF2_SALT_LENGTH_BYTES];
        buffer.get(salt);

        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        buffer.get(iv);

        // 5. Extract ciphertext
        byte[] ciphertext = new byte[buffer.remaining()];
        buffer.get(ciphertext);

        // 6. Derive key
        byte[] derivedKeyBytes = deriveKeyFromPassword(password, salt);
        SecretKey key = new SecretKeySpec(derivedKeyBytes, ALGORITHM);
        Arrays.fill(derivedKeyBytes, (byte) 0);

        // 7. Perform decryption
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        byte[] decryptedBytes = cipher.doFinal(ciphertext);
        
        // Decompress the decrypted bytes
        return decompress(decryptedBytes);
    }

    /**
     * Encrypts plaintext bytes using a raw 256-bit AES key.
     * Output format (SCRYPT V2): SCRYPT (6 bytes) | 0x02 (1 byte) | IV (12 bytes) | Ciphertext (variable)
     */
    public static byte[] encryptKeyBased(byte[] plaintext, byte[] keyBytes) throws Exception {
        if (keyBytes == null || keyBytes.length != 32) {
            throw new IllegalArgumentException("AES key must be exactly 256 bits (32 bytes).");
        }

        // Compress plaintext
        byte[] compressedPlaintext = compress(plaintext);

        // 1. Generate IV
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(iv);

        SecretKey key = new SecretKeySpec(keyBytes, ALGORITHM);

        // 2. Perform encryption
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        byte[] ciphertext = cipher.doFinal(compressedPlaintext);

        // 3. Assemble the packet
        ByteBuffer buffer = ByteBuffer.allocate(MAGIC_HEADER.length + 1 + iv.length + ciphertext.length);
        buffer.put(MAGIC_HEADER);
        buffer.put(VERSION_KEY_BASED);
        buffer.put(iv);
        buffer.put(ciphertext);

        return buffer.array();
    }

    /**
     * Decrypts an SCRYPT V2 key-based packet.
     */
    public static byte[] decryptKeyBased(byte[] payload, byte[] keyBytes) throws Exception {
        if (keyBytes == null || keyBytes.length != 32) {
            throw new IllegalArgumentException("AES key must be exactly 256 bits (32 bytes).");
        }

        // 1. Validate payload length (magic + version + iv + authTag)
        int minLength = MAGIC_HEADER.length + 1 + GCM_IV_LENGTH_BYTES + (GCM_TAG_LENGTH_BITS / 8);
        if (payload == null || payload.length < minLength) {
            throw new IllegalArgumentException("Payload is too short to be a valid SCRYPT key-based format.");
        }

        ByteBuffer buffer = ByteBuffer.wrap(payload);

        // 2. Validate magic header
        byte[] magic = new byte[MAGIC_HEADER.length];
        buffer.get(magic);
        if (!Arrays.equals(magic, MAGIC_HEADER)) {
            throw new IllegalArgumentException("Invalid magic header.");
        }

        // 3. Validate version
        byte version = buffer.get();
        if (version != VERSION_KEY_BASED) {
            throw new IllegalArgumentException("Invalid version/mode for key-based decryption.");
        }

        // 4. Extract IV
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        buffer.get(iv);

        // 5. Extract ciphertext
        byte[] ciphertext = new byte[buffer.remaining()];
        buffer.get(ciphertext);

        SecretKey key = new SecretKeySpec(keyBytes, ALGORITHM);

        // 6. Perform decryption
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        byte[] decryptedBytes = cipher.doFinal(ciphertext);

        // Decompress
        return decompress(decryptedBytes);
    }

    /**
     * Calculates the SHA-256 checksum of a file.
     */
    public static String calculateSHA256(String filePath) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (java.io.InputStream is = Files.newInputStream(Paths.get(filePath))) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        byte[] hash = digest.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString().toUpperCase();
    }

    /**
     * Encrypts a file using a password.
     */
    public static void encryptFilePasswordBased(String inputPath, String outputPath, char[] password) throws Exception {
        byte[] fileBytes = Files.readAllBytes(Paths.get(inputPath));
        byte[] encryptedPayload = encryptPasswordBased(fileBytes, password);
        Files.write(Paths.get(outputPath), encryptedPayload);
    }

    /**
     * Decrypts a file using a password.
     */
    public static void decryptFilePasswordBased(String inputPath, String outputPath, char[] password) throws Exception {
        byte[] encryptedPayload = Files.readAllBytes(Paths.get(inputPath));
        byte[] decryptedBytes = decryptPasswordBased(encryptedPayload, password);
        Files.write(Paths.get(outputPath), decryptedBytes);
    }

    /**
     * Encrypts a file using a raw AES key.
     */
    public static void encryptFileKeyBased(String inputPath, String outputPath, byte[] keyBytes) throws Exception {
        byte[] fileBytes = Files.readAllBytes(Paths.get(inputPath));
        byte[] encryptedPayload = encryptKeyBased(fileBytes, keyBytes);
        Files.write(Paths.get(outputPath), encryptedPayload);
    }

    /**
     * Decrypts a file using a raw AES key.
     */
    public static void decryptFileKeyBased(String inputPath, String outputPath, byte[] keyBytes) throws Exception {
        byte[] encryptedPayload = Files.readAllBytes(Paths.get(inputPath));
        byte[] decryptedBytes = decryptKeyBased(encryptedPayload, keyBytes);
        Files.write(Paths.get(outputPath), decryptedBytes);
    }

    /**
     * Compresses data using GZIP.
     */
    public static byte[] compress(byte[] data) throws java.io.IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            gzip.write(data);
        }
        return bos.toByteArray();
    }

    /**
     * Decompresses data using GZIP.
     */
    public static byte[] decompress(byte[] compressedData) throws java.io.IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(compressedData);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPInputStream gzip = new GZIPInputStream(bis)) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = gzip.read(buffer)) != -1) {
                bos.write(buffer, 0, read);
            }
        }
        return bos.toByteArray();
    }
}
