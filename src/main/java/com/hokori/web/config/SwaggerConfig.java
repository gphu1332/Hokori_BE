package com.hokori.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Bean
    public OpenAPI customOpenAPI() {
        List<Server> servers = new ArrayList<>();
        
        // Production: Use Railway URL or environment variable
        if ("prod".equals(activeProfile)) {
            String railwayUrl = System.getenv("RAILWAY_PUBLIC_DOMAIN");
            if (railwayUrl != null && !railwayUrl.isEmpty()) {
                servers.add(new Server()
                    .url("https://" + railwayUrl)
                    .description("Railway Production"));
            } else {
                // Fallback: Use custom domain
                servers.add(new Server()
                    .url("https://api.hokori-backend.org")
                    .description("Railway Production (custom domain)"));
            }
        } else {
            // Development: Use ngrok or localhost
            String ngrokUrl = System.getenv("NGROK_URL");
            if (ngrokUrl != null && !ngrokUrl.isEmpty()) {
                servers.add(new Server()
                    .url(ngrokUrl)
                    .description("Ngrok Tunnel"));
            } else {
                servers.add(new Server()
                    .url("http://localhost:8080")
                    .description("Local Development"));
            }
        }
        
        return new OpenAPI()
                .info(new Info()
                        .title("Hokori API")
                        .version("1.0.0")
                        .description("Japanese Learning Platform API"))
                .servers(servers)
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("Bearer Authentication", 
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token")));
    }
}
