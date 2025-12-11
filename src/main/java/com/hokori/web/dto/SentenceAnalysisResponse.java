package com.hokori.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Response DTO for Sentence Analysis
 * Contains vocabulary and grammar analysis results
 */
@Schema(description = "Response containing vocabulary and grammar analysis results")
public class SentenceAnalysisResponse {
    @Schema(description = "Japanese sentence to analyze (may be translated from Vietnamese)", example = "私は日本語を勉強しています")
    private String sentence; // Japanese sentence to analyze
    
    @Schema(description = "Original input sentence (if translated, this is the Vietnamese input)", example = "Tôi đang học tiếng Nhật")
    private String originalSentence; // Original input sentence (if translated from Vietnamese)
    
    @Schema(description = "Whether the sentence was translated from Vietnamese", example = "true")
    private Boolean isTranslated; // Whether the sentence was translated from Vietnamese
    
    @Schema(description = "User's JLPT level", example = "N5")
    private String level; // User's JLPT level
    
    @Schema(description = "List of vocabulary items with detailed information")
    private List<VocabularyItem> vocabulary; // Vocabulary analysis results
    
    @Schema(description = "List of grammar patterns found in the sentence")
    private List<GrammarItem> grammar; // Grammar analysis results
    
    @Schema(description = "Detailed sentence breakdown (sentence structure analysis)", 
            example = "{\"subject\": \"私\", \"predicate\": \"勉強しています\", \"particles\": [\"は\", \"を\"]}")
    private SentenceBreakdown sentenceBreakdown; // Detailed sentence structure analysis
    
    @Schema(description = "Related example sentences for better understanding", 
            example = "[\"私は英語を勉強しています。\", \"彼は日本語を勉強しています。\"]")
    private List<String> relatedSentences; // Related example sentences

    public SentenceAnalysisResponse() {}

    public SentenceAnalysisResponse(String sentence, String level) {
        this.sentence = sentence;
        this.level = level;
    }

    public String getSentence() {
        return sentence;
    }

    public void setSentence(String sentence) {
        this.sentence = sentence;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public List<VocabularyItem> getVocabulary() {
        return vocabulary;
    }

    public void setVocabulary(List<VocabularyItem> vocabulary) {
        this.vocabulary = vocabulary;
    }

    public List<GrammarItem> getGrammar() {
        return grammar;
    }

    public void setGrammar(List<GrammarItem> grammar) {
        this.grammar = grammar;
    }

    public SentenceBreakdown getSentenceBreakdown() {
        return sentenceBreakdown;
    }

    public void setSentenceBreakdown(SentenceBreakdown sentenceBreakdown) {
        this.sentenceBreakdown = sentenceBreakdown;
    }

    public List<String> getRelatedSentences() {
        return relatedSentences;
    }

    public void setRelatedSentences(List<String> relatedSentences) {
        this.relatedSentences = relatedSentences;
    }

    public String getOriginalSentence() {
        return originalSentence;
    }

    public void setOriginalSentence(String originalSentence) {
        this.originalSentence = originalSentence;
    }

    public Boolean getIsTranslated() {
        return isTranslated;
    }

    public void setIsTranslated(Boolean isTranslated) {
        this.isTranslated = isTranslated;
    }

    /**
     * Nested class for sentence breakdown
     */
    @Schema(description = "Detailed breakdown of sentence structure")
    public static class SentenceBreakdown {
        @Schema(description = "Subject of the sentence", example = "私")
        private String subject; // Subject
        
        @Schema(description = "Predicate/verb", example = "勉強しています")
        private String predicate; // Predicate/verb
        
        @Schema(description = "Particles used in the sentence", example = "[\"は\", \"を\"]")
        private List<String> particles; // Particles
        
        @Schema(description = "Object (if any)", example = "日本語")
        private String object; // Object
        
        @Schema(description = "Detailed explanation in Vietnamese", 
                example = "Câu này có cấu trúc: Chủ ngữ (私) + Trợ từ chủ đề (は) + Tân ngữ (日本語) + Trợ từ tân ngữ (を) + Động từ (勉強しています)")
        private String explanationVi; // Detailed explanation in Vietnamese

        public SentenceBreakdown() {}

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getPredicate() {
            return predicate;
        }

        public void setPredicate(String predicate) {
            this.predicate = predicate;
        }

        public List<String> getParticles() {
            return particles;
        }

        public void setParticles(List<String> particles) {
            this.particles = particles;
        }

        public String getObject() {
            return object;
        }

        public void setObject(String object) {
            this.object = object;
        }

        public String getExplanationVi() {
            return explanationVi;
        }

        public void setExplanationVi(String explanationVi) {
            this.explanationVi = explanationVi;
        }
    }
}

