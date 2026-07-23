package com.securecrypt.repository;

import com.securecrypt.model.FileMetadata;
import com.securecrypt.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Spring Data JPA repository for FileMetadata entity operations.
 */
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {
    List<FileMetadata> findByUser(User user);
}
