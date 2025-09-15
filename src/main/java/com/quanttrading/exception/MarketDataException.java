package com.quanttrading.exception;

/**
 * Exception thrown when market data operations fail
 */
public class MarketDataException extends TradingSystemException {
    
    private final String symbol;
    
    public MarketDataException(String symbol, String message) {
        super(String.format("Market data error for symbol '%s': %s", symbol, message));
        this.symbol = symbol;
    }
    
    public MarketDataException(String symbol, String message, Throwable cause) {
        super(String.format("Market data error for symbol '%s': %s", symbol, message), cause);
        this.symbol = symbol;
    }
    
    public String getSymbol() {
        return symbol;
    }
}