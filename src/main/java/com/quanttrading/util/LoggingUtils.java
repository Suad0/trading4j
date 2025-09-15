package com.quanttrading.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;

/**
 * Utility class for structured logging
 */
public class LoggingUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingUtils.class);
    
    public static final String CORRELATION_ID_KEY = "correlationId";
    public static final String OPERATION_KEY = "operation";
    public static final String SYMBOL_KEY = "symbol";
    public static final String ORDER_ID_KEY = "orderId";
    public static final String STRATEGY_KEY = "strategy";
    public static final String DURATION_KEY = "duration";
    public static final String ERROR_CODE_KEY = "errorCode";
    
    /**
     * Get current correlation ID from MDC
     */
    public static String getCorrelationId() {
        return MDC.get(CORRELATION_ID_KEY);
    }
    
    /**
     * Add structured context to MDC
     */
    public static void addContext(String key, String value) {
        if (value != null) {
            MDC.put(key, value);
        }
    }
    
    /**
     * Add multiple context values to MDC
     */
    public static void addContext(Map<String, String> context) {
        if (context != null) {
            context.forEach((key, value) -> {
                if (value != null) {
                    MDC.put(key, value);
                }
            });
        }
    }
    
    /**
     * Remove context from MDC
     */
    public static void removeContext(String key) {
        MDC.remove(key);
    }
    
    /**
     * Clear all context from MDC
     */
    public static void clearContext() {
        MDC.clear();
    }
    
    /**
     * Log trading operation start
     */
    public static void logOperationStart(String operation, String symbol) {
        addContext(OPERATION_KEY, operation);
        addContext(SYMBOL_KEY, symbol);
        logger.info("Starting operation: {} for symbol: {}", operation, symbol);
    }
    
    /**
     * Log trading operation completion
     */
    public static void logOperationComplete(String operation, String symbol, long durationMs) {
        addContext(DURATION_KEY, String.valueOf(durationMs));
        logger.info("Completed operation: {} for symbol: {} in {}ms", operation, symbol, durationMs);
        removeContext(OPERATION_KEY);
        removeContext(SYMBOL_KEY);
        removeContext(DURATION_KEY);
    }
    
    /**
     * Log trading operation error
     */
    public static void logOperationError(String operation, String symbol, String errorCode, String message, Throwable throwable) {
        addContext(OPERATION_KEY, operation);
        addContext(SYMBOL_KEY, symbol);
        addContext(ERROR_CODE_KEY, errorCode);
        logger.error("Operation failed: {} for symbol: {} - Error: {} - Message: {}", 
                    operation, symbol, errorCode, message, throwable);
        removeContext(OPERATION_KEY);
        removeContext(SYMBOL_KEY);
        removeContext(ERROR_CODE_KEY);
    }
    
    /**
     * Log strategy execution
     */
    public static void logStrategyExecution(String strategyName, String symbol, String signal) {
        addContext(STRATEGY_KEY, strategyName);
        addContext(SYMBOL_KEY, symbol);
        logger.info("Strategy execution: {} generated signal: {} for symbol: {}", 
                   strategyName, signal, symbol);
        removeContext(STRATEGY_KEY);
        removeContext(SYMBOL_KEY);
    }
    
    /**
     * Log order execution
     */
    public static void logOrderExecution(String orderId, String symbol, String orderType, String quantity, String price) {
        addContext(ORDER_ID_KEY, orderId);
        addContext(SYMBOL_KEY, symbol);
        logger.info("Order executed: {} - Type: {} - Symbol: {} - Quantity: {} - Price: {}", 
                   orderId, orderType, symbol, quantity, price);
        removeContext(ORDER_ID_KEY);
        removeContext(SYMBOL_KEY);
    }
}