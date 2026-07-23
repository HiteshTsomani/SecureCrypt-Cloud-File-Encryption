package com.securecrypt.service;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Production implementation of FileStorageService connecting to AWS S3.
 */
public class AwsS3StorageService implements FileStorageService {

    private final S3Client s3Client;
    private final String bucketName;

    public AwsS3StorageService(S3Client s3Client, String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        System.out.println("[AWS S3] Initialized S3 Storage Service client for bucket: " + bucketName);
    }

    @Override
    public void store(String key, byte[] content) throws Exception {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType("application/octet-stream")
                .build();
        
        s3Client.putObject(putRequest, RequestBody.fromBytes(content));
    }

    @Override
    public byte[] retrieve(String key) throws Exception {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        
        ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObjectAsBytes(getRequest);
        return responseBytes.asByteArray();
    }

    @Override
    public void delete(String key) throws Exception {
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        
        s3Client.deleteObject(deleteRequest);
    }
}
