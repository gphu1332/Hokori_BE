package com.hokori.web.dto;

/**
 * DTO for grammar pattern in sentence analysis
 */
public class GrammarItem {
    private String pattern; // Grammar pattern name
    private String jlptLevel; // JLPT level: N5, N4, N3, N2, N1
    private String explanationVi; // Explanation in Vietnamese
    private String example; // Example sentence
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

