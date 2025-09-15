package com.quanttrading.exception;

/**
 * Exception thrown when there are insufficient funds for a trade
 */
public class InsufficientFundsException extends TradingSystemException {
    
    public InsufficientFundsException(String message) {
        super(message);
    }
    
    public InsufficientFundsException(String message, Throwable cause) {
        super(message, cause);
    }
}