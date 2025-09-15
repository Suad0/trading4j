package com.quanttrading.service.impl;

import com.quanttrading.model.MarketData;
import com.quanttrading.service.StrategyPerformance;
import com.quanttrading.service.StrategyService;
import com.quanttrading.service.TradingService;
import com.quanttrading.strategy.StrategyRegistry;
import com.quanttrading.strategy.TradingSignal;
import com.quanttrading.strategy.TradingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of StrategyService for managing and executing trading strategies.
 */
@Service
public class StrategyServiceImpl implements StrategyService {
    
    private static final Logger logger = LoggerFactory.getLogger(StrategyServiceImpl.class);
    
    private final StrategyRegistry strategyRegistry;
    private final TradingService tradingService;
    private final Map<String, StrategyPerformance> performanceMap = new ConcurrentHashMap<>();
    
    @Autowired
    public StrategyServiceImpl(StrategyRegistry strategyRegistry, TradingService tradingService) {
        this.strategyRegistry = strategyRegistry;
        this.tradingService = tradingService;
    }
    
    @Override
    public void registerStrategy(TradingStrategy strategy) {
        strategyRegistry.registerStrategy(strategy);
        performanceMap.put(strategy.getName(), new StrategyPerformance(strategy.getName()));
        logger.info("Registered strategy with performance tracking: {}", strategy.getName());
    }
    
    @Override
    public boolean unregisterStrategy(String strategyName) {
        TradingStrategy removed = strategyRegistry.unregisterStrategy(strategyName);
        if (removed != null) {
            performanceMap.remove(strategyName);
            logger.info("Unregistered strategy and removed performance tracking: {}", strategyName);
            return true;
        }
        return false;
    }
    
    @Override
    public TradingStrategy getStrategy(String strategyName) {
        return strategyRegistry.getStrategy(strategyName);
    }
    
    @Override
    public List<TradingStrategy> getAllStrategies() {
        return new ArrayList<>(strategyRegistry.getAllStrategies());
    }
    
    @Override
    public List<TradingStrategy> getEnabledStrategies() {
        return strategyRegistry.getEnabledStrategies();
    }
    
    @Override
    public Map<String, List<TradingSignal>> executeStrategies(MarketData marketData) {
        if (marketData == null) {
            logger.warn("Cannot execute strategies with null market data");
            return Map.of();
        }
        
        List<TradingStrategy> enabledStrategies = getEnabledStrategies();
        Map<String, List<TradingSignal>> results = new HashMap<>();
        
        logger.debug("Executing {} enabled strategies for symbol {}", 
                    enabledStrategies.size(), marketData.getSymbol());
        
        for (TradingStrategy strategy : enabledStrategies) {
            try {
                List<TradingSignal> signals = strategy.analyze(marketData);
                results.put(strategy.getName(), signals);
                
                // Update performance tracking
                StrategyPerformance performance = performanceMap.get(strategy.getName());
                if (performance != null && !signals.isEmpty()) {
                    performance.recordSignalGenerated();
                    
                    // Execute signals if they should be executed
                    for (TradingSignal signal : signals) {
                        if (strategy.shouldExecute(signal)) {
                            executeSignal(signal, performance);
                        }
                    }
                }
                
                logger.debug("Strategy {} generated {} signals", strategy.getName(), signals.size());
                
            } catch (Exception e) {
                logger.error("Error executing strategy {}: {}", strategy.getName(), e.getMessage(), e);
                results.put(strategy.getName(), List.of());
            }
        }
        
        return results;
    }
    
