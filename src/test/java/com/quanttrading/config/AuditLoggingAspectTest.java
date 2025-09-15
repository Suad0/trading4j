package com.quanttrading.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLoggingAspectTest {

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private Signature signature;

    private AuditLoggingAspect auditLoggingAspect;
    private ListAppender<ILoggingEvent> listAppender;
    private Logger auditLogger;

    @BeforeEach
    void setUp() {
        auditLoggingAspect = new AuditLoggingAspect();
        auditLoggingAspect.objectMapper = new ObjectMapper();

        // Set up log capture
        auditLogger = (Logger) LoggerFactory.getLogger("AUDIT");
        listAppender = new ListAppender<>();
        listAppender.start();
        auditLogger.addAppender(listAppender);

        // Clear MDC
        MDC.clear();
    }

    @Test
    void logTradingOperation_ShouldLogOperationStart() {
        // Given
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("executeBuyOrder");
        when(joinPoint.getArgs()).thenReturn(new Object[]{"AAPL", "100"});

        // When
        auditLoggingAspect.logTradingOperation(joinPoint);

        // Then
        assertEquals(1, listAppender.list.size());
        ILoggingEvent logEvent = listAppender.list.get(0);
        assertTrue(logEvent.getMessage().contains("TRADING_OPERATION_START"));
        assertTrue(logEvent.getMessage().contains("executeBuyOrder"));
        assertNotNull(MDC.get("correlationId"));
        assertEquals("executeBuyOrder", MDC.get("operation"));
    }

    @Test
    void logTradingOperationSuccess_ShouldLogSuccess() {
        // Given
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("executeBuyOrder");
        MDC.put("correlationId", "test-correlation-id");
        Object result = "Order placed successfully";

        // When
        auditLoggingAspect.logTradingOperationSuccess(joinPoint, result);

        // Then
        assertEquals(1, listAppender.list.size());
        ILoggingEvent logEvent = listAppender.list.get(0);
        assertTrue(logEvent.getMessage().contains("TRADING_OPERATION_SUCCESS"));
        assertTrue(logEvent.getMessage().contains("executeBuyOrder"));
        assertTrue(logEvent.getMessage().contains("test-correlation-id"));
    }

    @Test
    void logTradingOperationFailure_ShouldLogError() {
        // Given
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("executeBuyOrder");
        MDC.put("correlationId", "test-correlation-id");
        Exception exception = new RuntimeException("Trading failed");

        // When
        auditLoggingAspect.logTradingOperationFailure(joinPoint, exception);

        // Then
        assertEquals(1, listAppender.list.size());
        ILoggingEvent logEvent = listAppender.list.get(0);
        assertTrue(logEvent.getMessage().contains("TRADING_OPERATION_FAILURE"));
        assertTrue(logEvent.getMessage().contains("executeBuyOrder"));
        assertTrue(logEvent.getMessage().contains("Trading failed"));
        assertTrue(logEvent.getMessage().contains("test-correlation-id"));
    }

    @Test
    void logPortfolioUpdate_ShouldLogUpdate() {
        // Given
        when(joinPoint.getArgs()).thenReturn(new Object[]{"AAPL", "100", "150.00"});

        // When
        auditLoggingAspect.logPortfolioUpdate(joinPoint);

        // Then
        assertEquals(1, listAppender.list.size());
        ILoggingEvent logEvent = listAppender.list.get(0);
        assertTrue(logEvent.getMessage().contains("PORTFOLIO_UPDATE"));
        assertNotNull(MDC.get("correlationId"));
    }

    @Test
    void sanitizeArgs_WithNullArgs_ShouldReturnEmptyArray() {
        // When
        String result = auditLoggingAspect.sanitizeArgs(null);

        // Then
        assertEquals("[]", result);
    }

    @Test
    void sanitizeArgs_WithEmptyArgs_ShouldReturnEmptyArray() {
        // When
        String result = auditLoggingAspect.sanitizeArgs(new Object[0]);

        // Then
        assertEquals("[]", result);
    }

    @Test
    void sanitizeResult_WithNullResult_ShouldReturnNull() {
        // When
        String result = auditLoggingAspect.sanitizeResult(null);

        // Then
        assertEquals("null", result);
    }
}