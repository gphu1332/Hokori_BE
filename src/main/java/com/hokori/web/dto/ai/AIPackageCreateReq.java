package com.hokori.web.dto.ai;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating an AI Package
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIPackageCreateReq {

    @NotBlank(message = "Package name is required")
    @Size(max = 100, message = "Package name must not exceed 100 characters")
    private String name;  // e.g., "Plus", "Pro"

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price must be non-negative")
    private Long priceCents;  // Price in cents (VND)

    private String currency = "VND";

    @NotNull(message = "Duration days is required")
    @Min(value = 1, message = "Duration must be at least 1 day")
    private Integer durationDays;  // e.g., 30 = 1 month

    private Integer grammarQuota;      // null = unlimited
    private Integer kaiwaQuota;        // null = unlimited
    private Integer pronunQuota;       // null = unlimited
    private Integer conversationQuota; // null = unlimited

    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private Integer displayOrder = 0;
}

