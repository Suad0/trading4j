package com.quanttrading.config;

import com.quanttrading.exception.ConfigurationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ConfigurationValidatorTest {

    @Mock
    private StartupReadinessIndicator readinessIndicator;

    private ConfigurationValidator validator;

    @BeforeEach
    void setUp() {
        // Valid configurations for testing
        AlpacaConfig validAlpacaConfig = new AlpacaConfig(
            "https://paper-api.alpaca.markets",
            "valid-api-key",
            "valid-secret-key",
            true
        );
        
        TradingConfig validTradingConfig = new TradingConfig(
            new BigDecimal("10000.00"),
            new BigDecimal("0.02"),
            false,
            List.of("AAPL", "GOOGL")
        );
        
        SecurityProperties validSecurityProperties = new SecurityProperties(
            new SecurityProperties.BasicAuth("testuser", "testpass")
        );
        
        validator = new ConfigurationValidator(validAlpacaConfig, validTradingConfig, validSecurityProperties, readinessIndicator);
    }

    @Test
    void validateConfiguration_WithValidConfig_ShouldPass() {
        // Should not throw exception
        assertDoesNotThrow(() -> validator.validateConfiguration());
    }

    @Test
    void validateConfiguration_WithInvalidAlpacaConfig_ShouldFail() {
        AlpacaConfig invalidAlpacaConfig = new AlpacaConfig(
            "",
            "your-api-key-here",
            "",
            true
        );
        
        TradingConfig validTradingConfig = new TradingConfig(
            new BigDecimal("10000.00"),
            new BigDecimal("0.02"),
            false,
            List.of("AAPL")
        );
        
        SecurityProperties validSecurityProperties = new SecurityProperties(
            new SecurityProperties.BasicAuth("testuser", "testpass")
        );
        
        ConfigurationValidator invalidValidator = new ConfigurationValidator(
            invalidAlpacaConfig, validTradingConfig, validSecurityProperties, readinessIndicator);
        
        ConfigurationException exception = assertThrows(
            ConfigurationException.class,
            () -> invalidValidator.validateConfiguration()
        );
        
        assertTrue(exception.getMessage().contains("Alpaca base URL is required"));
        assertTrue(exception.getMessage().contains("Alpaca API key must be configured"));
        assertTrue(exception.getMessage().contains("Alpaca secret key is required"));
    }

    @Test
    void validateConfiguration_WithInvalidTradingConfig_ShouldFail() {
        AlpacaConfig validAlpacaConfig = new AlpacaConfig(
            "https://paper-api.alpaca.markets",
            "valid-api-key",
            "valid-secret-key",
            true
        );
        
        TradingConfig invalidTradingConfig = new TradingConfig(
            new BigDecimal("-1000.00"), // Invalid negative value
            new BigDecimal("0.6"), // Invalid risk > 50%
            false,
            List.of("INVALID_SYMBOL_123") // Invalid symbol format
        );
        
        SecurityProperties validSecurityProperties = new SecurityProperties(
            new SecurityProperties.BasicAuth("testuser", "testpass")
        );
        
        ConfigurationValidator invalidValidator = new ConfigurationValidator(
            validAlpacaConfig, invalidTradingConfig, validSecurityProperties, readinessIndicator);
        
        ConfigurationException exception = assertThrows(
            ConfigurationException.class,
            () -> invalidValidator.validateConfiguration()
        );
        
        assertTrue(exception.getMessage().contains("Max position size must be positive"));
        assertTrue(exception.getMessage().contains("Risk per trade must be between 0 and 0.5"));
        assertTrue(exception.getMessage().contains("Invalid symbol format"));
    }

    @Test
    void validateConfiguration_WithEmptySymbolList_ShouldFail() {
        AlpacaConfig validAlpacaConfig = new AlpacaConfig(
            "https://paper-api.alpaca.markets",
            "valid-api-key",
            "valid-secret-key",
            true
        );
        
        TradingConfig invalidTradingConfig = new TradingConfig(
            new BigDecimal("10000.00"),
            new BigDecimal("0.02"),
            false,
            List.of() // Empty symbol list
        );
        
        SecurityProperties validSecurityProperties = new SecurityProperties(
            new SecurityProperties.BasicAuth("testuser", "testpass")
        );
        
        ConfigurationValidator invalidValidator = new ConfigurationValidator(
            validAlpacaConfig, invalidTradingConfig, validSecurityProperties, readinessIndicator);
        
        ConfigurationException exception = assertThrows(
            ConfigurationException.class,
            () -> invalidValidator.validateConfiguration()
        );
        
        assertTrue(exception.getMessage().contains("At least one allowed symbol must be configured"));
    }

    @Test
    void validateConfiguration_WithInvalidSecurityConfig_ShouldFail() {
        AlpacaConfig validAlpacaConfig = new AlpacaConfig(
            "https://paper-api.alpaca.markets",
            "valid-api-key",
            "valid-secret-key",
            true
        );
        
        TradingConfig validTradingConfig = new TradingConfig(
            new BigDecimal("10000.00"),
            new BigDecimal("0.02"),
            false,
            List.of("AAPL")
        );
        
        SecurityProperties invalidSecurityProperties = new SecurityProperties(
            new SecurityProperties.BasicAuth("", "") // Empty credentials
        );
        
        ConfigurationValidator invalidValidator = new ConfigurationValidator(
            validAlpacaConfig, validTradingConfig, invalidSecurityProperties, readinessIndicator);
        
        ConfigurationException exception = assertThrows(
            ConfigurationException.class,
            () -> invalidValidator.validateConfiguration()
        );
        
        assertTrue(exception.getMessage().contains("Security username is required"));
        assertTrue(exception.getMessage().contains("Security password is required"));
    }
}