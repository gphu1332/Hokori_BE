package com.hokori.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for grammar pattern in sentence analysis
 */
@Schema(description = "Grammar pattern with explanation and examples")
public class GrammarItem {
    @Schema(description = "Grammar pattern name", example = "を + verb")
    private String pattern; // Grammar pattern name
    
    @Schema(description = "JLPT level", example = "N5", allowableValues = {"N5", "N4", "N3", "N2", "N1"})
    private String jlptLevel; // JLPT level: N5, N4, N3, N2, N1
    
    @Schema(description = "Explanation in Vietnamese", 
            example = "Trợ từ を được dùng để đánh dấu tân ngữ trực tiếp")
    private String explanationVi; // Explanation in Vietnamese
    
    @Schema(description = "Example sentence", example = "本を読みます")
    private String example; // Example sentence
    
    @Schema(description = "Notes and common mistakes in Vietnamese", 
            example = "Lưu ý: Không nhầm với は (chủ đề)")
    private String notes; // Notes and common mistakes

    public GrammarItem() {}

    public GrammarItem(String pattern, String jlptLevel, String explanationVi) {
        this.pattern = pattern;
        this.jlptLevel = jlptLevel;
        this.explanationVi = explanationVi;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getJlptLevel() {
        return jlptLevel;
    }

    public void setJlptLevel(String jlptLevel) {
        this.jlptLevel = jlptLevel;
    }

    public String getExplanationVi() {
        return explanationVi;
    }

    public void setExplanationVi(String explanationVi) {
        this.explanationVi = explanationVi;
    }

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

