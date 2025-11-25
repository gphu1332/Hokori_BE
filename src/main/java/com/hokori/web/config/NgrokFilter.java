package com.hokori.web.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filter to handle ngrok browser warning headers
 * Only active in development mode (not production)
 * This helps team members access the API through ngrok without CORS issues
 */
@Component
@Profile("dev")
@Order(1)
public class NgrokFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Add ngrok headers to bypass browser warning
        // This filter only runs in dev profile, so safe to add headers
        httpResponse.setHeader("ngrok-skip-browser-warning", "true");
        
        // Continue filter chain - let CORS and Security handle OPTIONS requests
        chain.doFilter(request, response);
    }
}
