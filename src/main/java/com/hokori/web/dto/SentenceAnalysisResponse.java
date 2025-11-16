package com.hokori.web.dto;

import java.util.List;

/**
 * Response DTO for Sentence Analysis
 * Contains vocabulary and grammar analysis results
 */
public class SentenceAnalysisResponse {
    private String sentence; // Original sentence
    private String level; // User's JLPT level
    private List<VocabularyItem> vocabulary; // Vocabulary analysis results
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

