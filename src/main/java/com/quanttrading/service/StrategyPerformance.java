package com.quanttrading.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Performance metrics for a trading strategy.
 */
public class StrategyPerformance {
    
    private final String strategyName;
    private int totalSignalsGenerated;
    private int totalSignalsExecuted;
    private BigDecimal totalProfitLoss;
    private BigDecimal totalVolume;
    private int winningTrades;
    private int losingTrades;
    private BigDecimal largestWin;
    private BigDecimal largestLoss;
    private LocalDateTime firstSignalTime;
    private LocalDateTime lastSignalTime;
    private LocalDateTime lastUpdated;
    
    public StrategyPerformance(String strategyName) {
        this.strategyName = Objects.requireNonNull(strategyName, "Strategy name is required");
        this.totalSignalsGenerated = 0;
        this.totalSignalsExecuted = 0;
        this.totalProfitLoss = BigDecimal.ZERO;
        this.totalVolume = BigDecimal.ZERO;
        this.winningTrades = 0;
        this.losingTrades = 0;
        this.largestWin = BigDecimal.ZERO;
        this.largestLoss = BigDecimal.ZERO;
        this.lastUpdated = LocalDateTime.now();
    }
    
    /**
     * Record a signal generation.
     */
    public void recordSignalGenerated() {
        totalSignalsGenerated++;
        LocalDateTime now = LocalDateTime.now();
        if (firstSignalTime == null) {
            firstSignalTime = now;
        }
        lastSignalTime = now;
        lastUpdated = now;
    }
    
    /**
     * Record a signal execution.
     * @param volume trade volume
     */
    public void recordSignalExecuted(BigDecimal volume) {
        totalSignalsExecuted++;
        if (volume != null) {
            totalVolume = totalVolume.add(volume);
        }
        lastUpdated = LocalDateTime.now();
    }
    
    /**
     * Record trade profit/loss.
     * @param profitLoss profit or loss amount
     */
    public void recordTradeProfitLoss(BigDecimal profitLoss) {
        if (profitLoss == null) {
            return;
        }
        
        totalProfitLoss = totalProfitLoss.add(profitLoss);
        
        if (profitLoss.compareTo(BigDecimal.ZERO) > 0) {
            winningTrades++;
            if (profitLoss.compareTo(largestWin) > 0) {
                largestWin = profitLoss;
            }
        } else if (profitLoss.compareTo(BigDecimal.ZERO) < 0) {
            losingTrades++;
            if (profitLoss.compareTo(largestLoss) < 0) {
                largestLoss = profitLoss;
            }
        }
        
        lastUpdated = LocalDateTime.now();
    }
    
    /**
     * Get win rate as a percentage.
     * @return win rate (0.0 to 1.0)
     */
    public double getWinRate() {
        int totalTrades = winningTrades + losingTrades;
        return totalTrades > 0 ? (double) winningTrades / totalTrades : 0.0;
    }
    
    /**
     * Get signal execution rate as a percentage.
     * @return execution rate (0.0 to 1.0)
     */
    public double getExecutionRate() {
        return totalSignalsGenerated > 0 ? (double) totalSignalsExecuted / totalSignalsGenerated : 0.0;
    }
    
    /**
     * Get average profit per trade.
     * @return average profit per trade
     */
    public BigDecimal getAverageProfitPerTrade() {
        int totalTrades = winningTrades + losingTrades;
        return totalTrades > 0 ? totalProfitLoss.divide(BigDecimal.valueOf(totalTrades), 4, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
    }
    
    /**
     * Reset all performance metrics.
     */
    public void reset() {
        totalSignalsGenerated = 0;
        totalSignalsExecuted = 0;
        totalProfitLoss = BigDecimal.ZERO;
        totalVolume = BigDecimal.ZERO;
        winningTrades = 0;
        losingTrades = 0;
        largestWin = BigDecimal.ZERO;
        largestLoss = BigDecimal.ZERO;
        firstSignalTime = null;
        lastSignalTime = null;
        lastUpdated = LocalDateTime.now();
    }
    
    // Getters
    public String getStrategyName() {
        return strategyName;
    }
    
    public int getTotalSignalsGenerated() {
        return totalSignalsGenerated;
    }
    
    public int getTotalSignalsExecuted() {
        return totalSignalsExecuted;
    }
    
    public BigDecimal getTotalProfitLoss() {
        return totalProfitLoss;
    }
    
    public BigDecimal getTotalVolume() {
        return totalVolume;
    }
    
    public int getWinningTrades() {
        return winningTrades;
    }
    
    public int getLosingTrades() {
        return losingTrades;
    }
    
    public BigDecimal getLargestWin() {
        return largestWin;
    }
    
    public BigDecimal getLargestLoss() {
        return largestLoss;
    }
    
    public LocalDateTime getFirstSignalTime() {
        return firstSignalTime;
    }
    
    public LocalDateTime getLastSignalTime() {
        return lastSignalTime;
    }
    
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
    
    public int getTotalTrades() {
        return winningTrades + losingTrades;
    }
    
    public boolean isActive() {
        // Consider a strategy active if it has generated signals in the last 24 hours
        return lastSignalTime != null && 
               lastSignalTime.isAfter(LocalDateTime.now().minusDays(1));
    }
    
    @Override
    public String toString() {
        return "StrategyPerformance{" +
                "strategyName='" + strategyName + '\'' +
                ", totalSignalsGenerated=" + totalSignalsGenerated +
                ", totalSignalsExecuted=" + totalSignalsExecuted +
                ", totalProfitLoss=" + totalProfitLoss +
                ", winRate=" + String.format("%.2f%%", getWinRate() * 100) +
                ", executionRate=" + String.format("%.2f%%", getExecutionRate() * 100) +
                '}';
    }
}