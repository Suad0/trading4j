package com.quanttrading.config;

import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * Filter to add correlation ID to all requests for tracing
 */
@Component
@Order(1)
public class CorrelationIdFilter implements Filter {
    
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        try {
            // Get or generate correlation ID
            String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.trim().isEmpty()) {
                correlationId = UUID.randomUUID().toString();
            }
            
            // Add to MDC for logging
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
            
            // Add to response header
            httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);
            
            chain.doFilter(request, response);
        } finally {
            // Clean up MDC
            MDC.clear();
        }
    }
}