package com.hokori.web.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

/**
 * Configuration for Cloudflare R2 (S3-compatible object storage)
 */
@Slf4j
@Configuration
public class R2Config {

    @Value("${cloudflare.r2.account-id:}")
    private String accountId;

    @Value("${cloudflare.r2.access-key-id:}")
    private String accessKeyId;

    @Value("${cloudflare.r2.secret-access-key:}")
    private String secretAccessKey;

    @Value("${cloudflare.r2.endpoint:}")
    private String endpoint;

    @Value("${cloudflare.r2.bucket-name:}")
    private String bucketName;

    @Value("${cloudflare.r2.public-url:}")
    private String publicUrl;

    /**
     * Create S3Client configured for Cloudflare R2
     * R2 uses S3-compatible API, so we can use AWS S3 SDK
     */
    @Bean
    public S3Client s3Client() {
        if (accessKeyId == null || accessKeyId.isEmpty() || 
            secretAccessKey == null || secretAccessKey.isEmpty() ||
            endpoint == null || endpoint.isEmpty()) {
            log.error("❌ Cloudflare R2 configuration is missing!");
            log.error("   Please set: CLOUDFLARE_R2_ACCESS_KEY_ID, CLOUDFLARE_R2_SECRET_ACCESS_KEY, CLOUDFLARE_R2_ENDPOINT");
            throw new IllegalStateException(
                "Cloudflare R2 configuration is missing. " +
                "Please set cloudflare.r2.access-key-id, cloudflare.r2.secret-access-key, and cloudflare.r2.endpoint"
            );
        }

        log.info("✅ Cloudflare R2 configuration loaded successfully");
        log.info("   Endpoint: {}", endpoint);
        log.info("   Bucket: {}", bucketName != null && !bucketName.isEmpty() ? bucketName : "NOT SET");
        log.info("   Public URL: {}", publicUrl != null && !publicUrl.isEmpty() ? publicUrl : "NOT SET");
        log.info("   Access Key ID: {}...{}", 
                accessKeyId.substring(0, Math.min(8, accessKeyId.length())),
                accessKeyId.substring(Math.max(0, accessKeyId.length() - 4)));

        S3Client client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                ))
                .region(Region.US_EAST_1) // R2 doesn't use regions, but SDK requires it
                .build();

        log.info("✅ S3Client for R2 initialized successfully");
        return client;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getPublicUrl() {
        return publicUrl;
    }
}

