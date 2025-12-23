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
    
    @Schema(description = "Different usages of the same grammar pattern with different meanings in different contexts", 
            example = "[{\"usage\": \"ように (purpose/goal)\", \"meaning\": \"Để làm gì đó\", \"example\": \"日本語が話せるように勉強します\"}, {\"usage\": \"ように (like/similar)\", \"meaning\": \"Giống như\", \"example\": \"鳥のように飛びます\"}]")
    private List<UsageVariation> usageVariations; // Different usages of the same grammar pattern

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

    public List<UsageVariation> getUsageVariations() {
        return usageVariations;
    }

    public void setUsageVariations(List<UsageVariation> usageVariations) {
        this.usageVariations = usageVariations;
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

    /**
     * Nested class for different usages of the same grammar pattern
     */
    @Schema(description = "Different usage of the same grammar pattern with different meaning")
    public static class UsageVariation {
        @Schema(description = "Specific usage form or context", example = "ように (purpose/goal)")
        private String usage; // Specific usage form or context
        
        @Schema(description = "Meaning of this usage in Vietnamese", 
                example = "Để làm gì đó (mục đích)")
        private String meaning; // Meaning of this usage in Vietnamese
        
        @Schema(description = "Example sentence showing this usage", 
                example = "日本語が話せるように勉強します")
        private String example; // Example sentence showing this usage
        
        @Schema(description = "When to use this variation (explanation in Vietnamese)", 
                example = "Dùng khi muốn diễn đạt mục đích hoặc mục tiêu")
        private String whenToUse; // When to use this variation

        public UsageVariation() {}

        public UsageVariation(String usage, String meaning, String example) {
            this.usage = usage;
            this.meaning = meaning;
            this.example = example;
        }

        public String getUsage() {
            return usage;
        }

        public void setUsage(String usage) {
            this.usage = usage;
        }

        public String getMeaning() {
            return meaning;
        }

        public void setMeaning(String meaning) {
            this.meaning = meaning;
        }

        public String getExample() {
            return example;
        }

        public void setExample(String example) {
            this.example = example;
        }

        public String getWhenToUse() {
            return whenToUse;
        }

        public void setWhenToUse(String whenToUse) {
            this.whenToUse = whenToUse;
        }
    }
}

