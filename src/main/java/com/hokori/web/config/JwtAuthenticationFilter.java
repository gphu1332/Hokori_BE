package com.hokori.web.config;

import com.hokori.web.entity.User;
import com.hokori.web.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtConfig jwtConfig;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        final String requestTokenHeader = request.getHeader("Authorization");

        String email = null;
        String jwtToken = null;

        // JWT Token is in the form "Bearer token". Remove Bearer word and get only the Token
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            try {
                email = jwtConfig.extractUsername(jwtToken);
            } catch (IllegalArgumentException e) {
                logger.warn("Unable to get JWT Token");
            } catch (ExpiredJwtException e) {
                logger.warn("JWT Token has expired");
            }
        }

        // Once we get the token validate it.
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Validate token
            if (jwtConfig.validateToken(jwtToken, email)) {
                
                try {
                    // Get user from repository
                    var userOpt = userRepository.findByEmail(email);
                    if (userOpt.isPresent()) {
                        var user = userOpt.get();
                        
                        // Check if user is active
                        if (user.getIsActive()) {
                            // Extract roles from JWT claims (fallback to database)
                            List<String> roles = List.of();
                            try {
                                var claims = jwtConfig.extractAllClaims(jwtToken);
                                @SuppressWarnings("unchecked")
                                var rolesFromToken = (List<String>) claims.get("roles");
                                if (rolesFromToken != null && !rolesFromToken.isEmpty()) {
                                    roles = rolesFromToken;
                                }
                            } catch (Exception ex) {
                                // Fallback to database roles
                                if (user.getRole() != null) {
                                    roles = List.of(user.getRole().getRoleName());
                                }
                            }
                            
                            // Convert roles to authorities
                            var authorities = roles.stream()
                                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                                    .collect(Collectors.toList());

                            UsernamePasswordAuthenticationToken authToken = 
                                    new UsernamePasswordAuthenticationToken(email, null, authorities);
                            
                            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            
                            SecurityContextHolder.getContext().setAuthentication(authToken);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Cannot set user authentication: {}", e);
                }
            }
        }
        filterChain.doFilter(request, response);
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        
        // Skip JWT filter for public endpoints
        return path.startsWith("/api/auth/") ||
               path.startsWith("/actuator/") ||
               path.startsWith("/v3/api-docs/") ||
               path.startsWith("/swagger-ui/") ||
               path.equals("/swagger-ui.html") ||
               path.startsWith("/api-docs/") ||
               path.equals("/health") ||
               path.equals("/api/health");
    }
}
