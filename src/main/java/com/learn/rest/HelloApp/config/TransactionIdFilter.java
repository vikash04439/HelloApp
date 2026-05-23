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
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Filter that generates a unique transaction ID for every incoming request.
 * Logs request/response with body to GatewayReqRes log file.
 */
@Component
@Order(1)
public class TransactionIdFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(TransactionIdFilter.class);
    private static final Logger gatewayLogger = LoggerFactory.getLogger("GatewayReqRes");

    @Value("${applicationNode:}")
    private String applicationNode;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String transactionId = TransactionIdGenerator.generate(applicationNode);
        TransactionContext.set(transactionId);
        MDC.put("transactionId", transactionId);

        // Wrap request and response to cache bodies
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, 2048);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String fullUri = queryString != null ? uri + "?" + queryString : uri;
        String clientIp = request.getRemoteAddr();
        String contentType = request.getContentType();

        logger.info(">>> Transaction [{}] started for [{}] {}", transactionId, method, fullUri);

        long startTime = System.currentTimeMillis();
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = wrappedResponse.getStatus();
            String responseContentType = wrappedResponse.getContentType();

            // Extract request body
            String requestBody = getContent(wrappedRequest.getContentAsByteArray());
            // Extract response body
            String responseBody = getContent(wrappedResponse.getContentAsByteArray());

            // Log request with body
            gatewayLogger.info("REQUEST  | TxnId: {} | Method: {} | URI: {} | ClientIP: {} | ContentType: {} | Body: {}",
                    transactionId, method, fullUri, clientIp, contentType,
                    requestBody.isEmpty() ? "[empty]" : requestBody);

            // Log response with body
            gatewayLogger.info("RESPONSE | TxnId: {} | Method: {} | URI: {} | Status: {} | ContentType: {} | Duration: {}ms | Body: {}",
                    transactionId, method, fullUri, status, responseContentType, duration,
                    responseBody.isEmpty() ? "[empty]" : responseBody);

            logger.info(">>> Transaction [{}] completed for [{}] {} - Status: {}",
                    transactionId, method, fullUri, status);

            // IMPORTANT: copy body back to response so client receives it
            wrappedResponse.copyBodyToResponse();

            TransactionContext.clear();
            MDC.remove("transactionId");
        }
    }

    private String getContent(byte[] content) {
        if (content == null || content.length == 0) {
            return "";
        }
        String body = new String(content, StandardCharsets.UTF_8);
        // Truncate very large bodies to avoid flooding the log
        if (body.length() > 2000) {
            return body.substring(0, 2000) + "...[TRUNCATED]";
        }
        return body;
    }
}

