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

    /**
     * Lưu file vào PostgreSQL database
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

            // Create FileStorage entity
            FileStorage fileStorage = new FileStorage();
            fileStorage.setFilePath(filePath);
            fileStorage.setFileName(original);
            fileStorage.setContentType(contentType);
            fileStorage.setFileData(fileData);
            fileStorage.setFileSizeBytes(file.getSize());

            // Save to database
            fileStorageRepository.save(fileStorage);
            fileStorageRepository.flush(); // Ensure file is persisted immediately, especially for large files
            
            log.debug("File stored successfully: {} ({} bytes)", filePath, file.getSize());
            return filePath;
        } catch (IllegalArgumentException e) {
            throw e; // Re-throw validation errors
        } catch (IOException e) {
            throw new RuntimeException("Cannot store file: " + e.getMessage(), e);
        }
    }

    /**
     * Lấy file từ database theo filePath
     * @param filePath Đường dẫn tương đối
     * @return FileStorage entity hoặc null nếu không tìm thấy
     */
    public FileStorage getFile(String filePath) {
        return fileStorageRepository.findByFilePathAndDeletedFlagFalse(filePath)
                .orElse(null);
    }

    /**
     * Xóa file (soft delete)
     * @param filePath Đường dẫn tương đối
     */
    public void deleteFile(String filePath) {
        fileStorageRepository.findByFilePathAndDeletedFlagFalse(filePath)
                .ifPresent(file -> {
                    file.setDeletedFlag(true);
                    fileStorageRepository.save(file);
                });
    }
}

