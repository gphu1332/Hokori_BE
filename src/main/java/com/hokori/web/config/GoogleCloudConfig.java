package com.hokori.web.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.language.v1.LanguageServiceClient;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechSettings;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.TextToSpeechSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Configuration for Google Cloud AI services
 * Loads credentials from file or environment variables
 */
@Configuration
public class GoogleCloudConfig {
    
    @Value("${google.cloud.project-id:hokori-web}")
    private String projectId;
    
    @Value("${google.cloud.enabled:false}")
    private boolean googleCloudEnabled;
    
    @Value("${google.cloud.credentials.path:classpath:google-cloud-service-account.json}")
    private String credentialsPath;
    
    // Environment variables for Railway (alternative to JSON file)
    @Value("${google.cloud.private-key:}")
    private String privateKey;
    
    @Value("${google.cloud.private-key-id:}")
    private String privateKeyId;
    
    @Value("${google.cloud.client-email:}")
    private String clientEmail;
    
    @Value("${google.cloud.client-id:}")
    private String clientId;
    
    @Value("${google.cloud.client-x509-cert-url:}")
    private String clientX509CertUrl;
    
    /**
     * Load Google Credentials from file or environment variables
     */
    private GoogleCredentials loadCredentials() throws IOException {
        if (!googleCloudEnabled) {
            throw new IOException("Google Cloud AI is disabled. Set google.cloud.enabled=true to enable.");
        }
        
        // Option 1: Load from environment variables (for Railway)
        if (privateKey != null && !privateKey.isEmpty() && 
            clientEmail != null && !clientEmail.isEmpty()) {
            try {
                String jsonCredentials = buildGoogleCloudJsonFromEnv();
                InputStream credentialsStream = new ByteArrayInputStream(
                    jsonCredentials.getBytes(StandardCharsets.UTF_8));
                GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream);
                System.out.println("✅ Google Cloud credentials loaded from environment variables");
                return credentials;
            } catch (Exception e) {
                System.out.println("⚠️ Failed to load Google Cloud from env vars, trying file: " + e.getMessage());
            }
        }
        
        // Option 2: Load from JSON file (for local dev)
        String resourcePath = credentialsPath.startsWith("classpath:") ? 
            credentialsPath.substring("classpath:".length()) : credentialsPath;
        
        Resource resource = new ClassPathResource(resourcePath);
        if (resource.exists()) {
            InputStream serviceAccount = resource.getInputStream();
            GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
            System.out.println("✅ Google Cloud credentials loaded from file: " + resourcePath);
            return credentials;
        }
        
        // Option 3: Try default credentials (if running on GCP)
        try {
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
            System.out.println("✅ Google Cloud credentials loaded from default application credentials");
            return credentials;
        } catch (Exception e) {
            System.out.println("⚠️ Failed to load default credentials: " + e.getMessage());
        }
        
        throw new IOException("Google Cloud credentials not found. Please configure credentials file or environment variables.");
    }
    
    private String buildGoogleCloudJsonFromEnv() {
        // Escape JSON string properly
        // Handle both cases: Railway may keep \n as literal or convert to actual newline
        String escapedPrivateKey = "";
        if (privateKey != null) {
            String normalizedKey = privateKey;
            
            // Check if it contains actual newlines (Railway converted \n to real newline)
            if (privateKey.contains("\n") || privateKey.contains("\r")) {
                // Already has actual newlines, use as-is
                normalizedKey = privateKey;
            } else if (privateKey.contains("\\n")) {
                // Contains literal \n, convert to actual newline
                normalizedKey = privateKey.replace("\\n", "\n").replace("\\r", "\r");
            }
            
            // Then escape for JSON: convert actual newlines to \n for JSON
            escapedPrivateKey = normalizedKey.replace("\\", "\\\\")
                     .replace("\"", "\\\"")
                     .replace("\n", "\\n")
                     .replace("\r", "\\r");
        }
        
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
    
    /**
     * Bean for Cloud Translation API
     */
    @Bean
    public Translate translateClient() {
        if (!googleCloudEnabled) {
            return null;
        }
        try {
            GoogleCredentials credentials = loadCredentials();
            TranslateOptions options = TranslateOptions.newBuilder()
                .setCredentials(credentials)
                .setProjectId(projectId)
                .build();
            Translate translate = options.getService();
            System.out.println("✅ Cloud Translation API client initialized");
            return translate;
        } catch (Exception e) {
            System.out.println("❌ Failed to initialize Cloud Translation API: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Bean for Cloud Natural Language API
     */
    @Bean
    public LanguageServiceClient languageServiceClient() {
        if (!googleCloudEnabled) {
            return null;
        }
        try {
            // LanguageServiceClient will use default credentials from environment
            // or GOOGLE_APPLICATION_CREDENTIALS environment variable
            LanguageServiceClient client = LanguageServiceClient.create();
            System.out.println("✅ Cloud Natural Language API client initialized");
            return client;
        } catch (Exception e) {
            System.out.println("❌ Failed to initialize Cloud Natural Language API: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Bean for Cloud Speech-to-Text API
     */
    @Bean
    public SpeechClient speechClient() {
        if (!googleCloudEnabled) {
            return null;
        }
        try {
            GoogleCredentials credentials = loadCredentials();
            SpeechSettings settings = SpeechSettings.newBuilder()
                .setCredentialsProvider(() -> credentials)
                .build();
            SpeechClient client = SpeechClient.create(settings);
            System.out.println("✅ Cloud Speech-to-Text API client initialized");
            return client;
        } catch (Exception e) {
            System.out.println("❌ Failed to initialize Cloud Speech-to-Text API: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Bean for Cloud Text-to-Speech API
     */
    @Bean
    public TextToSpeechClient textToSpeechClient() {
        if (!googleCloudEnabled) {
            return null;
        }
        try {
            GoogleCredentials credentials = loadCredentials();
            TextToSpeechSettings settings = TextToSpeechSettings.newBuilder()
                .setCredentialsProvider(() -> credentials)
                .build();
            TextToSpeechClient client = TextToSpeechClient.create(settings);
            System.out.println("✅ Cloud Text-to-Speech API client initialized");
            return client;
        } catch (Exception e) {
            System.out.println("❌ Failed to initialize Cloud Text-to-Speech API: " + e.getMessage());
            return null;
        }
    }
}

