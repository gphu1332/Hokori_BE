package com.hokori.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Hokori API")
                        .version("1.0.0")
                        .description("Japanese Learning Platform API - Content-Based Learning + JLPT Mock Tests")
                        .contact(new Contact()
                                .name("Hokori Team")
                                .email("contact@hokori.com")
                                .url("https://github.com/gphu1332/Hokori_BE"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
