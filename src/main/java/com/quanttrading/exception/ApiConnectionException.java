package com.quanttrading.exception;

/**
 * Exception thrown when API connection fails
 */
public class ApiConnectionException extends TradingSystemException {
    
    public ApiConnectionException(String message) {
        super(message);
    }
    
    public ApiConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}