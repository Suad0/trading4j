package com.quanttrading.strategy;

import com.quanttrading.model.MarketData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * Abstract base class for trading strategies providing common functionality.
 */
public abstract class AbstractTradingStrategy implements TradingStrategy {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    protected StrategyConfig config;
    protected boolean enabled;
    
    protected AbstractTradingStrategy(StrategyConfig config) {
        this.config = Objects.requireNonNull(config, "Strategy config is required");
        this.enabled = config.isEnabled();
    }
    
    @Override
    public abstract String getName();
    
    @Override
    public List<TradingSignal> analyze(MarketData marketData) {
        if (!enabled) {
            logger.debug("Strategy {} is disabled, skipping analysis", getName());
            return List.of();
        }
        
        if (marketData == null) {
            logger.warn("Market data is null for strategy {}", getName());
            return List.of();
        }
        
        try {
            logger.debug("Analyzing market data for {} with strategy {}", 
                        marketData.getSymbol(), getName());
            return doAnalyze(marketData);
        } catch (Exception e) {
            logger.error("Error analyzing market data with strategy {}: {}", 
                        getName(), e.getMessage(), e);
            return List.of();
        }
    }
    
    /**
     * Perform the actual analysis logic. Subclasses must implement this method.
     * @param marketData market data to analyze
     * @return list of trading signals
     */
    protected abstract List<TradingSignal> doAnalyze(MarketData marketData);
    
    @Override
    public boolean shouldExecute(TradingSignal signal) {
        if (signal == null) {
            logger.warn("Trading signal is null for strategy {}", getName());
            return false;
        }
        
        if (!enabled) {
            logger.debug("Strategy {} is disabled, not executing signal", getName());
            return false;
        }
        
        // Check minimum confidence threshold
        if (signal.getConfidence() < config.getMinConfidence()) {
            logger.debug("Signal confidence {} below minimum {} for strategy {}", 
                        signal.getConfidence(), config.getMinConfidence(), getName());
            return false;
        }
        
        try {
            return doShouldExecute(signal);
        } catch (Exception e) {
            logger.error("Error evaluating signal execution for strategy {}: {}", 
                        getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Perform additional signal execution validation. Subclasses can override this method.
     * @param signal trading signal to evaluate
     * @return true if signal should be executed
     */
    protected boolean doShouldExecute(TradingSignal signal) {
        return true; // Default implementation allows execution
    }
    
    @Override
    public StrategyConfig getConfig() {
        return config;
    }
    
    @Override
    public void updateConfig(StrategyConfig config) {
        this.config = Objects.requireNonNull(config, "Strategy config is required");
        this.enabled = config.isEnabled();
        logger.info("Updated configuration for strategy {}", getName());
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        logger.info("Strategy {} {}", getName(), enabled ? "enabled" : "disabled");
    }
    
    /**
     * Validate that required parameters are present in the configuration.
     * @param requiredParams array of required parameter names
     * @throws IllegalArgumentException if any required parameter is missing
     */
    protected void validateRequiredParameters(String... requiredParams) {
        for (String param : requiredParams) {
            if (!config.getParameters().containsKey(param)) {
                throw new IllegalArgumentException("Required parameter '" + param + 
                                                 "' is missing for strategy " + getName());
            }
        }
    }
    
    /**
     * Get a parameter value with type checking and default value.
     * @param key parameter key
     * @param type expected parameter type
     * @param defaultValue default value if parameter is not found
     * @return parameter value or default value
     */
    protected <T> T getConfigParameter(String key, Class<T> type, T defaultValue) {
        return config.getParameter(key, type, defaultValue);
    }
}