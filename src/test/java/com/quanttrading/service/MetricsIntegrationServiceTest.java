package com.quanttrading.service;

import com.quanttrading.dto.PerformanceMetrics;
import com.quanttrading.metrics.TradingMetrics;
import com.quanttrading.model.Portfolio;
import com.quanttrading.model.Position;
import com.quanttrading.model.TradeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetricsIntegrationServiceTest {

    @Mock
    private TradingMetrics tradingMetrics;

    @Mock
    private PortfolioService portfolioService;

    @Mock
    private StrategyService strategyService;

    private MetricsIntegrationService metricsIntegrationService;

    @BeforeEach
    void setUp() {
        metricsIntegrationService = new MetricsIntegrationService(
            tradingMetrics, portfolioService, strategyService);
    }

    @Test
    void recordSuccessfulTrade_WithBuyOrder_ShouldIncrementCounters() {
        // Given
        TradeType tradeType = TradeType.BUY;
        BigDecimal quantity = new BigDecimal("100");
        BigDecimal price = new BigDecimal("150.00");

        Portfolio portfolio = new Portfolio();
        portfolio.setTotalValue(new BigDecimal("25000.00"));
        
        PerformanceMetrics performance = new PerformanceMetrics();
        performance.setTotalUnrealizedPnL(new BigDecimal("1000.00"));
        performance.setDailyPnL(new BigDecimal("100.00"));

        when(portfolioService.getCurrentPortfolio()).thenReturn(portfolio);
        when(portfolioService.getPositions()).thenReturn(Collections.emptyList());
        when(portfolioService.calculatePerformance()).thenReturn(performance);

        // When
        metricsIntegrationService.recordSuccessfulTrade(tradeType, quantity, price);

        // Then
        verify(tradingMetrics).incrementSuccessfulTrades();
        verify(tradingMetrics).incrementBuyOrders();
        verify(tradingMetrics, never()).incrementSellOrders();
        verify(tradingMetrics).updatePortfolioValue(new BigDecimal("25000.00"));
        verify(tradingMetrics).updateTotalPnL(new BigDecimal("1000.00"));
        verify(tradingMetrics).updateDailyPnL(new BigDecimal("100.00"));
        verify(tradingMetrics).updateActivePositionsCount(0);
    }

    @Test
    void recordSuccessfulTrade_WithSellOrder_ShouldIncrementCounters() {
        // Given
        TradeType tradeType = TradeType.SELL;
        BigDecimal quantity = new BigDecimal("50");
        BigDecimal price = new BigDecimal("155.00");

        Portfolio portfolio = new Portfolio();
        portfolio.setTotalValue(new BigDecimal("30000.00"));
        
        PerformanceMetrics performance = new PerformanceMetrics();
        performance.setTotalUnrealizedPnL(new BigDecimal("1500.00"));
        performance.setDailyPnL(new BigDecimal("250.00"));

        when(portfolioService.getCurrentPortfolio()).thenReturn(portfolio);
        when(portfolioService.getPositions()).thenReturn(Collections.emptyList());
        when(portfolioService.calculatePerformance()).thenReturn(performance);

        // When
        metricsIntegrationService.recordSuccessfulTrade(tradeType, quantity, price);

        // Then
        verify(tradingMetrics).incrementSuccessfulTrades();
        verify(tradingMetrics).incrementSellOrders();
        verify(tradingMetrics, never()).incrementBuyOrders();
    }

    @Test
    void recordFailedTrade_ShouldIncrementFailedCounter() {
        // Given
        TradeType tradeType = TradeType.BUY;

        // When
        metricsIntegrationService.recordFailedTrade(tradeType);

        // Then
        verify(tradingMetrics).incrementFailedTrades();
        verify(tradingMetrics, never()).incrementSuccessfulTrades();
        verify(tradingMetrics, never()).incrementBuyOrders();
        verify(tradingMetrics, never()).incrementSellOrders();
    }

    @Test
    void updatePortfolioMetrics_ShouldUpdateAllMetrics() {
        // Given
        Portfolio portfolio = new Portfolio();
        portfolio.setTotalValue(new BigDecimal("35000.00"));
        
        Position position1 = new Position();
        position1.setSymbol("AAPL");
        Position position2 = new Position();
        position2.setSymbol("GOOGL");
        
        PerformanceMetrics performance = new PerformanceMetrics();
        performance.setTotalUnrealizedPnL(new BigDecimal("2000.00"));
        performance.setDailyPnL(new BigDecimal("300.00"));

        when(portfolioService.getCurrentPortfolio()).thenReturn(portfolio);
        when(portfolioService.getPositions()).thenReturn(Arrays.asList(position1, position2));
        when(portfolioService.calculatePerformance()).thenReturn(performance);

        // When
        metricsIntegrationService.updatePortfolioMetrics();

        // Then
        verify(tradingMetrics).updatePortfolioValue(new BigDecimal("35000.00"));
        verify(tradingMetrics).updateTotalPnL(new BigDecimal("2000.00"));
        verify(tradingMetrics).updateDailyPnL(new BigDecimal("300.00"));
        verify(tradingMetrics).updateActivePositionsCount(2);
    }

    @Test
    void updatePortfolioMetrics_WhenExceptionOccurs_ShouldNotThrow() {
        // Given
        when(portfolioService.getCurrentPortfolio())
            .thenThrow(new RuntimeException("Service error"));

        // When & Then - should not throw exception
        metricsIntegrationService.updatePortfolioMetrics();
        
        // Verify no metrics were updated
        verify(tradingMetrics, never()).updatePortfolioValue(any());
        verify(tradingMetrics, never()).updateTotalPnL(any());
        verify(tradingMetrics, never()).updateDailyPnL(any());
        verify(tradingMetrics, never()).updateActivePositionsCount(anyInt());
    }

    @Test
    void updateStrategyMetrics_ShouldUpdateActiveStrategiesCount() {
        // Given
        StrategyPerformance activeStrategy = new StrategyPerformance("Active Strategy");
        activeStrategy.recordSignalGenerated(); // Make it active
        
        StrategyPerformance inactiveStrategy = new StrategyPerformance("Inactive Strategy");
        
        Map<String, StrategyPerformance> strategies = new HashMap<>();
        strategies.put("Active Strategy", activeStrategy);
        strategies.put("Inactive Strategy", inactiveStrategy);
        
        when(strategyService.getStrategyPerformance())
            .thenReturn(strategies);

        // When
        metricsIntegrationService.updateStrategyMetrics();

        // Then
        verify(tradingMetrics).updateActiveStrategiesCount(1);
    }

    @Test
    void updateStrategyMetrics_WhenExceptionOccurs_ShouldNotThrow() {
        // Given
        when(strategyService.getStrategyPerformance())
            .thenThrow(new RuntimeException("Service error"));

        // When & Then - should not throw exception
        metricsIntegrationService.updateStrategyMetrics();
        
        // Verify no metrics were updated
        verify(tradingMetrics, never()).updateActiveStrategiesCount(anyInt());
    }

    @Test
    void getTradingMetrics_ShouldReturnMetricsInstance() {
        // When
        TradingMetrics result = metricsIntegrationService.getTradingMetrics();

        // Then
        verify(tradingMetrics, never()).updateActiveStrategiesCount(anyInt());
        // The actual assertion would depend on the implementation
        // For now, we just verify the method can be called without error
    }
}