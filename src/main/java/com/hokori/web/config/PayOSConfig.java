package com.hokori.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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
        // Use SimpleClientHttpRequestFactory with increased timeouts for DNS resolution
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(20).toMillis()); // Increased to 20s for DNS
        factory.setReadTimeout((int) Duration.ofSeconds(30).toMillis()); // Read timeout
        
        RestTemplate restTemplate = new RestTemplate(factory);
        log.info("PayOS RestTemplate configured with SimpleClientHttpRequestFactory (20s connect timeout for DNS)");
        return restTemplate;
    }
    
    @Bean
    public ObjectMapper payOSObjectMapper() {
        return new ObjectMapper();
    }
}
