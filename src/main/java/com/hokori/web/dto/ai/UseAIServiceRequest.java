package com.hokori.web.dto.ai;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for using AI service (deduct quota)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UseAIServiceRequest {
    
    @NotNull(message = "Service type is required")
    private String serviceType;  // GRAMMAR, KAIWA, PRONUN, CONVERSATION
    
    @Min(value = 1, message = "Amount must be at least 1")
    private Integer amount = 1;  // Default: 1 quota unit
}

