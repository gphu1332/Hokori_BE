package com.hokori.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
// dto/TeacherQualificationUpdateRequest.java
public class TeacherQualificationUpdateRequest {
    @Size(max = 100) private String highestDegree;
    @Size(max = 255) private String major;
    @Min(0) @Max(60) private Integer yearsOfExperience;
    @Size(max = 10000) private String certifications; // JSON/String
    @Size(max = 10000) private String evidenceUrls;
}

