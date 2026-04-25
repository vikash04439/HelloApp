package com.learn.rest.HelloApp.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that generates a unique transaction ID for every incoming request.
 * The ID is stored in ThreadLocal (TransactionContext) and MDC for logging.
 */
@Component
@Order(1)
public class TransactionIdFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(TransactionIdFilter.class);

    @Value("${applicationNode:}")
    private String applicationNode;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String transactionId = TransactionIdGenerator.generate(applicationNode);
        TransactionContext.set(transactionId);
        MDC.put("transactionId", transactionId);

        String method = request.getMethod();
        String uri = request.getRequestURI();
        logger.info(">>> Transaction [{}] started for [{}] {}", transactionId, method, uri);

        try {
            filterChain.doFilter(request, response);
        } finally {
            logger.info(">>> Transaction [{}] completed for [{}] {} - Status: {}",
                    transactionId, method, uri, response.getStatus());
            TransactionContext.clear();
            MDC.remove("transactionId");
        }
    }
}

