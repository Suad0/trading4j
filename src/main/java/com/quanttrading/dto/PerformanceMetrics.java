package com.quanttrading.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO representing portfolio performance metrics
 */
public class PerformanceMetrics {
    
    private BigDecimal totalValue;
    private BigDecimal cashBalance;
    private BigDecimal positionsValue;
    private BigDecimal totalUnrealizedPnL;
    private BigDecimal dailyPnL;
    private BigDecimal dailyReturn;
    private BigDecimal totalReturn;
    private int positionCount;
    private LocalDateTime calculatedAt;
    
    // Constructors
    public PerformanceMetrics() {
        this.calculatedAt = LocalDateTime.now();
    }
    
    public PerformanceMetrics(BigDecimal totalValue, BigDecimal cashBalance, 
                            BigDecimal positionsValue, BigDecimal totalUnrealizedPnL) {
        this.totalValue = totalValue;
        this.cashBalance = cashBalance;
        this.positionsValue = positionsValue;
        this.totalUnrealizedPnL = totalUnrealizedPnL;
        this.calculatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public BigDecimal getTotalValue() {
        return totalValue;
    }
    
    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }
    
    public BigDecimal getCashBalance() {
        return cashBalance;
    }
    
    public void setCashBalance(BigDecimal cashBalance) {
        this.cashBalance = cashBalance;
    }
    
    public BigDecimal getPositionsValue() {
        return positionsValue;
    }
    
    public void setPositionsValue(BigDecimal positionsValue) {
        this.positionsValue = positionsValue;
    }
    
    public BigDecimal getTotalUnrealizedPnL() {
        return totalUnrealizedPnL;
    }
    
    public void setTotalUnrealizedPnL(BigDecimal totalUnrealizedPnL) {
        this.totalUnrealizedPnL = totalUnrealizedPnL;
    }
    
    public BigDecimal getDailyPnL() {
        return dailyPnL;
    }
    
    public void setDailyPnL(BigDecimal dailyPnL) {
        this.dailyPnL = dailyPnL;
    }
    
    public BigDecimal getDailyReturn() {
        return dailyReturn;
    }
    
    public void setDailyReturn(BigDecimal dailyReturn) {
        this.dailyReturn = dailyReturn;
    }
    
    public BigDecimal getTotalReturn() {
        return totalReturn;
    }
    
    public void setTotalReturn(BigDecimal totalReturn) {
        this.totalReturn = totalReturn;
    }
    
    public int getPositionCount() {
        return positionCount;
    }
    
    public void setPositionCount(int positionCount) {
        this.positionCount = positionCount;
    }
    
    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }
    
    public void setCalculatedAt(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }
    
    public BigDecimal getTotalPnL() {
        return totalUnrealizedPnL;
    }
}