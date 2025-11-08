package com.hokori.web.dto.asset;

import com.google.firebase.database.annotations.NotNull;
import com.hokori.web.Enum.AssetType;
import com.hokori.web.Enum.AssetVisibility;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssetCreateRequest {
    @NotNull
    private AssetType type;
    @NotBlank
    private String title;
    private String description;
    @NotBlank private String fileName;
    private String extension;
    private String mimeType;
    private Long sizeBytes;
    private String checksumSha256;
    @NotBlank private String relativePath;
    private String publicUrl;
    private AssetVisibility visibility;  // default PRIVATE
    private Integer durationSec;
    private Integer width;
    private Integer height;
}
