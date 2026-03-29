package com.learn.rest.HelloApp.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Custom filter to log incoming authentication headers.
 * Runs before Spring Security's BasicAuthenticationFilter.
 * Logs whether Basic Auth credentials are present and the username (not password).
 */
@Component
public class AuthLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AuthLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String method = request.getMethod();
        String uri = request.getRequestURI();

        if (authHeader != null && authHeader.startsWith("Basic ")) {
            logger.info(">>> [{}] {} - Basic Auth header IS present", method, uri);
            try {
                String base64Credentials = authHeader.substring("Basic ".length());
                String decoded = new String(java.util.Base64.getDecoder().decode(base64Credentials));
                String username = decoded.split(":")[0];
                logger.info(">>> Authenticated username: {}", username);
            } catch (Exception e) {
                logger.warn(">>> Failed to decode Basic Auth header");
            }
        } else if (authHeader != null) {
            logger.info(">>> [{}] {} - Auth header present but NOT Basic: {}",
                    method, uri, authHeader.substring(0, Math.min(authHeader.length(), 10)));
        } else {
            logger.warn(">>> [{}] {} - NO Authorization header found!", method, uri);
        }

        filterChain.doFilter(request, response);

        logger.info(">>> [{}] {} - Response status: {}", method, uri, response.getStatus());
    }
}

