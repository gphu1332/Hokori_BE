package com.hokori.web.dto.asset;

import com.hokori.web.Enum.AssetStatus;
import com.hokori.web.Enum.AssetType;
import com.hokori.web.Enum.AssetVisibility;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class AssetResponse {
    private Long id;
    private AssetType type;
    private String title;
    private String description;
    private String fileName;
    private String mimeType;
    private Long sizeBytes;
    private String publicUrl;
    private AssetVisibility visibility;
    private Integer durationSec;
    private Integer width;
    private Integer height;
    private AssetStatus status;
    private Instant createdAt;
}
