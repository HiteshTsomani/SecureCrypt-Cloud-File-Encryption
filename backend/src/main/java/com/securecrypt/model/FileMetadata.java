package com.securecrypt.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA entity representing metadata for an encrypted file uploaded by a user.
 */
@Entity
@Table(name = "file_metadata")
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false)
    private String encryptedFilename; // Unique identifier/path on local disk/S3

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false)
    private String algorithm; // e.g., "AES-256-GCM"

    @Column(nullable = false)
    private LocalDateTime uploadDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public FileMetadata() {
    }

    public FileMetadata(String originalFilename, String encryptedFilename, Long fileSize, String algorithm, LocalDateTime uploadDate, User user) {
        this.originalFilename = originalFilename;
        this.encryptedFilename = encryptedFilename;
        this.fileSize = fileSize;
        this.algorithm = algorithm;
        this.uploadDate = uploadDate;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getEncryptedFilename() {
        return encryptedFilename;
    }

    public void setEncryptedFilename(String encryptedFilename) {
        this.encryptedFilename = encryptedFilename;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
