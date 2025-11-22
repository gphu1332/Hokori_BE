package com.hokori.web.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "file_storage", indexes = {
    @Index(name = "idx_file_storage_path", columnList = "file_path"),
    @Index(name = "idx_file_storage_deleted", columnList = "deleted_flag")
})
@Getter
@Setter
public class FileStorage extends BaseEntity {

    /**
     * Đường dẫn tương đối (giữ để backward compatibility)
     * Ví dụ: "sections/123/uuid.mp4"
     */
    @Column(name = "file_path", length = 500, nullable = false, unique = true)
    private String filePath;

    /**
     * Tên file gốc khi upload
     */
    @Column(name = "file_name", length = 255)
    private String fileName;

    /**
     * MIME type (ví dụ: "video/mp4", "image/jpeg", "application/pdf")
     */
    @Column(name = "content_type", length = 100)
    private String contentType;

    /**
     * Binary data của file
     * PostgreSQL sẽ dùng BYTEA type
     * Note: Không dùng @Lob với PostgreSQL BYTEA, chỉ cần columnDefinition
     */
    @Column(name = "file_data", nullable = false, columnDefinition = "BYTEA")
    private byte[] fileData;

    /**
     * Kích thước file tính bằng bytes
     */
    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;
}

