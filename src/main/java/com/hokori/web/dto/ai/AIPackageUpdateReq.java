package com.hokori.web.dto.ai;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an AI Package
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIPackageUpdateReq {

    @Size(max = 100, message = "Package name must not exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Min(value = 0, message = "Price must be non-negative")
    private Long priceCents;

    private String currency;

    @Min(value = 1, message = "Duration must be at least 1 day")
    private Integer durationDays;

    private Integer grammarQuota;
    private Integer kaiwaQuota;
    private Integer pronunQuota;

    private Boolean isActive;
    private Integer displayOrder;
}

