package com.hokori.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Response DTO for Sentence Analysis
 * Contains vocabulary and grammar analysis results
 */
@Schema(description = "Response containing vocabulary and grammar analysis results")
public class SentenceAnalysisResponse {
    @Schema(description = "Original Japanese sentence", example = "私は日本語を勉強しています")
    private String sentence; // Original sentence
    
    @Schema(description = "User's JLPT level", example = "N5")
    private String level; // User's JLPT level
    
    @Schema(description = "List of vocabulary items with detailed information")
    private List<VocabularyItem> vocabulary; // Vocabulary analysis results
    
    @Schema(description = "List of grammar patterns found in the sentence")
    private List<GrammarItem> grammar; // Grammar analysis results

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
}

