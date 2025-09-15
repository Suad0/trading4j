package com.quanttrading.exception;

/**
 * Exception thrown when configuration validation fails
 */
public class ConfigurationException extends TradingSystemException {
    
    private final String configKey;
    
    public ConfigurationException(String configKey, String message) {
        super(String.format("Configuration error for '%s': %s", configKey, message));
        this.configKey = configKey;
    }
    
    public ConfigurationException(String configKey, String message, Throwable cause) {
        super(String.format("Configuration error for '%s': %s", configKey, message), cause);
        this.configKey = configKey;
    }
    
    public String getConfigKey() {
        return configKey;
    }
}