package com.hokori.web.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_sentiment_history")
public class AiSentimentHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id")
    private Long userId;
    
//    @Lob
    @Column(name = "text", columnDefinition = "TEXT")
    private String text;
    
    @Size(max = 50)
    @Column(name = "sentiment_score")
    private String sentimentScore;
    
    @Size(max = 50)
    @Column(name = "magnitude")
    private String magnitude;
    
//    @Lob
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
    
    // Constructors
    public AiSentimentHistory() {}
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public String getSentimentScore() {
        return sentimentScore;
    }
    
    public void setSentimentScore(String sentimentScore) {
        this.sentimentScore = sentimentScore;
    }
    
    public String getMagnitude() {
        return magnitude;
    }
    
    public void setMagnitude(String magnitude) {
        this.magnitude = magnitude;
    }
    
    public String getDetails() {
        return details;
    }
    
    public void setDetails(String details) {
        this.details = details;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

