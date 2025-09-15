package com.quanttrading.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "positions")
public class Position {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    @NotBlank
    @Column(name = "symbol", nullable = false)
    private String symbol;
    
    @NotNull
    @Column(name = "quantity", precision = 19, scale = 8)
    private BigDecimal quantity;
    
    @NotNull
    @PositiveOrZero
    @Column(name = "average_price", precision = 19, scale = 4)
    private BigDecimal averagePrice;
    
    @PositiveOrZero
    @Column(name = "current_price", precision = 19, scale = 4)
    private BigDecimal currentPrice;
    
    @Column(name = "unrealized_pnl", precision = 19, scale = 4)
    private BigDecimal unrealizedPnL;
    
    @NotNull
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_account_id", referencedColumnName = "account_id")
    private Portfolio portfolio;
    
    // Constructors
    public Position() {
        this.lastUpdated = LocalDateTime.now();
    }
    
    public Position(String symbol, BigDecimal quantity, BigDecimal averagePrice) {
        this.symbol = symbol;
        this.quantity = quantity;
        this.averagePrice = averagePrice;
        this.lastUpdated = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
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
        this.lastUpdated = LocalDateTime.now();
        updateUnrealizedPnL();
    }
    
    public BigDecimal getAveragePrice() {
        return averagePrice;
    }
    
    public void setAveragePrice(BigDecimal averagePrice) {
        this.averagePrice = averagePrice;
        this.lastUpdated = LocalDateTime.now();
        updateUnrealizedPnL();
    }
    
    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }
    
    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
        this.lastUpdated = LocalDateTime.now();
        updateUnrealizedPnL();
    }
    
    public BigDecimal getUnrealizedPnL() {
        return unrealizedPnL;
    }
    
    public void setUnrealizedPnL(BigDecimal unrealizedPnL) {
        this.unrealizedPnL = unrealizedPnL;
    }
    
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    public Portfolio getPortfolio() {
        return portfolio;
    }
    
    public void setPortfolio(Portfolio portfolio) {
        this.portfolio = portfolio;
    }
    
    // Helper methods
    private void updateUnrealizedPnL() {
        if (currentPrice != null && averagePrice != null && quantity != null) {
            this.unrealizedPnL = currentPrice.subtract(averagePrice).multiply(quantity);
        }
    }
    
    public BigDecimal getMarketValue() {
        if (currentPrice != null && quantity != null) {
            return currentPrice.multiply(quantity);
        }
        return BigDecimal.ZERO;
    }
    
    @PreUpdate
    @PrePersist
    public void updateTimestamp() {
        this.lastUpdated = LocalDateTime.now();
        updateUnrealizedPnL();
    }
}