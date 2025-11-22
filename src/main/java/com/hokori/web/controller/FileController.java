package com.hokori.web.controller;

import com.hokori.web.entity.FileStorage;
import com.hokori.web.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("/files")
@CrossOrigin(origins = "*")
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
        try {
            // Extract filePath from request URI
            // Ví dụ: /files/sections/123/uuid.mp4 -> sections/123/uuid.mp4
            String requestPath = request.getRequestURI();
            log.debug("File request path: {}", requestPath);
            
            // Handle case: /files (no path after)
            if (requestPath.equals("/files") || requestPath.equals("/files/")) {
                log.warn("Invalid file request: empty path");
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File path is required");
            }
            
            String filePath = requestPath.startsWith("/files/") 
                ? requestPath.substring("/files/".length())
                : requestPath.substring(1);
            
            // Remove query string if exists
            if (filePath.contains("?")) {
                filePath = filePath.substring(0, filePath.indexOf("?"));
            }
            
            log.debug("Looking for file with path: {}", filePath);
            
            FileStorage fileStorage = fileStorageService.getFile(filePath);
            
            if (fileStorage == null) {
                log.warn("File not found: {}", filePath);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found: " + filePath);
            }

            log.debug("File found: {} ({} bytes, type: {})", filePath, fileStorage.getFileSizeBytes(), fileStorage.getContentType());

            // Set content type
            MediaType mediaType;
            try {
                mediaType = MediaType.parseMediaType(fileStorage.getContentType());
            } catch (Exception e) {
                log.warn("Invalid content type: {}, using application/octet-stream", fileStorage.getContentType());
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(mediaType);
            headers.setContentLength(fileStorage.getFileSizeBytes());
            
            // Set content disposition for inline display (browser sẽ hiển thị trực tiếp)
            if (fileStorage.getFileName() != null) {
                headers.setContentDispositionFormData("inline", fileStorage.getFileName());
            }
            
            // Add cache control headers
            headers.setCacheControl("public, max-age=3600"); // Cache for 1 hour
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileStorage.getFileData());
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error serving file", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error serving file: " + e.getMessage());
        }
    }
}

