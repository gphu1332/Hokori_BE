package com.hokori.web.dto.course;

import com.hokori.web.Enum.JLPTLevel;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CourseUpsertReq {
    @NotBlank private String title;
    private String subtitle;
    private String description;
    private JLPTLevel level;   // N5..N1
    private Long priceCents;
    private Long discountedPriceCents;
    private String currency;
    private Long coverAssetId;
}
