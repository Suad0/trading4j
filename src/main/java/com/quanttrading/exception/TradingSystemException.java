package com.quanttrading.exception;

/**
 * Base exception for trading system errors
 */
public class TradingSystemException extends RuntimeException {
    
    public TradingSystemException(String message) {
        super(message);
    }
    
    public TradingSystemException(String message, Throwable cause) {
        super(message, cause);
    }
}