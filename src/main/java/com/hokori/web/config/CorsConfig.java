package com.hokori.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS Configuration for Frontend Integration
 * 
 * Supports:
 * - Local development: localhost (all ports), 127.0.0.1 (all ports)
 * - Development tools: ngrok tunnels (*.ngrok-free.app, *.ngrok.io, *.ngrok.app)
 * - Production: Vercel (*.vercel.app), Railway (*.up.railway.app)
 * 
 * IMPORTANT: 
 * - FE local development (localhost) CAN call this API
 * - FE deployed on Vercel CAN call this API
 * - Uses pattern-based matching to support all origins while allowing credentials
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Use pattern-based matching to support all origins
        // This allows both localhost (development) and production domains (Vercel, Railway)
        // Pattern "*" matches:
        // - http://localhost:* (all ports)
        // - http://127.0.0.1:* (all ports)
        // - https://*.vercel.app (Vercel deployments)
        // - https://*.up.railway.app (Railway deployments)
        // - https://*.ngrok-free.app, *.ngrok.io, *.ngrok.app (ngrok tunnels)
        // 
        // Note: When setAllowCredentials(true), cannot use addAllowedOrigin("*")
        // Must use addAllowedOriginPattern("*") instead
        configuration.addAllowedOriginPattern("*");
        
        // Allow all HTTP methods
        configuration.addAllowedMethod("*");
        
        // Allow all headers
        configuration.addAllowedHeader("*");
        
        // Allow credentials (cookies, authorization headers)
        // IMPORTANT: When credentials are enabled, must use addAllowedOriginPattern instead of addAllowedOrigin
        configuration.setAllowCredentials(true);
        
        // Cache preflight for 1 hour
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
