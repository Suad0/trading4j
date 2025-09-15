package com.quanttrading.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.ServletException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
        MDC.clear();
    }

    @Test
    void shouldGenerateCorrelationIdWhenNotProvided() throws ServletException, IOException {
        // When
        filter.doFilter(request, response, filterChain);

        // Then
        String correlationId = response.getHeader("X-Correlation-ID");
        assertNotNull(correlationId);
        assertFalse(correlationId.trim().isEmpty());
        
        // Verify UUID format (36 characters with hyphens)
        assertEquals(36, correlationId.length());
        assertTrue(correlationId.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    }

    @Test
    void shouldUseProvidedCorrelationId() throws ServletException, IOException {
        // Given
        String providedCorrelationId = "test-correlation-id-123";
        request.addHeader("X-Correlation-ID", providedCorrelationId);

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        String responseCorrelationId = response.getHeader("X-Correlation-ID");
        assertEquals(providedCorrelationId, responseCorrelationId);
    }

    @Test
    void shouldGenerateNewIdWhenProvidedIdIsEmpty() throws ServletException, IOException {
        // Given
        request.addHeader("X-Correlation-ID", "");

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        String correlationId = response.getHeader("X-Correlation-ID");
        assertNotNull(correlationId);
        assertFalse(correlationId.trim().isEmpty());
    }

    @Test
    void shouldGenerateNewIdWhenProvidedIdIsWhitespace() throws ServletException, IOException {
        // Given
        request.addHeader("X-Correlation-ID", "   ");

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        String correlationId = response.getHeader("X-Correlation-ID");
        assertNotNull(correlationId);
        assertFalse(correlationId.trim().isEmpty());
    }

    @Test
    void shouldClearMDCAfterRequest() throws ServletException, IOException {
        // Given
        String providedCorrelationId = "test-correlation-id-123";
        request.addHeader("X-Correlation-ID", providedCorrelationId);

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        assertNull(MDC.get("correlationId"));
    }

    @Test
    void shouldClearMDCEvenWhenExceptionOccurs() throws ServletException, IOException {
        // Given
        String providedCorrelationId = "test-correlation-id-123";
        request.addHeader("X-Correlation-ID", providedCorrelationId);
        
        MockFilterChain throwingFilterChain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) 
                    throws IOException, ServletException {
                throw new ServletException("Test exception");
            }
        };

        // When & Then
        assertThrows(ServletException.class, () -> {
            filter.doFilter(request, response, throwingFilterChain);
        });
        
        // Verify MDC is still cleared
        assertNull(MDC.get("correlationId"));
    }
}