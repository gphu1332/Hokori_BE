package com.hokori.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "payos")
@Data
public class PayOSConfig {
    
    private boolean enabled = true;
    private String clientId;
    private String apiKey;
    private String checksumKey;
    private String apiUrl = "https://api-merchant.payos.vn";
    
    // Trim whitespace from environment variables
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl != null ? apiUrl.trim() : null;
    }
    private String webhookUrl;
    private String returnUrl;
    private String cancelUrl;
    
    @Bean
    public RestTemplate payOSRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(clientHttpRequestFactory());
        return restTemplate;
    }
    
    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(30).toMillis());
        return factory;
    }
    
    @Bean
    public ObjectMapper payOSObjectMapper() {
        return new ObjectMapper();
    }
}
