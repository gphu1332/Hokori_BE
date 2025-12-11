package com.hokori.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for ending a conversation practice session
 * FE sends final conversation history for AI evaluation
 */
@Schema(description = "Request for ending a conversation practice session")
public class ConversationEndRequest {
    
    @NotBlank(message = "Conversation ID is required")
    @Schema(description = "Temporary conversation ID", 
            example = "conv-abc123", 
            required = true)
    private String conversationId; // Temporary ID
    
    @NotNull(message = "Conversation history is required")
    @Schema(description = "Final complete conversation history", 
            example = "[{\"role\":\"ai\",\"text\":\"こんにちは\",\"textVi\":\"Xin chào\"},{\"role\":\"user\",\"text\":\"こんにちは\",\"textVi\":\"Xin chào\"},...]", 
            required = true)
    private List<Map<String, String>> conversationHistory; // Final history
    
    @Schema(description = "JLPT level", 
            example = "N5", 
            allowableValues = {"N5", "N4", "N3", "N2", "N1"})
    private String level; // JLPT level
    
    @Schema(description = "Conversation scenario", 
            example = "restaurant")
    private String scenario; // Conversation scenario
    
    public ConversationEndRequest() {}
    
    public String getConversationId() {
        return conversationId;
    }
    
    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
    
    public List<Map<String, String>> getConversationHistory() {
        return conversationHistory;
    }
    
    public void setConversationHistory(List<Map<String, String>> conversationHistory) {
        this.conversationHistory = conversationHistory;
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
}

