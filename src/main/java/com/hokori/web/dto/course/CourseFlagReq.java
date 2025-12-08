package com.hokori.web.dto.course;

import com.hokori.web.Enum.FlagType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseFlagReq {
    @NotNull(message = "Flag type is required")
    private FlagType flagType;
    
    private String reason; // Lý do chi tiết (optional)
}

