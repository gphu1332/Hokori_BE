package com.hokori.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Hokori API")
                        .version("1.0.0")
                        .description("Japanese Learning Platform API - Content-Based Learning + JLPT Mock Tests\n\n" +
                                "**Authentication:**\n" +
                                "- Firebase Login: Use `/api/auth/login/firebase` with Firebase ID token\n" +
                                "- Username/Password: Use `/api/auth/login` with credentials\n" +
                                "- Include JWT token in Authorization header: `Bearer <token>`\n\n" +
                                "**Testing with Ngrok:**\n" +
                                "1. Start app: `./mvnw spring-boot:run`\n" +
                                "2. Start ngrok: `ngrok http " + serverPort + "`\n" +
                                "3. Use ngrok URL for API calls")
                        .contact(new Contact()
                                .name("Hokori Team")
                                .email("contact@hokori.com")
                                .url("https://github.com/gphu1332/Hokori_BE"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Local Development"),
                        new Server().url("https://your-ngrok-url.ngrok.io").description("Ngrok Tunnel (Update URL)")
                ));
    }
}
