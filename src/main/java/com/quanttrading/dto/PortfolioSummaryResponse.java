package com.quanttrading.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for portfolio summary information
 */
public class PortfolioSummaryResponse {
    
    private String accountId;
    private BigDecimal totalValue;
    private BigDecimal cashBalance;
    private BigDecimal positionsValue;
    private BigDecimal totalUnrealizedPnL;
    private int positionCount;
    private LocalDateTime lastUpdated;
    
    // Constructors
    public PortfolioSummaryResponse() {}
    
    public PortfolioSummaryResponse(String accountId, BigDecimal totalValue, BigDecimal cashBalance,
                                  BigDecimal positionsValue, BigDecimal totalUnrealizedPnL,
                                  int positionCount, LocalDateTime lastUpdated) {
        this.accountId = accountId;
        this.totalValue = totalValue;
        this.cashBalance = cashBalance;
        this.positionsValue = positionsValue;
        this.totalUnrealizedPnL = totalUnrealizedPnL;
        this.positionCount = positionCount;
        this.lastUpdated = lastUpdated;
    }
    
    // Getters and Setters
    public String getAccountId() {
        return accountId;
    }
    
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
    
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
    
    public int getPositionCount() {
        return positionCount;
    }
    
    public void setPositionCount(int positionCount) {
        this.positionCount = positionCount;
    }
    
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}