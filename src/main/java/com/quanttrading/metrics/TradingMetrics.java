package com.quanttrading.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Custom metrics for trading system performance and health
 */
@Component
public class TradingMetrics {

    private final Counter successfulTradesCounter;
    private final Counter failedTradesCounter;
    private final Counter buyOrdersCounter;
    private final Counter sellOrdersCounter;
    private final Timer orderExecutionTimer;
    private final Timer marketDataFetchTimer;
    private final Timer strategyExecutionTimer;
    
    private final AtomicReference<BigDecimal> totalPortfolioValue = new AtomicReference<>(BigDecimal.ZERO);
    private final AtomicReference<BigDecimal> totalPnL = new AtomicReference<>(BigDecimal.ZERO);
    private final AtomicReference<BigDecimal> dailyPnL = new AtomicReference<>(BigDecimal.ZERO);
    private final AtomicInteger activePositionsCount = new AtomicInteger(0);
    private final AtomicInteger activeStrategiesCount = new AtomicInteger(0);

    public TradingMetrics(MeterRegistry meterRegistry) {
        // Counters for trade operations
        this.successfulTradesCounter = Counter.builder("trading.trades.successful")
            .description("Number of successful trades executed")
            .register(meterRegistry);
            
        this.failedTradesCounter = Counter.builder("trading.trades.failed")
            .description("Number of failed trade attempts")
            .register(meterRegistry);
            
        this.buyOrdersCounter = Counter.builder("trading.orders.buy")
            .description("Number of buy orders placed")
            .register(meterRegistry);
            
        this.sellOrdersCounter = Counter.builder("trading.orders.sell")
            .description("Number of sell orders placed")
            .register(meterRegistry);

        // Timers for performance monitoring
        this.orderExecutionTimer = Timer.builder("trading.order.execution.time")
            .description("Time taken to execute orders")
            .register(meterRegistry);
            
        this.marketDataFetchTimer = Timer.builder("trading.marketdata.fetch.time")
            .description("Time taken to fetch market data")
            .register(meterRegistry);
            
        this.strategyExecutionTimer = Timer.builder("trading.strategy.execution.time")
            .description("Time taken to execute trading strategies")
            .register(meterRegistry);

        // Gauges for current state
        Gauge.builder("trading.portfolio.value", this, TradingMetrics::getPortfolioValue)
            .description("Current total portfolio value")
            .register(meterRegistry);
            
        Gauge.builder("trading.pnl.total", this, TradingMetrics::getTotalPnL)
            .description("Total profit and loss")
            .register(meterRegistry);
            
        Gauge.builder("trading.pnl.daily", this, TradingMetrics::getDailyPnL)
            .description("Daily profit and loss")
            .register(meterRegistry);
            
        Gauge.builder("trading.positions.active", this, TradingMetrics::getActivePositionsCount)
            .description("Number of active positions")
            .register(meterRegistry);
            
        Gauge.builder("trading.strategies.active", this, TradingMetrics::getActiveStrategiesCount)
            .description("Number of active trading strategies")
            .register(meterRegistry);
    }

    // Counter methods
    public void incrementSuccessfulTrades() {
        successfulTradesCounter.increment();
    }

    public void incrementFailedTrades() {
        failedTradesCounter.increment();
    }

    public void incrementBuyOrders() {
        buyOrdersCounter.increment();
    }

    public void incrementSellOrders() {
        sellOrdersCounter.increment();
    }

    // Timer methods
    public Timer.Sample startOrderExecutionTimer() {
        return Timer.start();
    }

    public void recordOrderExecutionTime(Timer.Sample sample) {
        sample.stop(orderExecutionTimer);
    }

    public Timer.Sample startMarketDataFetchTimer() {
        return Timer.start();
    }

    public void recordMarketDataFetchTime(Timer.Sample sample) {
        sample.stop(marketDataFetchTimer);
    }

    public Timer.Sample startStrategyExecutionTimer() {
        return Timer.start();
    }

    public void recordStrategyExecutionTime(Timer.Sample sample) {
        sample.stop(strategyExecutionTimer);
    }

    // Gauge update methods
    public void updatePortfolioValue(BigDecimal value) {
        totalPortfolioValue.set(value);
    }

    public void updateTotalPnL(BigDecimal pnl) {
        totalPnL.set(pnl);
    }

    public void updateDailyPnL(BigDecimal pnl) {
        dailyPnL.set(pnl);
    }

    public void updateActivePositionsCount(int count) {
        activePositionsCount.set(count);
    }

    public void updateActiveStrategiesCount(int count) {
        activeStrategiesCount.set(count);
    }

    // Getter methods for gauges
    private double getPortfolioValue() {
        return totalPortfolioValue.get().doubleValue();
    }

    private double getTotalPnL() {
        return totalPnL.get().doubleValue();
    }

    private double getDailyPnL() {
        return dailyPnL.get().doubleValue();
    }

    private double getActivePositionsCount() {
        return activePositionsCount.get();
    }

    private double getActiveStrategiesCount() {
        return activeStrategiesCount.get();
    }
}