package com.quanttrading.exception;

/**
 * Exception thrown when portfolio operations fail
 */
public class PortfolioException extends TradingSystemException {
    
    public PortfolioException(String message) {
        super(message);
    }
    
    public PortfolioException(String message, Throwable cause) {
        super(message, cause);
    }
}