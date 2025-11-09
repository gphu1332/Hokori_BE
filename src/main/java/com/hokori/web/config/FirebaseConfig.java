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
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = false)
public class FirebaseConfig {
    
    @Value("${firebase.project-id}")
    private String projectId;
    
    @Value("${firebase.credentials.path:}")
    private String credentialsPath;
    
    // Environment variables for Railway (alternative to JSON file)
    @Value("${firebase.private-key:}")
    private String privateKey;
    
    @Value("${firebase.private-key-id:}")
    private String privateKeyId;
    
    @Value("${firebase.client-email:}")
    private String clientEmail;
    
    @Value("${firebase.client-id:}")
    private String clientId;
    
    @Value("${firebase.client-x509-cert-url:}")
    private String clientX509CertUrl;
    
    @PostConstruct
    public void initializeFirebase() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                GoogleCredentials credentials = null;
                
                // Option 1: Load from environment variables (for Railway)
                if (privateKey != null && !privateKey.isEmpty() && 
                    clientEmail != null && !clientEmail.isEmpty()) {
                    try {
                        String jsonCredentials = buildFirebaseJsonFromEnv();
                        InputStream credentialsStream = new ByteArrayInputStream(
                            jsonCredentials.getBytes(StandardCharsets.UTF_8));
                        credentials = GoogleCredentials.fromStream(credentialsStream);
                        System.out.println("✅ Firebase credentials loaded from environment variables");
                    } catch (Exception e) {
                        System.out.println("⚠️ Failed to load Firebase from env vars, trying file: " + e.getMessage());
                    }
                }
                
                // Option 2: Load from JSON file (for local dev)
                if (credentials == null && credentialsPath != null && !credentialsPath.isEmpty()) {
                    String resourcePath = credentialsPath.startsWith("classpath:") ? 
                        credentialsPath.substring("classpath:".length()) : credentialsPath;
                    
                    Resource resource = new ClassPathResource(resourcePath);
                    if (resource.exists()) {
                        InputStream serviceAccount = resource.getInputStream();
                        credentials = GoogleCredentials.fromStream(serviceAccount);
                        System.out.println("✅ Firebase credentials loaded from file: " + resourcePath);
                    }
                }
                
                // Initialize Firebase
                if (credentials != null) {
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(credentials)
                            .setProjectId(projectId)
                            .build();
                    
                    FirebaseApp.initializeApp(options);
                    System.out.println("✅ Firebase initialized successfully with project: " + projectId);
                } else {
                    System.out.println("❌ Firebase credentials not found. Please configure:");
                    System.out.println("   - Upload firebase-service-account.json to Railway, OR");
                    System.out.println("   - Set FIREBASE_* environment variables");
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
    
    private String buildFirebaseJsonFromEnv() {
        // Escape JSON string properly
        String escapedPrivateKey = privateKey != null ? 
            privateKey.replace("\\", "\\\\")
                     .replace("\"", "\\\"")
                     .replace("\n", "\\n")
                     .replace("\r", "\\r") : "";
        
        return String.format(
            "{\n" +
            "  \"type\": \"service_account\",\n" +
            "  \"project_id\": \"%s\",\n" +
            "  \"private_key_id\": \"%s\",\n" +
            "  \"private_key\": \"%s\",\n" +
            "  \"client_email\": \"%s\",\n" +
            "  \"client_id\": \"%s\",\n" +
            "  \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",\n" +
            "  \"token_uri\": \"https://oauth2.googleapis.com/token\",\n" +
            "  \"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\",\n" +
            "  \"client_x509_cert_url\": \"%s\",\n" +
            "  \"universe_domain\": \"googleapis.com\"\n" +
            "}",
            projectId != null ? projectId : "",
            privateKeyId != null ? privateKeyId : "",
            escapedPrivateKey,
            clientEmail != null ? clientEmail : "",
            clientId != null ? clientId : "",
            clientX509CertUrl != null ? clientX509CertUrl : ""
        );
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
