package com.quanttrading.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for position information
 */
public class PositionResponse {
    
    private String symbol;
    private BigDecimal quantity;
    private BigDecimal averagePrice;
    private BigDecimal currentPrice;
    private BigDecimal marketValue;
    private BigDecimal unrealizedPnL;
    private BigDecimal unrealizedPnLPercent;
    private BigDecimal costBasis;
    private LocalDateTime lastUpdated;
    
    // Constructors
    public PositionResponse() {}
    
    public PositionResponse(String symbol, BigDecimal quantity, BigDecimal averagePrice,
                          BigDecimal currentPrice, BigDecimal marketValue, BigDecimal unrealizedPnL,
                          BigDecimal unrealizedPnLPercent, BigDecimal costBasis, LocalDateTime lastUpdated) {
        this.symbol = symbol;
        this.quantity = quantity;
        this.averagePrice = averagePrice;
        this.currentPrice = currentPrice;
        this.marketValue = marketValue;
        this.unrealizedPnL = unrealizedPnL;
        this.unrealizedPnLPercent = unrealizedPnLPercent;
        this.costBasis = costBasis;
        this.lastUpdated = lastUpdated;
    }
    
    // Getters and Setters
    public String getSymbol() {
        return symbol;
    }
    
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
    
    public BigDecimal getQuantity() {
        return quantity;
    }
    
    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
    
    public BigDecimal getAveragePrice() {
        return averagePrice;
    }
    
    public void setAveragePrice(BigDecimal averagePrice) {
        this.averagePrice = averagePrice;
    }
    
    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }
    
    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }
    
    public BigDecimal getMarketValue() {
        return marketValue;
    }
    
    public void setMarketValue(BigDecimal marketValue) {
        this.marketValue = marketValue;
    }
    
    public BigDecimal getUnrealizedPnL() {
        return unrealizedPnL;
    }
    
    public void setUnrealizedPnL(BigDecimal unrealizedPnL) {
        this.unrealizedPnL = unrealizedPnL;
    }
    
    public BigDecimal getUnrealizedPnLPercent() {
        return unrealizedPnLPercent;
    }
    
    public void setUnrealizedPnLPercent(BigDecimal unrealizedPnLPercent) {
        this.unrealizedPnLPercent = unrealizedPnLPercent;
    }
    
    public BigDecimal getCostBasis() {
        return costBasis;
    }
    
    public void setCostBasis(BigDecimal costBasis) {
        this.costBasis = costBasis;
    }
    
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}