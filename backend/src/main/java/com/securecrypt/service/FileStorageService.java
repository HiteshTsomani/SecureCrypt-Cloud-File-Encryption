package com.securecrypt.service;

/**
 * Storage service interface defining CRUD operations for encrypted binary blobs.
 */
public interface FileStorageService {
    
    /**
     * Stores the encrypted binary payload under a unique key.
     */
    void store(String key, byte[] content) throws Exception;
    
    /**
     * Retrieves the encrypted binary payload by its key.
     */
    byte[] retrieve(String key) throws Exception;
    
    /**
     * Deletes the encrypted binary payload by its key.
     */
    void delete(String key) throws Exception;
}
