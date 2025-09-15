package com.quanttrading.exception;

/**
 * Exception thrown when strategy execution fails
 */
public class StrategyExecutionException extends TradingSystemException {
    
    private final String strategyName;
    
    public StrategyExecutionException(String strategyName, String message) {
        super(String.format("Strategy '%s' execution failed: %s", strategyName, message));
        this.strategyName = strategyName;
    }
    
    public StrategyExecutionException(String strategyName, String message, Throwable cause) {
        super(String.format("Strategy '%s' execution failed: %s", strategyName, message), cause);
        this.strategyName = strategyName;
    }
    
    public String getStrategyName() {
        return strategyName;
    }
}