package com.hokori.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

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
    
    @Schema(description = "Example sentences using this grammar pattern", 
            example = "[\"本を読みます。\", \"コーヒーを飲みます。\"]")
    private List<String> examples; // Example sentences using this grammar pattern
    
    @Schema(description = "Confusing patterns at the same JLPT level that might be mistaken", 
            example = "[{\"pattern\": \"は + verb\", \"difference\": \"は marks topic, を marks object\"}]")
    private List<ConfusingPattern> confusingPatterns; // Patterns that might be confused with this one

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

    public List<String> getExamples() {
        return examples;
    }

    public void setExamples(List<String> examples) {
        this.examples = examples;
    }

    public List<ConfusingPattern> getConfusingPatterns() {
        return confusingPatterns;
    }

    public void setConfusingPatterns(List<ConfusingPattern> confusingPatterns) {
        this.confusingPatterns = confusingPatterns;
    }

    /**
     * Nested class for confusing grammar patterns
     */
    @Schema(description = "Grammar pattern that might be confused with the main pattern")
    public static class ConfusingPattern {
        @Schema(description = "Pattern name that might be confused", example = "は + verb")
        private String pattern; // Pattern name
        
        @Schema(description = "Difference explanation in Vietnamese", 
                example = "は đánh dấu chủ đề, を đánh dấu tân ngữ trực tiếp")
        private String difference; // Difference explanation in Vietnamese
        
        @Schema(description = "Example showing the difference", example = "私は本を読みます。")
        private String example; // Example sentence

        public ConfusingPattern() {}

        public ConfusingPattern(String pattern, String difference, String example) {
            this.pattern = pattern;
            this.difference = difference;
            this.example = example;
        }

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }

        public String getDifference() {
            return difference;
        }

        public void setDifference(String difference) {
            this.difference = difference;
        }

        public String getExample() {
            return example;
        }

        public void setExample(String example) {
            this.example = example;
        }
    }
}