    @Override
    public List<TradingSignal> executeStrategy(String strategyName, MarketData marketData) {
        TradingStrategy strategy = strategyRegistry.getStrategy(strategyName);
        if (strategy == null) {
            logger.warn("Strategy not found: {}", strategyName);
            return List.of();
        }
        
        if (!strategy.isEnabled()) {
            logger.debug("Strategy {} is disabled", strategyName);
            return List.of();
        }
        
        if (marketData == null) {
            logger.warn("Cannot execute strategy {} with null market data", strategyName);
            return List.of();
        }
        
        try {
            List<TradingSignal> signals = strategy.analyze(marketData);
            
            // Update performance tracking
            StrategyPerformance performance = performanceMap.get(strategyName);
            if (performance != null && !signals.isEmpty()) {
                performance.recordSignalGenerated();
                
                // Execute signals if they should be executed
                for (TradingSignal signal : signals) {
                    if (strategy.shouldExecute(signal)) {
                        executeSignal(signal, performance);
                    }
                }
            }
            
            logger.debug("Strategy {} generated {} signals", strategyName, signals.size());
            return signals;
            
        } catch (Exception e) {
            logger.error("Error executing strategy {}: {}", strategyName, e.getMessage(), e);
            return List.of();
        }
    }
    
    @Override
    public boolean setStrategyEnabled(String strategyName, boolean enabled) {
        return strategyRegistry.setStrategyEnabled(strategyName, enabled);
    }
    
    @Override
    public Map<String, StrategyPerformance> getStrategyPerformance() {
        return new HashMap<>(performanceMap);
    }
    
    @Override
    public StrategyPerformance getStrategyPerformance(String strategyName) {
        return performanceMap.get(strategyName);
    }
    
    @Override
    public void resetPerformanceTracking() {
        performanceMap.values().forEach(StrategyPerformance::reset);
        logger.info("Reset performance tracking for all strategies");
    }
    
    @Override
    public boolean resetStrategyPerformance(String strategyName) {
        StrategyPerformance performance = performanceMap.get(strategyName);
        if (performance != null) {
            performance.reset();
            logger.info("Reset performance tracking for strategy: {}", strategyName);
            return true;
        }
        return false;
    }
    
    /**
     * Scheduled method to execute strategies periodically.
     * This method would typically be triggered by market data updates.
     */
    @Scheduled(fixedDelay = 60000) // Run every minute
    public void scheduledStrategyExecution() {
        // This is a placeholder for scheduled execution
        // In a real implementation, this would be triggered by market data updates
        // or run at specific intervals when markets are open
        
        List<TradingStrategy> enabledStrategies = getEnabledStrategies();
        if (enabledStrategies.isEmpty()) {
            return;
        }
        
        logger.debug("Scheduled strategy execution check - {} enabled strategies", enabledStrategies.size());
        
        // In a real implementation, you would:
        // 1. Fetch latest market data for all symbols
        // 2. Execute strategies with the latest data
        // 3. Handle any generated signals
        
        // For now, we just log that the scheduler is running
        // The actual execution will be triggered by market data updates
    }
    
    /**
     * Execute a trading signal and update performance metrics.
     */
    private void executeSignal(TradingSignal signal, StrategyPerformance performance) {
        try {
            logger.info("Executing signal: {} {} {} shares at {}", 
                       signal.getTradeType(), signal.getSymbol(), 
                       signal.getQuantity(), signal.getTargetPrice());
            
            // Execute the trade through TradingService
            switch (signal.getTradeType()) {
                case BUY -> tradingService.executeBuyOrder(signal.getSymbol(), 
                                                          signal.getQuantity(), 
                                                          null); // Using market order
                case SELL -> tradingService.executeSellOrder(signal.getSymbol(), 
                                                            signal.getQuantity(), 
                                                            null); // Using market order
            }
            
            // Update performance metrics
            performance.recordSignalExecuted(signal.getQuantity());
            
            logger.info("Successfully executed signal for strategy: {}", signal.getStrategyName());
            
        } catch (Exception e) {
            logger.error("Failed to execute signal for strategy {}: {}", 
                        signal.getStrategyName(), e.getMessage(), e);
        }
    }
    
    /**
     * Get the number of registered strategies.
     * @return number of registered strategies
     */
    public int getStrategyCount() {
        return strategyRegistry.getStrategyCount();
    }
    
    /**
     * Get the number of enabled strategies.
     * @return number of enabled strategies
     */
    public int getEnabledStrategyCount() {
        return strategyRegistry.getEnabledStrategyCount();
    }
}