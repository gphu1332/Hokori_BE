package com.hokori.web.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@Configuration
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = false)
public class FirebaseConfig {
    
    @Value("${firebase.project-id}")
    private String projectId;
    
    @Value("${firebase.credentials.path}")
    private String credentialsPath;
    
    @PostConstruct
    public void initializeFirebase() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                // Try to load from classpath first
                String resourcePath = credentialsPath.startsWith("classpath:") ? 
                    credentialsPath.substring("classpath:".length()) : credentialsPath;
                
                Resource resource = new ClassPathResource(resourcePath);
                if (resource.exists()) {
                    InputStream serviceAccount = resource.getInputStream();
                    
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                            .setProjectId(projectId)
                            .build();
                    
                    FirebaseApp.initializeApp(options);
                    System.out.println("✅ Firebase initialized successfully with project: " + projectId);
                } else {
                    System.out.println("❌ Firebase service account file not found at: " + resourcePath);
                    System.out.println("Firebase features will be disabled. Username/password auth will still work.");
                }
            } else {
                System.out.println("✅ Firebase already initialized");
            }
        } catch (Exception e) {
            System.out.println("❌ Failed to initialize Firebase: " + e.getMessage());
            System.out.println("Firebase features will be disabled. Username/password auth will still work.");
            e.printStackTrace();
        }
    }
    
    @Bean
    public FirebaseAuth firebaseAuth() {
        try {
            return FirebaseAuth.getInstance();
        } catch (Exception e) {
            System.out.println("Firebase Auth not available: " + e.getMessage());
            return null;
        }
    }
}
