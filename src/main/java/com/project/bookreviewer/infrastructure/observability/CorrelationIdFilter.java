package com.project.bookreviewer.infrastructure.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Propagates {@code X-Correlation-ID} for request tracing across logs and responses.
 * Incoming header is reused when present; otherwise a new UUID is generated.
 */
@Component
// Highest precedence to ensure the correlation id is set before any other filters
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Correlation-ID";
    public static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String correlationId = resolveCorrelationId(request.getHeader(HEADER_NAME));
        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER_NAME, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            // Remove the correlation id from the MDC after the request is processed
            MDC.remove(MDC_KEY);
        }
    }

    // Generates a new correlation id if the incoming one is null or blank
    static String resolveCorrelationId(String incoming) {
        if (incoming == null || incoming.isBlank()) {
            return UUID.randomUUID().toString();
        }
        String trimmed = incoming.trim();
        // Cap length to avoid log abuse / huge headers
        return trimmed.length() > 128 ? trimmed.substring(0, 128) : trimmed;
    }
}
