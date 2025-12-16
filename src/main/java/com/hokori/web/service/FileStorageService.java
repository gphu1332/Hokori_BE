package com.hokori.web.service;

import com.hokori.web.entity.FileStorage;
import com.hokori.web.repository.FileStorageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final FileStorageRepository fileStorageRepository;
    private final R2Service r2Service;

    /**
     * Lưu file lên Cloudflare R2 và lưu metadata vào database
     * @param file MultipartFile từ request
     * @param subFolder Thư mục con (ví dụ: "sections/123")
     * @return filePath tương đối để lưu vào SectionsContent
     */
    @Transactional
    public String store(MultipartFile file, String subFolder) {
        try {
            // Validate file
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("File is empty or null");
            }
            
            // Validate file size (max 100MB as configured in application.yml)
            // Note: Cloudflare Free plan has ~100MB upload limit
            long maxSize = 100L * 1024 * 1024; // 100MB
            if (file.getSize() > maxSize) {
                throw new IllegalArgumentException("File size exceeds maximum limit of 100MB");
            }
            
            // Generate unique file name
            String original = file.getOriginalFilename();
            String ext = "";
            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf("."));
            }
            
            // Retry loop to ensure unique filePath (handle race condition)
            String filePath;
            int maxRetries = 10;
            int retryCount = 0;
            
            do {
                String fileName = UUID.randomUUID() + ext;
                filePath = subFolder + "/" + fileName;
                retryCount++;
                
                // Check if file already exists
                if (fileStorageRepository.existsByFilePathAndDeletedFlagFalse(filePath)) {
                    if (retryCount >= maxRetries) {
                        throw new RuntimeException("Failed to generate unique file path after " + maxRetries + " attempts");
                    }
                    // Retry with new UUID
                    continue;
                }
                
                // File path is unique, break loop
                break;
            } while (retryCount < maxRetries);

            // Read file bytes
            byte[] fileData = file.getBytes();
            String contentType = file.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // Upload file to R2
            String fileUrl = r2Service.uploadFile(fileData, filePath, contentType);
            log.info("File uploaded to R2: {} -> {}", filePath, fileUrl);

            // Create FileStorage entity (chỉ lưu metadata, không lưu binary data)
            FileStorage fileStorage = new FileStorage();
            fileStorage.setFilePath(filePath);
            fileStorage.setFileName(original);
            fileStorage.setContentType(contentType);
            fileStorage.setFileUrl(fileUrl); // Lưu URL từ R2
            fileStorage.setFileSizeBytes(file.getSize());

            // Save metadata to database
            fileStorageRepository.save(fileStorage);
            fileStorageRepository.flush();
            
            log.debug("File metadata stored successfully: {} ({} bytes)", filePath, file.getSize());
            return filePath;
        } catch (IllegalArgumentException e) {
            throw e; // Re-throw validation errors
        } catch (IOException e) {
            throw new RuntimeException("Cannot store file: " + e.getMessage(), e);
        }
    }

    /**
     * Lấy file metadata từ database theo filePath
     * @param filePath Đường dẫn tương đối
     * @return FileStorage entity hoặc null nếu không tìm thấy
     */
    public FileStorage getFile(String filePath) {
        return fileStorageRepository.findByFilePathAndDeletedFlagFalse(filePath)
                .orElse(null);
    }

    /**
     * Lấy file bytes từ R2 (fallback về database nếu file cũ chưa migrate)
     * @param filePath Đường dẫn tương đối
     * @return File bytes hoặc null nếu không tìm thấy
     */
    public byte[] getFileBytes(String filePath) {
        FileStorage fileStorage = getFile(filePath);
        if (fileStorage == null) {
            return null;
        }

        // Nếu có URL từ R2, download từ R2
        if (fileStorage.getFileUrl() != null && !fileStorage.getFileUrl().isEmpty()) {
            return r2Service.downloadFile(filePath);
        }

        // Fallback: file cũ chưa migrate, lấy từ database
        if (fileStorage.getFileData() != null) {
            log.debug("Using legacy file data from database for: {}", filePath);
            return fileStorage.getFileData();
        }

        return null;
    }

    /**
     * Xóa file từ R2 và database (soft delete)
     * @param filePath Đường dẫn tương đối
     */
    public void deleteFile(String filePath) {
        fileStorageRepository.findByFilePathAndDeletedFlagFalse(filePath)
                .ifPresent(file -> {
                    // Xóa file từ R2 nếu có URL
                    if (file.getFileUrl() != null && !file.getFileUrl().isEmpty()) {
                        try {
                            r2Service.deleteFile(filePath);
                            log.debug("File deleted from R2: {}", filePath);
                        } catch (Exception e) {
                            log.warn("Failed to delete file from R2: {}", filePath, e);
                            // Continue với soft delete trong database
                        }
                    }

                    // Soft delete trong database
                    file.setDeletedFlag(true);
                    fileStorageRepository.save(file);
                });
    }

    /**
     * Lấy public URL của file
     * @param filePath Đường dẫn tương đối
     * @return Public URL hoặc null nếu không tìm thấy
     */
    public String getFileUrl(String filePath) {
        FileStorage fileStorage = getFile(filePath);
        if (fileStorage == null) {
            return null;
        }

        // Nếu có URL từ R2, return URL đó
        if (fileStorage.getFileUrl() != null && !fileStorage.getFileUrl().isEmpty()) {
            return fileStorage.getFileUrl();
        }

        // Fallback: generate URL từ R2 (cho file cũ chưa migrate)
        return r2Service.getPublicUrl(filePath);
    }
}

