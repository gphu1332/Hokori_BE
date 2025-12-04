package com.hokori.web.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for calling Google Gemini API
 * Uses REST API for simplicity (no need for Vertex AI SDK)
 * Supports both API Key and OAuth2 token authentication
 */
@Slf4j
@Service
public class GeminiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${google.cloud.project-id:hokori-web}")
    private String projectId;

    @Value("${google.cloud.enabled:false}")
    private boolean googleCloudEnabled;

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.0-flash-exp}")
    private String modelName;

    // Service account credentials for OAuth2 (alternative to API key)
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

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

    @Autowired
    public GeminiService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Call Gemini API with a prompt
     * Returns the text response from Gemini
     */
    public String generateContent(String prompt) {
        if (!googleCloudEnabled) {
            throw new RuntimeException("Google Cloud AI is not enabled");
        }

        try {
            String url = String.format(GEMINI_API_URL, modelName);
            
            // Add API key to URL if available, otherwise use OAuth2 token
            if (apiKey != null && !apiKey.isEmpty()) {
                url += "?key=" + apiKey;
            }
            
            Map<String, Object> requestBody = new HashMap<>();
            List<Map<String, Object>> contents = new ArrayList<>();
            Map<String, Object> content = new HashMap<>();
            List<Map<String, Object>> parts = new ArrayList<>();
            Map<String, Object> part = new HashMap<>();
            part.put("text", prompt);
            parts.add(part);
            content.put("parts", parts);
            contents.add(content);
            requestBody.put("contents", contents);

            // Generation config
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("topK", 40);
            generationConfig.put("topP", 0.95);
            generationConfig.put("maxOutputTokens", 8192);
            requestBody.put("generationConfig", generationConfig);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // If no API key, use OAuth2 token
            if (apiKey == null || apiKey.isEmpty()) {
                String accessToken = getAccessToken();
                headers.setBearerAuth(accessToken);
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            log.debug("Calling Gemini API: {}", url);
            ResponseEntity<GeminiResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    GeminiResponse.class
            );

            if (response.getBody() != null && response.getBody().getCandidates() != null 
                    && !response.getBody().getCandidates().isEmpty()) {
                GeminiResponse.Candidate candidate = response.getBody().getCandidates().get(0);
                if (candidate.getContent() != null && candidate.getContent().getParts() != null
                        && !candidate.getContent().getParts().isEmpty()) {
                    String text = candidate.getContent().getParts().get(0).getText();
                    log.debug("Gemini response received, length: {}", text.length());
                    return text;
                }
            }

            log.warn("Gemini API returned empty response");
            return null;

        } catch (Exception e) {
            log.error("Error calling Gemini API", e);
            throw new RuntimeException("Failed to call Gemini API: " + e.getMessage(), e);
        }
    }

    /**
     * Call Gemini API and parse JSON response
     */
    public JsonNode generateContentAsJson(String prompt) {
        String response = generateContent(prompt);
        if (response == null || response.trim().isEmpty()) {
            return null;
        }

        try {
            // Try to extract JSON from markdown code blocks if present
            String jsonText = response;
            if (response.contains("```json")) {
                int start = response.indexOf("```json") + 7;
                int end = response.indexOf("```", start);
                if (end > start) {
                    jsonText = response.substring(start, end).trim();
                }
            } else if (response.contains("```")) {
                int start = response.indexOf("```") + 3;
                int end = response.indexOf("```", start);
                if (end > start) {
                    jsonText = response.substring(start, end).trim();
                }
            }

            return objectMapper.readTree(jsonText);
        } catch (Exception e) {
            log.error("Failed to parse Gemini JSON response", e);
            log.debug("Response text: {}", response);
            return null;
        }
    }

    /**
     * Get access token from Google Cloud service account credentials
     */
    private String getAccessToken() throws IOException {
        if (privateKey == null || privateKey.isEmpty() || clientEmail == null || clientEmail.isEmpty()) {
            throw new IOException("Service account credentials not configured. Please set google.cloud.private-key, google.cloud.private-key-id, google.cloud.client-email, google.cloud.client-id, and google.cloud.client-x509-cert-url");
        }

        try {
            // Build JSON credentials from environment variables
            String jsonCredentials = buildGoogleCloudJsonFromEnv();
            InputStream credentialsStream = new ByteArrayInputStream(
                    jsonCredentials.getBytes(StandardCharsets.UTF_8));
            GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream);
            credentials.refreshIfExpired();
            return credentials.getAccessToken().getTokenValue();
        } catch (Exception e) {
            log.error("Failed to get access token from service account", e);
            throw new IOException("Failed to get access token: " + e.getMessage(), e);
        }
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

    // Response DTOs
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class GeminiResponse {
        @JsonProperty("candidates")
        private List<Candidate> candidates;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class Candidate {
            @JsonProperty("content")
            private Content content;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class Content {
            @JsonProperty("parts")
            private List<Part> parts;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class Part {
            @JsonProperty("text")
            private String text;
        }
    }
}

