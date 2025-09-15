package com.quanttrading.config;

import com.quanttrading.exception.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class ConfigurationValidator {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationValidator.class);

    private final AlpacaConfig alpacaConfig;
    private final TradingConfig tradingConfig;
    private final SecurityProperties securityProperties;
    private final StartupReadinessIndicator readinessIndicator;

    public ConfigurationValidator(AlpacaConfig alpacaConfig, 
                                TradingConfig tradingConfig,
                                SecurityProperties securityProperties,
                                StartupReadinessIndicator readinessIndicator) {
        this.alpacaConfig = alpacaConfig;
        this.tradingConfig = tradingConfig;
        this.securityProperties = securityProperties;
        this.readinessIndicator = readinessIndicator;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateConfiguration() {
        logger.info("Validating application configuration...");
        
        List<String> errors = new ArrayList<>();
        
        validateAlpacaConfig(errors);
        validateTradingConfig(errors);
        validateSecurityConfig(errors);
        
        if (!errors.isEmpty()) {
            String errorMessage = "Configuration validation failed: " + String.join(", ", errors);
            logger.error(errorMessage);
            readinessIndicator.markStartupFailed(errorMessage);
            throw new ConfigurationException("application", errorMessage);
        }
        
        readinessIndicator.markConfigurationValidated();
        logger.info("Configuration validation completed successfully");
    }

    private void validateAlpacaConfig(List<String> errors) {
        if (alpacaConfig.baseUrl() == null || alpacaConfig.baseUrl().trim().isEmpty()) {
            errors.add("Alpaca base URL is required");
        }
        
        if (alpacaConfig.apiKey() == null || alpacaConfig.apiKey().trim().isEmpty()) {
            errors.add("Alpaca API key is required");
        }
        
        if (alpacaConfig.secretKey() == null || alpacaConfig.secretKey().trim().isEmpty()) {
            errors.add("Alpaca secret key is required");
        }
        
        if (alpacaConfig.apiKey() != null && alpacaConfig.apiKey().equals("your-api-key-here")) {
            errors.add("Alpaca API key must be configured with actual value");
        }
        
        if (alpacaConfig.secretKey() != null && alpacaConfig.secretKey().equals("your-secret-key-here")) {
            errors.add("Alpaca secret key must be configured with actual value");
        }
    }

    private void validateTradingConfig(List<String> errors) {
        if (tradingConfig.maxPositionSize().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Max position size must be positive");
        }
        
        if (tradingConfig.riskPerTrade().compareTo(BigDecimal.ZERO) <= 0 || 
            tradingConfig.riskPerTrade().compareTo(new BigDecimal("0.5")) > 0) {
            errors.add("Risk per trade must be between 0 and 0.5 (50%)");
        }
        
        if (tradingConfig.allowedSymbols() == null || tradingConfig.allowedSymbols().isEmpty()) {
            errors.add("At least one allowed symbol must be configured");
        }
        
        // Validate symbol format
        if (tradingConfig.allowedSymbols() != null) {
            for (String symbol : tradingConfig.allowedSymbols()) {
                if (symbol == null || !symbol.matches("^[A-Z]{1,5}$")) {
                    errors.add("Invalid symbol format: " + symbol);
                }
            }
        }
    }

    private void validateSecurityConfig(List<String> errors) {
        if (securityProperties.basicAuth().username() == null || 
            securityProperties.basicAuth().username().trim().isEmpty()) {
            errors.add("Security username is required");
        }
        
        if (securityProperties.basicAuth().password() == null || 
            securityProperties.basicAuth().password().trim().isEmpty()) {
            errors.add("Security password is required");
        }
        
        if (securityProperties.basicAuth().username() != null && 
            securityProperties.basicAuth().username().equals("admin") &&
            securityProperties.basicAuth().password() != null &&
            securityProperties.basicAuth().password().equals("password")) {
            logger.warn("Using default security credentials - change these in production!");
        }
    }
}