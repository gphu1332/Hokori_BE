package com.hokori.web.entity;

import com.hokori.web.Enum.AssetStatus;
import com.hokori.web.Enum.AssetType;
import com.hokori.web.Enum.AssetVisibility;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.Instant;

@Entity
@Table(name = "assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE assets SET deleted_flag = 1 WHERE id = ?")
@Where(clause = "deleted_flag = 0")
public class Asset {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AssetType type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_assets_owner"))
    private User owner;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(length = 20)
    private String extension;

    @Column(length = 100)
    private String mimeType;

    private Long sizeBytes;

    @Column(length = 64)
    private String checksumSha256;

    @Column(nullable = false, length = 30)
    private String storageProvider; // LOCAL/S3/...

    @Column(nullable = false, length = 500)
    private String relativePath;

    @Column(length = 1000)
    private String publicUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssetVisibility visibility;

    private Integer durationSec;
    private Integer width;
    private Integer height;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssetStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private boolean deletedFlag = false;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = AssetStatus.READY;
        if (visibility == null) visibility = AssetVisibility.PRIVATE;
        if (storageProvider == null) storageProvider = "LOCAL";
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }
}

