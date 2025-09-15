package com.quanttrading.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LoggingUtilsTest {

    @BeforeEach
    void setUp() {
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void shouldAddSingleContextToMDC() {
        // When
        LoggingUtils.addContext("testKey", "testValue");

        // Then
        assertEquals("testValue", MDC.get("testKey"));
    }

    @Test
    void shouldNotAddNullValueToMDC() {
        // When
        LoggingUtils.addContext("testKey", null);

        // Then
        assertNull(MDC.get("testKey"));
    }

    @Test
    void shouldAddMultipleContextToMDC() {
        // Given
        Map<String, String> context = new HashMap<>();
        context.put("key1", "value1");
        context.put("key2", "value2");
        context.put("key3", null); // Should be ignored

        // When
        LoggingUtils.addContext(context);

        // Then
        assertEquals("value1", MDC.get("key1"));
        assertEquals("value2", MDC.get("key2"));
        assertNull(MDC.get("key3"));
    }

    @Test
    void shouldHandleNullContextMap() {
        // When & Then (should not throw exception)
        assertDoesNotThrow(() -> LoggingUtils.addContext((Map<String, String>) null));
    }

    @Test
    void shouldRemoveContextFromMDC() {
        // Given
        MDC.put("testKey", "testValue");

        // When
        LoggingUtils.removeContext("testKey");

        // Then
        assertNull(MDC.get("testKey"));
    }

    @Test
    void shouldClearAllContextFromMDC() {
        // Given
        MDC.put("key1", "value1");
        MDC.put("key2", "value2");

        // When
        LoggingUtils.clearContext();

        // Then
        assertNull(MDC.get("key1"));
        assertNull(MDC.get("key2"));
    }

    @Test
    void shouldGetCorrelationIdFromMDC() {
        // Given
        String correlationId = "test-correlation-id";
        MDC.put(LoggingUtils.CORRELATION_ID_KEY, correlationId);

        // When
        String result = LoggingUtils.getCorrelationId();

        // Then
        assertEquals(correlationId, result);
    }

    @Test
    void shouldReturnNullWhenCorrelationIdNotSet() {
        // When
        String result = LoggingUtils.getCorrelationId();

        // Then
        assertNull(result);
    }

    @Test
    void shouldLogOperationStartAndAddContext() {
        // When
        LoggingUtils.logOperationStart("BUY_ORDER", "AAPL");

        // Then
        assertEquals("BUY_ORDER", MDC.get(LoggingUtils.OPERATION_KEY));
        assertEquals("AAPL", MDC.get(LoggingUtils.SYMBOL_KEY));
    }

    @Test
    void shouldLogOperationCompleteAndRemoveContext() {
        // Given
        LoggingUtils.addContext(LoggingUtils.OPERATION_KEY, "BUY_ORDER");
        LoggingUtils.addContext(LoggingUtils.SYMBOL_KEY, "AAPL");

        // When
        LoggingUtils.logOperationComplete("BUY_ORDER", "AAPL", 1500L);

        // Then
        assertNull(MDC.get(LoggingUtils.OPERATION_KEY));
        assertNull(MDC.get(LoggingUtils.SYMBOL_KEY));
        assertNull(MDC.get(LoggingUtils.DURATION_KEY));
    }

    @Test
    void shouldLogOperationErrorAndRemoveContext() {
        // Given
        Exception testException = new RuntimeException("Test error");

        // When
        LoggingUtils.logOperationError("BUY_ORDER", "AAPL", "ORDER_FAILED", "Order validation failed", testException);

        // Then
        assertNull(MDC.get(LoggingUtils.OPERATION_KEY));
        assertNull(MDC.get(LoggingUtils.SYMBOL_KEY));
        assertNull(MDC.get(LoggingUtils.ERROR_CODE_KEY));
    }

    @Test
    void shouldLogStrategyExecutionAndRemoveContext() {
        // When
        LoggingUtils.logStrategyExecution("SMA_STRATEGY", "AAPL", "BUY");

        // Then
        assertNull(MDC.get(LoggingUtils.STRATEGY_KEY));
        assertNull(MDC.get(LoggingUtils.SYMBOL_KEY));
    }

    @Test
    void shouldLogOrderExecutionAndRemoveContext() {
        // When
        LoggingUtils.logOrderExecution("order-123", "AAPL", "BUY", "100", "150.50");

        // Then
        assertNull(MDC.get(LoggingUtils.ORDER_ID_KEY));
        assertNull(MDC.get(LoggingUtils.SYMBOL_KEY));
    }
}