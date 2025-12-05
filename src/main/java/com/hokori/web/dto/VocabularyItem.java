package com.hokori.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * DTO for vocabulary item in sentence analysis
 */
@Schema(description = "Vocabulary item with detailed information")
public class VocabularyItem {
    @Schema(description = "Japanese word", example = "私")
    private String word; // Japanese word
    
    @Schema(description = "Reading in hiragana", example = "わたし")
    private String reading; // Reading in hiragana
    
    @Schema(description = "Meaning in Vietnamese", example = "tôi")
    private String meaningVi; // Meaning in Vietnamese
    
    @Schema(description = "JLPT level", example = "N5", allowableValues = {"N5", "N4", "N3", "N2", "N1"})
    private String jlptLevel; // JLPT level: N5, N4, N3, N2, N1
    
    @Schema(description = "Kanji details (if word contains kanji)")
    private KanjiDetails kanjiDetails; // Kanji details (if applicable)
    
    @Schema(description = "Importance level based on user's JLPT level", 
            example = "high", 
            allowableValues = {"high", "medium", "low"})
    private String importance; // "high", "medium", "low" based on user's level
    
    @Schema(description = "Example sentences using this vocabulary (in Vietnamese)", 
            example = "[\"私は学生です。\", \"私の本です。\"]")
    private List<String> examples; // Example sentences using this vocabulary
    
    @Schema(description = "Kanji variants (if word is hiragana, suggest kanji; if kanji, show alternative writings)", 
            example = "[\"私\", \"わたし\"]")
    private List<String> kanjiVariants; // Kanji alternatives (hiragana → kanji suggestions)

    public VocabularyItem() {}

    public VocabularyItem(String word, String reading, String meaningVi, String jlptLevel) {
        this.word = word;
        this.reading = reading;
        this.meaningVi = meaningVi;
        this.jlptLevel = jlptLevel;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getReading() {
        return reading;
    }

    public void setReading(String reading) {
        this.reading = reading;
    }

    public String getMeaningVi() {
        return meaningVi;
    }

    public void setMeaningVi(String meaningVi) {
        this.meaningVi = meaningVi;
    }

    public String getJlptLevel() {
        return jlptLevel;
    }

    public void setJlptLevel(String jlptLevel) {
        this.jlptLevel = jlptLevel;
    }

    public KanjiDetails getKanjiDetails() {
        return kanjiDetails;
    }

    public void setKanjiDetails(KanjiDetails kanjiDetails) {
        this.kanjiDetails = kanjiDetails;
    }

    public String getImportance() {
        return importance;
    }

    public void setImportance(String importance) {
        this.importance = importance;
    }

    /**
     * Nested class for Kanji details
     */
    @Schema(description = "Kanji details for vocabulary items containing kanji")
    public static class KanjiDetails {
        @Schema(description = "Radical", example = "禾")
        private String radical; // Radical
        
        @Schema(description = "Stroke count", example = "7")
        private Integer strokeCount; // Stroke count
        
        @Schema(description = "Onyomi reading", example = "シ")
        private String onyomi; // Onyomi reading
        
        @Schema(description = "Kunyomi reading", example = "わたし")
        private String kunyomi; // Kunyomi reading
        
        @Schema(description = "Related compound words", example = "[\"私的\", \"私立\"]")
        private List<String> relatedWords; // Related compound words

        public KanjiDetails() {}

        public String getRadical() {
            return radical;
        }

        public void setRadical(String radical) {
            this.radical = radical;
        }

        public Integer getStrokeCount() {
            return strokeCount;
        }

        public void setStrokeCount(Integer strokeCount) {
            this.strokeCount = strokeCount;
        }

        public String getOnyomi() {
            return onyomi;
        }

        public void setOnyomi(String onyomi) {
            this.onyomi = onyomi;
        }

        public String getKunyomi() {
            return kunyomi;
        }

        public void setKunyomi(String kunyomi) {
            this.kunyomi = kunyomi;
        }

        public List<String> getRelatedWords() {
            return relatedWords;
        }

        public void setRelatedWords(List<String> relatedWords) {
            this.relatedWords = relatedWords;
        }
    }

    public List<String> getExamples() {
        return examples;
    }

    public void setExamples(List<String> examples) {
        this.examples = examples;
    }

    public List<String> getKanjiVariants() {
        return kanjiVariants;
    }

    public void setKanjiVariants(List<String> kanjiVariants) {
        this.kanjiVariants = kanjiVariants;
    }
}

