package com.securecrypt.config;

import com.securecrypt.service.AwsS3StorageService;
import com.securecrypt.service.FileStorageService;
import com.securecrypt.service.LocalFileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Dynamic configuration for FileStorageService beans depending on AWS S3 setting flags.
 */
@Configuration
public class StorageConfig {

    @Value("${aws.s3.enabled:false}")
    private boolean s3Enabled;

    @Value("${aws.s3.bucket:securecrypt-blobs}")
    private String bucketName;

    @Value("${aws.s3.region:us-east-1}")
    private String region;

    @Value("${aws.s3.access-key:}")
    private String accessKey;

    @Value("${aws.s3.secret-key:}")
    private String secretKey;

    @Value("${storage.local.dir:data/s3-sim}")
    private String localDir;

    @Bean
    public FileStorageService fileStorageService() {
        if (s3Enabled) {
            AwsCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            S3Client s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .build();
            return new AwsS3StorageService(s3Client, bucketName);
        } else {
            return new LocalFileStorageService(localDir);
        }
    }
}
