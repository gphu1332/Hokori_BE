package com.hokori.web.service;

import com.hokori.web.config.R2Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Service để upload/download files từ Cloudflare R2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class R2Service {

    private final S3Client s3Client;
    private final R2Config r2Config;

    /**
     * Upload file lên R2
     * @param fileData File bytes
     * @param filePath Đường dẫn trong bucket (ví dụ: "sections/123/uuid.mp4")
     * @param contentType MIME type (ví dụ: "video/mp4")
     * @return Public URL của file
     */
    public String uploadFile(byte[] fileData, String filePath, String contentType) {
        try {
            String bucketName = r2Config.getBucketName();
            if (bucketName == null || bucketName.isEmpty()) {
                throw new IllegalStateException("R2 bucket name is not configured");
            }

            // Upload file to R2
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileData));

            // Generate public URL
            String publicUrl = r2Config.getPublicUrl();
            if (publicUrl == null || publicUrl.isEmpty()) {
                // Fallback: construct URL from endpoint
                throw new IllegalStateException("R2 public URL is not configured");
            }

            // Ensure publicUrl ends with /
            if (!publicUrl.endsWith("/")) {
                publicUrl += "/";
            }

            // URL encode filePath for public URL
            String encodedPath = URLEncoder.encode(filePath, StandardCharsets.UTF_8)
                    .replace("+", "%20"); // Replace + with %20 for spaces

            String fullUrl = publicUrl + encodedPath;
            log.info("✅ File uploaded to R2 successfully: {} -> {}", filePath, fullUrl);
            return fullUrl;

        } catch (Exception e) {
            log.error("Failed to upload file to R2: {}", filePath, e);
            throw new RuntimeException("Failed to upload file to R2: " + e.getMessage(), e);
        }
    }

    /**
     * Download file từ R2
     * @param filePath Đường dẫn trong bucket
     * @return File bytes hoặc null nếu không tìm thấy
     */
    public byte[] downloadFile(String filePath) {
        try {
            String bucketName = r2Config.getBucketName();
            if (bucketName == null || bucketName.isEmpty()) {
                throw new IllegalStateException("R2 bucket name is not configured");
            }

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .build();

            try (InputStream inputStream = s3Client.getObject(getObjectRequest)) {
                return inputStream.readAllBytes();
            }

        } catch (NoSuchKeyException e) {
            log.debug("File not found in R2: {}", filePath);
            return null;
        } catch (Exception e) {
            log.error("Failed to download file from R2: {}", filePath, e);
            throw new RuntimeException("Failed to download file from R2: " + e.getMessage(), e);
        }
    }

    /**
     * Xóa file từ R2
     * @param filePath Đường dẫn trong bucket
     */
    public void deleteFile(String filePath) {
        try {
            String bucketName = r2Config.getBucketName();
            if (bucketName == null || bucketName.isEmpty()) {
                throw new IllegalStateException("R2 bucket name is not configured");
            }

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.debug("File deleted from R2: {}", filePath);

        } catch (Exception e) {
            log.error("Failed to delete file from R2: {}", filePath, e);
            throw new RuntimeException("Failed to delete file from R2: " + e.getMessage(), e);
        }
    }

    /**
     * Kiểm tra file có tồn tại trong R2 không
     * @param filePath Đường dẫn trong bucket
     * @return true nếu file tồn tại
     */
    public boolean fileExists(String filePath) {
        try {
            String bucketName = r2Config.getBucketName();
            if (bucketName == null || bucketName.isEmpty()) {
                return false;
            }

            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .build();

            s3Client.headObject(headObjectRequest);
            return true;

        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.error("Failed to check file existence in R2: {}", filePath, e);
            return false;
        }
    }

    /**
     * Generate public URL cho file (không cần download)
     * @param filePath Đường dẫn trong bucket
     * @return Public URL
     */
    public String getPublicUrl(String filePath) {
        String publicUrl = r2Config.getPublicUrl();
        if (publicUrl == null || publicUrl.isEmpty()) {
            throw new IllegalStateException("R2 public URL is not configured");
        }

        if (!publicUrl.endsWith("/")) {
            publicUrl += "/";
        }

        // URL encode filePath
        String encodedPath = URLEncoder.encode(filePath, StandardCharsets.UTF_8)
                .replace("+", "%20");

        return publicUrl + encodedPath;
    }
}

