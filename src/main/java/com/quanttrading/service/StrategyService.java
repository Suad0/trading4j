package com.quanttrading.service;

import com.quanttrading.strategy.TradingStrategy;
import com.quanttrading.strategy.TradingSignal;
import com.quanttrading.model.MarketData;

import java.util.List;
import java.util.Map;

/**
 * Service for managing and executing trading strategies.
 */
public interface StrategyService {
    
    /**
     * Register a trading strategy.
     * @param strategy the strategy to register
     */
    void registerStrategy(TradingStrategy strategy);
    
    /**
     * Unregister a trading strategy.
     * @param strategyName name of the strategy to unregister
     * @return true if strategy was found and removed
     */
    boolean unregisterStrategy(String strategyName);
    
    /**
     * Get a strategy by name.
     * @param strategyName name of the strategy
     * @return the strategy, or null if not found
     */
    TradingStrategy getStrategy(String strategyName);
    
    /**
     * Get all registered strategies.
     * @return list of all strategies
     */
    List<TradingStrategy> getAllStrategies();
    
    /**
     * Get all enabled strategies.
     * @return list of enabled strategies
     */
    List<TradingStrategy> getEnabledStrategies();
    
    /**
     * Execute all enabled strategies with the given market data.
     * @param marketData market data to analyze
     * @return map of strategy names to generated signals
     */
    Map<String, List<TradingSignal>> executeStrategies(MarketData marketData);
    
    /**
     * Execute a specific strategy with the given market data.
     * @param strategyName name of the strategy to execute
     * @param marketData market data to analyze
     * @return list of generated signals
     */
    List<TradingSignal> executeStrategy(String strategyName, MarketData marketData);
    
    /**
     * Enable or disable a strategy.
     * @param strategyName name of the strategy
     * @param enabled true to enable, false to disable
     * @return true if strategy was found and updated
     */
    boolean setStrategyEnabled(String strategyName, boolean enabled);
    
    /**
     * Get strategy performance metrics.
     * @return map of strategy names to performance data
     */
    Map<String, StrategyPerformance> getStrategyPerformance();
    
    /**
     * Get performance metrics for a specific strategy.
     * @param strategyName name of the strategy
     * @return performance data, or null if strategy not found
     */
    StrategyPerformance getStrategyPerformance(String strategyName);
    
    /**
     * Reset performance tracking for all strategies.
     */
    void resetPerformanceTracking();
    
    /**
     * Reset performance tracking for a specific strategy.
     * @param strategyName name of the strategy
     * @return true if strategy was found and reset
     */
    boolean resetStrategyPerformance(String strategyName);
}