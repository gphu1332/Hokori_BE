package com.hokori.web.controller;

import com.hokori.web.entity.FileStorage;
import com.hokori.web.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    /**
     * Serve file từ database
     * Endpoint: GET /files/{filePath}
     * Ví dụ: GET /files/sections/123/uuid.mp4
     */
    @GetMapping("/**")
    public ResponseEntity<byte[]> serveFile(HttpServletRequest request) {
        // Extract filePath from request URI
        // Ví dụ: /files/sections/123/uuid.mp4 -> sections/123/uuid.mp4
        String requestPath = request.getRequestURI();
        String filePath = requestPath.startsWith("/files/") 
            ? requestPath.substring("/files/".length())
            : requestPath.substring(1);
        
        // Remove query string if exists
        if (filePath.contains("?")) {
            filePath = filePath.substring(0, filePath.indexOf("?"));
        }
        
        FileStorage fileStorage = fileStorageService.getFile(filePath);
        
        if (fileStorage == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found: " + filePath);
        }

        // Set content type
        MediaType mediaType = MediaType.parseMediaType(fileStorage.getContentType());
        
        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentLength(fileStorage.getFileSizeBytes());
        
        // Set content disposition for inline display (browser sẽ hiển thị trực tiếp)
        headers.setContentDispositionFormData("inline", fileStorage.getFileName());
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(fileStorage.getFileData());
    }
}

