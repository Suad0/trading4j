package com.quanttrading.strategy;

import com.quanttrading.model.MarketData;
import java.util.List;

/**
 * Interface for trading strategies that analyze market data and generate trading signals.
 */
public interface TradingStrategy {
    
    /**
     * Get the unique name of this strategy.
     * @return strategy name
     */
    String getName();
    
    /**
     * Analyze market data and generate trading signals.
     * @param marketData current market data for analysis
     * @return list of trading signals generated
     */
    List<TradingSignal> analyze(MarketData marketData);
    
    /**
     * Determine if a trading signal should be executed based on strategy rules.
     * @param signal the trading signal to evaluate
     * @return true if the signal should be executed
     */
    boolean shouldExecute(TradingSignal signal);
    
    /**
     * Get the configuration for this strategy.
     * @return strategy configuration
     */
    StrategyConfig getConfig();
    
    /**
     * Update the strategy configuration.
     * @param config new configuration to apply
     */
    void updateConfig(StrategyConfig config);
    
    /**
     * Check if the strategy is currently enabled.
     * @return true if strategy is enabled
     */
    boolean isEnabled();
    
    /**
     * Enable or disable the strategy.
     * @param enabled true to enable, false to disable
     */
    void setEnabled(boolean enabled);
}