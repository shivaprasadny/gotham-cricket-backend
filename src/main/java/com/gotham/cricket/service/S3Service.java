package com.gotham.cricket.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.*;

import java.time.Duration;

@Service
public class S3Service {

    @Value("${aws.s3.profile-bucket}")
    private String bucket;

    @Value("${aws.region}")
    private String region;

    // Generates a pre-signed PUT URL for uploading a profile image directly from the frontend
    public String generateUploadUrl(String key, String contentType, int expiryMinutes) {
        try (S3Presigner presigner = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build()) {

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expiryMinutes))
                    .putObjectRequest(putRequest)
                    .build();

            return presigner.presignPutObject(presignRequest).url().toString();
        }
    }

    // Generates a pre-signed GET URL so the frontend can display the image without making the bucket public
    public String generateDownloadUrl(String key, int expiryMinutes) {
        if (key == null || key.isBlank()) return null;
        try (S3Presigner presigner = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build()) {

            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expiryMinutes))
                    .getObjectRequest(getRequest)
                    .build();

            return presigner.presignGetObject(presignRequest).url().toString();
        }
    }

    // Deletes the object from S3 if it exists
    public void deleteObject(String key) {
        if (key == null || key.isBlank()) return;
        try (S3Client client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build()) {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        }
    }
}
