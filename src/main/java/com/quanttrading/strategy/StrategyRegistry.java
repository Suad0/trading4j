package com.quanttrading.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing trading strategies.
 */
@Component
public class StrategyRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(StrategyRegistry.class);
    
    private final Map<String, TradingStrategy> strategies = new ConcurrentHashMap<>();
    
    /**
     * Register a trading strategy.
     * @param strategy the strategy to register
     * @throws IllegalArgumentException if strategy name is already registered
     */
    public void registerStrategy(TradingStrategy strategy) {
        Objects.requireNonNull(strategy, "Strategy cannot be null");
        String name = strategy.getName();
        
        if (strategies.containsKey(name)) {
            throw new IllegalArgumentException("Strategy with name '" + name + "' is already registered");
        }
        
        strategies.put(name, strategy);
        logger.info("Registered strategy: {}", name);
    }
    
    /**
     * Unregister a trading strategy.
     * @param strategyName name of the strategy to unregister
     * @return the unregistered strategy, or null if not found
     */
    public TradingStrategy unregisterStrategy(String strategyName) {
        TradingStrategy removed = strategies.remove(strategyName);
        if (removed != null) {
            logger.info("Unregistered strategy: {}", strategyName);
        } else {
            logger.warn("Attempted to unregister non-existent strategy: {}", strategyName);
        }
        return removed;
    }
    
    /**
     * Get a strategy by name.
     * @param strategyName name of the strategy
     * @return the strategy, or null if not found
     */
    public TradingStrategy getStrategy(String strategyName) {
        return strategies.get(strategyName);
    }
    
    /**
     * Get all registered strategies.
     * @return unmodifiable collection of all strategies
     */
    public Collection<TradingStrategy> getAllStrategies() {
        return Collections.unmodifiableCollection(strategies.values());
    }
    
    /**
     * Get all enabled strategies.
     * @return list of enabled strategies
     */
    public List<TradingStrategy> getEnabledStrategies() {
        return strategies.values().stream()
                .filter(TradingStrategy::isEnabled)
                .toList();
    }
    
    /**
     * Get strategy names.
     * @return set of all registered strategy names
     */
    public Set<String> getStrategyNames() {
        return Collections.unmodifiableSet(strategies.keySet());
    }
    
    /**
     * Check if a strategy is registered.
     * @param strategyName name of the strategy
     * @return true if strategy is registered
     */
    public boolean isStrategyRegistered(String strategyName) {
        return strategies.containsKey(strategyName);
    }
    
    /**
     * Get the number of registered strategies.
     * @return number of registered strategies
     */
    public int getStrategyCount() {
        return strategies.size();
    }
    
    /**
     * Get the number of enabled strategies.
     * @return number of enabled strategies
     */
    public int getEnabledStrategyCount() {
        return (int) strategies.values().stream()
                .filter(TradingStrategy::isEnabled)
                .count();
    }
    
    /**
     * Enable or disable a strategy.
     * @param strategyName name of the strategy
     * @param enabled true to enable, false to disable
     * @return true if strategy was found and updated
     */
    public boolean setStrategyEnabled(String strategyName, boolean enabled) {
        TradingStrategy strategy = strategies.get(strategyName);
        if (strategy != null) {
            strategy.setEnabled(enabled);
            logger.info("Strategy {} {}", strategyName, enabled ? "enabled" : "disabled");
            return true;
        }
        logger.warn("Attempted to {} non-existent strategy: {}", 
                   enabled ? "enable" : "disable", strategyName);
        return false;
    }
    
    /**
     * Update strategy configuration.
     * @param strategyName name of the strategy
     * @param config new configuration
     * @return true if strategy was found and updated
     */
    public boolean updateStrategyConfig(String strategyName, StrategyConfig config) {
        TradingStrategy strategy = strategies.get(strategyName);
        if (strategy != null) {
            strategy.updateConfig(config);
            logger.info("Updated configuration for strategy: {}", strategyName);
            return true;
        }
        logger.warn("Attempted to update config for non-existent strategy: {}", strategyName);
        return false;
    }
    
    /**
     * Clear all registered strategies.
     */
    public void clear() {
        int count = strategies.size();
        strategies.clear();
        logger.info("Cleared {} strategies from registry", count);
    }
}