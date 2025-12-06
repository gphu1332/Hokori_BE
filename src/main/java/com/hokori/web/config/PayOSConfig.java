package com.hokori.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "payos")
@Data
@Slf4j
public class PayOSConfig {
    
    private boolean enabled = true;
    private String clientId;
    private String apiKey;
    private String checksumKey;
    private String apiUrl = "https://api.payos.vn/v2";
    private String webhookUrl;
    private String returnUrl;
    private String cancelUrl;
    
    @Bean
    public RestTemplate payOSRestTemplate() {
        try {
            // Use Apache HttpClient with better DNS handling and connection pooling
            CloseableHttpClient httpClient = createHttpClient();
            HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
            
            RestTemplate restTemplate = new RestTemplate(factory);
            log.info("PayOS RestTemplate configured with Apache HttpClient for better DNS handling");
            return restTemplate;
        } catch (Exception e) {
            log.warn("Failed to create Apache HttpClient, falling back to SimpleClientHttpRequestFactory", e);
            // Fallback to simple factory
            return createSimpleRestTemplate();
        }
    }
    
    private CloseableHttpClient createHttpClient() {
        try {
            // Create connection manager with pooling for better performance
            PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
            connectionManager.setMaxTotal(100); // Max total connections
            connectionManager.setDefaultMaxPerRoute(20); // Max connections per route
            
            // Configure request timeouts (increased for DNS resolution)
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout((int) Duration.ofSeconds(20).toMillis()) // Increased to 20s for DNS
                    .setSocketTimeout((int) Duration.ofSeconds(30).toMillis()) // Socket timeout
                    .setConnectionRequestTimeout((int) Duration.ofSeconds(10).toMillis()) // Connection request timeout
                    .build();
            
            // Build HttpClient with custom configuration
            CloseableHttpClient httpClient = HttpClientBuilder.create()
                    .setConnectionManager(connectionManager)
                    .setDefaultRequestConfig(requestConfig)
                    .setUserAgent("Hokori-Backend/1.0")
                    .disableCookieManagement() // Disable cookies for API calls
                    .build();
            
            log.info("Apache HttpClient created with connection pooling and extended timeouts for DNS resolution");
            return httpClient;
        } catch (Exception e) {
            log.error("Error creating Apache HttpClient", e);
            throw new RuntimeException("Failed to create HttpClient", e);
        }
    }
    
    private RestTemplate createSimpleRestTemplate() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = 
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(20).toMillis()); // Increased timeout for DNS
        factory.setReadTimeout((int) Duration.ofSeconds(30).toMillis());
        
        RestTemplate restTemplate = new RestTemplate(factory);
        log.info("PayOS RestTemplate configured with SimpleClientHttpRequestFactory (fallback)");
        return restTemplate;
    }
    
    @Bean
    public ObjectMapper payOSObjectMapper() {
        return new ObjectMapper();
    }
}
