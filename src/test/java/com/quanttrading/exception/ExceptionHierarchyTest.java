package com.quanttrading.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionHierarchyTest {

    @Test
    void shouldCreateTradingSystemException() {
        // Given
        String message = "Test trading system error";
        Throwable cause = new RuntimeException("Root cause");

        // When
        TradingSystemException exception1 = new TradingSystemException(message);
        TradingSystemException exception2 = new TradingSystemException(message, cause);

        // Then
        assertEquals(message, exception1.getMessage());
        assertEquals(message, exception2.getMessage());
        assertEquals(cause, exception2.getCause());
    }

    @Test
    void shouldCreateApiConnectionException() {
        // Given
        String message = "API connection failed";
        Throwable cause = new RuntimeException("Network error");

        // When
        ApiConnectionException exception1 = new ApiConnectionException(message);
        ApiConnectionException exception2 = new ApiConnectionException(message, cause);

        // Then
        assertEquals(message, exception1.getMessage());
        assertEquals(message, exception2.getMessage());
        assertEquals(cause, exception2.getCause());
        assertTrue(exception1 instanceof TradingSystemException);
    }

    @Test
    void shouldCreateInsufficientFundsException() {
        // Given
        String message = "Insufficient funds for trade";

        // When
        InsufficientFundsException exception = new InsufficientFundsException(message);

        // Then
        assertEquals(message, exception.getMessage());
        assertTrue(exception instanceof TradingSystemException);
    }

    @Test
    void shouldCreateInvalidOrderException() {
        // Given
        String message = "Invalid order parameters";

        // When
        InvalidOrderException exception = new InvalidOrderException(message);

        // Then
        assertEquals(message, exception.getMessage());
        assertTrue(exception instanceof TradingSystemException);
    }

    @Test
    void shouldCreateStrategyExecutionException() {
        // Given
        String strategyName = "SMA_STRATEGY";
        String message = "Strategy execution failed";

        // When
        StrategyExecutionException exception = new StrategyExecutionException(strategyName, message);

        // Then
        assertTrue(exception.getMessage().contains(strategyName));
        assertTrue(exception.getMessage().contains(message));
        assertEquals(strategyName, exception.getStrategyName());
        assertTrue(exception instanceof TradingSystemException);
    }

    @Test
    void shouldCreateMarketDataException() {
        // Given
        String symbol = "AAPL";
        String message = "Market data unavailable";

        // When
        MarketDataException exception = new MarketDataException(symbol, message);

        // Then
        assertTrue(exception.getMessage().contains(symbol));
        assertTrue(exception.getMessage().contains(message));
        assertEquals(symbol, exception.getSymbol());
        assertTrue(exception instanceof TradingSystemException);
    }

    @Test
    void shouldCreatePortfolioException() {
        // Given
        String message = "Portfolio calculation error";

        // When
        PortfolioException exception = new PortfolioException(message);

        // Then
        assertEquals(message, exception.getMessage());
        assertTrue(exception instanceof TradingSystemException);
    }

    @Test
    void shouldCreateConfigurationException() {
        // Given
        String configKey = "alpaca.api.key";
        String message = "Configuration value missing";

        // When
        ConfigurationException exception = new ConfigurationException(configKey, message);

        // Then
        assertTrue(exception.getMessage().contains(configKey));
        assertTrue(exception.getMessage().contains(message));
        assertEquals(configKey, exception.getConfigKey());
        assertTrue(exception instanceof TradingSystemException);
    }

    @Test
    void shouldMaintainExceptionHierarchy() {
        // Test that all custom exceptions extend TradingSystemException
        assertTrue(new ApiConnectionException("test") instanceof TradingSystemException);
        assertTrue(new InsufficientFundsException("test") instanceof TradingSystemException);
        assertTrue(new InvalidOrderException("test") instanceof TradingSystemException);
        assertTrue(new StrategyExecutionException("strategy", "test") instanceof TradingSystemException);
        assertTrue(new MarketDataException("AAPL", "test") instanceof TradingSystemException);
        assertTrue(new PortfolioException("test") instanceof TradingSystemException);
        assertTrue(new ConfigurationException("key", "test") instanceof TradingSystemException);

        // Test that TradingSystemException extends RuntimeException
        assertTrue(new TradingSystemException("test") instanceof RuntimeException);
    }
}