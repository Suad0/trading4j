package com.quanttrading.service;

import com.quanttrading.metrics.TradingMetrics;
import com.quanttrading.model.TradeType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Service to integrate trading operations with metrics collection
 */
@Service
public class MetricsIntegrationService {

    private final TradingMetrics tradingMetrics;
    private final PortfolioService portfolioService;
    private final StrategyService strategyService;

    public MetricsIntegrationService(TradingMetrics tradingMetrics, 
                                   PortfolioService portfolioService,
                                   StrategyService strategyService) {
        this.tradingMetrics = tradingMetrics;
        this.portfolioService = portfolioService;
        this.strategyService = strategyService;
    }

    /**
     * Record a successful trade execution
     */
    public void recordSuccessfulTrade(TradeType tradeType, BigDecimal quantity, BigDecimal price) {
        tradingMetrics.incrementSuccessfulTrades();
        
        if (tradeType == TradeType.BUY) {
            tradingMetrics.incrementBuyOrders();
        } else if (tradeType == TradeType.SELL) {
            tradingMetrics.incrementSellOrders();
        }
        
        updatePortfolioMetrics();
    }

    /**
     * Record a failed trade attempt
     */
    public void recordFailedTrade(TradeType tradeType) {
        tradingMetrics.incrementFailedTrades();
    }

    /**
     * Update portfolio-related metrics
     */
    public void updatePortfolioMetrics() {
        try {
            var portfolio = portfolioService.getCurrentPortfolio();
            var positions = portfolioService.getPositions();
            var performance = portfolioService.calculatePerformance();
            
            tradingMetrics.updatePortfolioValue(portfolio.getTotalValue());
            tradingMetrics.updateTotalPnL(performance.getTotalUnrealizedPnL());
            tradingMetrics.updateDailyPnL(performance.getDailyPnL());
            tradingMetrics.updateActivePositionsCount(positions.size());
            
        } catch (Exception e) {
            // Log error but don't fail the operation
            // This is metrics collection, not critical business logic
        }
    }

    /**
     * Update strategy-related metrics
     */
    public void updateStrategyMetrics() {
        try {
            var strategies = strategyService.getStrategyPerformance();
            long activeStrategies = strategies.values().stream()
                .mapToLong(s -> s.isActive() ? 1 : 0)
                .sum();
            
            tradingMetrics.updateActiveStrategiesCount((int) activeStrategies);
            
        } catch (Exception e) {
            // Log error but don't fail the operation
        }
    }

    /**
     * Get trading metrics instance for direct timer usage
     */
    public TradingMetrics getTradingMetrics() {
        return tradingMetrics;
    }
}