package com.hokori.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class FirebaseAuthRequest {
    
    @NotBlank(message = "Firebase token is required")
    @Size(max = 2000, message = "Firebase token must not exceed 2000 characters")
    private String firebaseToken;
    
    // Constructors
    public FirebaseAuthRequest() {}
    
    public FirebaseAuthRequest(String firebaseToken) {
        this.firebaseToken = firebaseToken;
    }
    
    // Getters and Setters
    public String getFirebaseToken() {
        return firebaseToken;
    }
    
    public void setFirebaseToken(String firebaseToken) {
        this.firebaseToken = firebaseToken;
    }
    
    @Override
    public String toString() {
        return "FirebaseAuthRequest{" +
                "firebaseToken='" + (firebaseToken != null ? "[TOKEN]" : "null") + '\'' +
                '}';
    }
}
