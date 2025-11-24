package com.hokori.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfig {
    
    /**
     * Configure ObjectMapper with JavaTimeModule for Java 8 date/time support.
     * This is required to serialize LocalDateTime, Instant, etc. to JSON.
     * 
     * IMPORTANT: Spring Boot 3.x should auto-enable JavaTimeModule, but we ensure it's registered
     * to avoid serialization errors in production.
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper mapper = builder.build();
        
        // Ensure JavaTimeModule is registered (handles LocalDateTime, Instant, etc.)
        mapper.registerModule(new JavaTimeModule());
        
        // Disable writing dates as timestamps (use ISO-8601 format instead)
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        return mapper;
    }
}

