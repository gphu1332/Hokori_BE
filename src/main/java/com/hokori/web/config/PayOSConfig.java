package com.hokori.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@ConfigurationProperties(prefix = "payos")
@Data
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
        return new RestTemplate();
    }
    
    @Bean
    public ObjectMapper payOSObjectMapper() {
        return new ObjectMapper();
    }
}
