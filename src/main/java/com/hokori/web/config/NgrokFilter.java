package com.hokori.web.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filter to handle ngrok browser warning headers
 * Only active in development mode (not production)
 * This helps team members access the API through ngrok without CORS issues
 */
@Component
@Order(1)
public class NgrokFilter implements Filter {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Only add ngrok headers in development mode (not production)
        // This ensures Railway deployment doesn't have unnecessary headers
        if (!"prod".equals(activeProfile)) {
            // Add ngrok headers to bypass browser warning
            httpResponse.setHeader("ngrok-skip-browser-warning", "true");
        }
        
        // Handle preflight requests
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        
        chain.doFilter(request, response);
    }
}
