package com.hokori.web.service;

import com.hokori.web.entity.FileStorage;
import com.hokori.web.repository.FileStorageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

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
    public String store(MultipartFile file, String subFolder) {
        try {
            // Generate unique file name
            String original = file.getOriginalFilename();
            String ext = "";
            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID() + ext;
            String filePath = subFolder + "/" + fileName;

            // Check if file already exists
            if (fileStorageRepository.existsByFilePathAndDeletedFlagFalse(filePath)) {
                // If exists, generate new UUID
                fileName = UUID.randomUUID() + ext;
                filePath = subFolder + "/" + fileName;
            }

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

            return filePath;
        } catch (IOException e) {
            throw new RuntimeException("Cannot store file", e);
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

