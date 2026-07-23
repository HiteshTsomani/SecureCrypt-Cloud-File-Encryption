package com.securecrypt.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Local simulation of S3 storage service. Stores file payloads inside a directory on disk.
 */
public class LocalFileStorageService implements FileStorageService {

    private final Path storageDirectory;

    public LocalFileStorageService(String storageDir) {
        this.storageDirectory = Paths.get(storageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageDirectory);
            System.out.println("[SIMULATION] Initialized local simulated S3 storage at: " + this.storageDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize local simulated storage folder: " + storageDir, e);
        }
    }

    @Override
    public void store(String key, byte[] content) throws Exception {
        Path targetPath = this.storageDirectory.resolve(key).normalize();
        if (!targetPath.startsWith(this.storageDirectory)) {
            throw new SecurityException("Security Exception: Attempt to write outside storage directory bounds.");
        }
        Files.write(targetPath, content);
    }

    @Override
    public byte[] retrieve(String key) throws Exception {
        Path targetPath = this.storageDirectory.resolve(key).normalize();
        if (!targetPath.startsWith(this.storageDirectory)) {
            throw new SecurityException("Security Exception: Attempt to read outside storage directory bounds.");
        }
        if (!Files.exists(targetPath)) {
            throw new IOException("File not found in storage: " + key);
        }
        return Files.readAllBytes(targetPath);
    }

    @Override
    public void delete(String key) throws Exception {
        Path targetPath = this.storageDirectory.resolve(key).normalize();
        if (!targetPath.startsWith(this.storageDirectory)) {
            throw new SecurityException("Security Exception: Attempt to delete outside storage directory bounds.");
        }
        Files.deleteIfExists(targetPath);
    }
}
