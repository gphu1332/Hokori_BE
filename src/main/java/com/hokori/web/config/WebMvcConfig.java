package com.hokori.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebMvcConfig - Configuration for Spring MVC
 * 
 * NOTE: File serving is now handled by FileController which serves files from PostgreSQL database.
 * The previous filesystem-based resource handler has been removed.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    // File serving is now handled by FileController (serves from PostgreSQL)
    // No need for filesystem resource handler anymore
}

