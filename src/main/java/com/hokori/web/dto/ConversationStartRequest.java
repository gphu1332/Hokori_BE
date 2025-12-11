package com.hokori.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for starting a conversation practice session
 */
@Schema(description = "Request for starting a conversation practice session")
public class ConversationStartRequest {
    
    @NotBlank(message = "Level is required")
    @Schema(description = "User's JLPT level", 
            example = "N5", 
            allowableValues = {"N5", "N4", "N3", "N2", "N1"}, 
            required = true)
    private String level; // JLPT level: N5, N4, N3, N2, N1
    
    @NotBlank(message = "Scenario is required")
    @Schema(description = "Conversation scenario/topic. Can be in Vietnamese (e.g., 'nhà hàng', 'mua sắm') or Japanese (e.g., 'レストラン', 'ショッピング'). Will be automatically detected and normalized.", 
            example = "restaurant", 
            required = true)
    private String scenario; // Conversation scenario (can be Vietnamese or Japanese)
    
    public ConversationStartRequest() {}
    
    public ConversationStartRequest(String level, String scenario) {
        this.level = level;
        this.scenario = scenario;
    }
    
    public String getLevel() {
        return level;
    }
    
    public void setLevel(String level) {
        this.level = level;
    }
    
    public String getScenario() {
        return scenario;
    }
    
    public void setScenario(String scenario) {
        this.scenario = scenario;
    }
    
    public boolean isValidLevel() {
        if (level == null || level.isEmpty()) {
            return false;
        }
        String upperLevel = level.toUpperCase();
        return upperLevel.equals("N5") ||
               upperLevel.equals("N4") ||
               upperLevel.equals("N3") ||
               upperLevel.equals("N2") ||
               upperLevel.equals("N1");
    }
}

