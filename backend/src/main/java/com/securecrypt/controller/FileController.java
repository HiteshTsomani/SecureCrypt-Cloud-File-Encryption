package com.securecrypt.controller;

import com.securecrypt.model.FileMetadata;
import com.securecrypt.model.User;
import com.securecrypt.repository.FileMetadataRepository;
import com.securecrypt.service.FileStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ContentDisposition;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller managing zero-knowledge file metadata storage and listings.
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileMetadataRepository fileMetadataRepository;
    private final FileStorageService fileStorageService;

    public FileController(FileMetadataRepository fileMetadataRepository, FileStorageService fileStorageService) {
        this.fileMetadataRepository = fileMetadataRepository;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Uploads an encrypted file binary to storage and registers its metadata.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("originalFilename") String originalFilename,
            @RequestParam("algorithm") String algorithm) {
        try {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("Uploaded file cannot be empty.");
            }

            // Retrieve current authenticated principal User object
            User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

            // Generate a random UUID storage key
            String storageKey = UUID.randomUUID().toString() + ".scf";

            // Store binary blob in storage (S3 or local sim)
            fileStorageService.store(storageKey, file.getBytes());

            // Save metadata record pointing to the storage key
            FileMetadata metadata = new FileMetadata(
                    originalFilename,
                    storageKey,
                    file.getSize(),
                    algorithm,
                    LocalDateTime.now(),
                    currentUser
            );

            fileMetadataRepository.save(metadata);

            Map<String, String> response = new HashMap<>();
            response.put("message", "File encrypted binary uploaded and recorded successfully!");
            response.put("id", metadata.getId().toString());
            response.put("storageKey", storageKey);

            return new ResponseEntity<>(response, HttpStatus.CREATED);

        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to upload file to storage: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Downloads an encrypted file binary from storage.
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<?> downloadFile(@PathVariable("id") Long id) {
        try {
            // Retrieve current authenticated principal User object
            User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

            // Find metadata record
            FileMetadata metadata = fileMetadataRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("File metadata record not found."));

            // Verify ownership
            if (!metadata.getUser().getId().equals(currentUser.getId())) {
                return new ResponseEntity<>(Map.of("error", "Access denied: You do not own this file."), HttpStatus.FORBIDDEN);
            }

            // Retrieve binary from S3/Storage
            byte[] fileContent = fileStorageService.retrieve(metadata.getEncryptedFilename());

            // Stream file back
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename(metadata.getOriginalFilename() + ".scf")
                            .build().toString())
                    .body(fileContent);

        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to download file: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Deletes an encrypted file binary from storage and removes its metadata record.
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteFile(@PathVariable("id") Long id) {
        try {
            // Retrieve current authenticated principal User object
            User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

            // Find metadata record
            FileMetadata metadata = fileMetadataRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("File metadata record not found."));

            // Verify ownership
            if (!metadata.getUser().getId().equals(currentUser.getId())) {
                return new ResponseEntity<>(Map.of("error", "Access denied: You do not own this file."), HttpStatus.FORBIDDEN);
            }

            // Delete binary from S3/Storage
            fileStorageService.delete(metadata.getEncryptedFilename());

            // Delete metadata row from DB
            fileMetadataRepository.delete(metadata);

            Map<String, String> response = new HashMap<>();
            response.put("message", "File permanently deleted from cloud storage and database.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to delete file: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Registers a new file metadata record for the authenticated user.
     */
    @PostMapping("/upload-metadata")
    public ResponseEntity<?> uploadMetadata(@RequestBody MetadataRequest request) {
        try {
            // Retrieve current authenticated principal User object
            User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

            FileMetadata metadata = new FileMetadata(
                    request.getOriginalFilename(),
                    request.getEncryptedFilename(),
                    request.getFileSize(),
                    request.getAlgorithm(),
                    LocalDateTime.now(),
                    currentUser
            );

            fileMetadataRepository.save(metadata);

            Map<String, String> response = new HashMap<>();
            response.put("message", "File metadata recorded successfully!");
            response.put("id", metadata.getId().toString());
            return new ResponseEntity<>(response, HttpStatus.CREATED);

        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to record file metadata: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Lists all file metadata records associated with the authenticated user.
     */
    @GetMapping("/list-metadata")
    public ResponseEntity<?> listMetadata() {
        try {
            User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

            List<FileMetadataResponse> list = fileMetadataRepository.findByUser(currentUser).stream()
                    .map(metadata -> new FileMetadataResponse(
                            metadata.getId(),
                            metadata.getOriginalFilename(),
                            metadata.getEncryptedFilename(),
                            metadata.getFileSize(),
                            metadata.getAlgorithm(),
                            metadata.getUploadDate()
                    ))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(list);

        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to fetch metadata list: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Static request DTO
    public static class MetadataRequest {
        private String originalFilename;
        private String encryptedFilename;
        private Long fileSize;
        private String algorithm;

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
    }

    // Static response DTO
    public static class FileMetadataResponse {
        private Long id;
        private String originalFilename;
        private String encryptedFilename;
        private Long fileSize;
        private String algorithm;
        private LocalDateTime uploadDate;

        public FileMetadataResponse(Long id, String originalFilename, String encryptedFilename, Long fileSize, String algorithm, LocalDateTime uploadDate) {
            this.id = id;
            this.originalFilename = originalFilename;
            this.encryptedFilename = encryptedFilename;
            this.fileSize = fileSize;
            this.algorithm = algorithm;
            this.uploadDate = uploadDate;
        }

        public Long getId() {
            return id;
        }

        public String getOriginalFilename() {
            return originalFilename;
        }

        public String getEncryptedFilename() {
            return encryptedFilename;
        }

        public Long getFileSize() {
            return fileSize;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        public LocalDateTime getUploadDate() {
            return uploadDate;
        }
    }
}
