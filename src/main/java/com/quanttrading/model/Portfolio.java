package com.quanttrading.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "portfolios")
public class Portfolio {
    
    @Id
    @Column(name = "account_id")
    private String accountId;
    
    @NotNull
    @PositiveOrZero
    @Column(name = "cash_balance", precision = 19, scale = 4)
    private BigDecimal cashBalance;
    
    @NotNull
    @PositiveOrZero
    @Column(name = "total_value", precision = 19, scale = 4)
    private BigDecimal totalValue;
    
    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Position> positions = new ArrayList<>();
    
    @NotNull
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
    
    // Constructors
    public Portfolio() {
        this.lastUpdated = LocalDateTime.now();
    }
    
    public Portfolio(String accountId, BigDecimal cashBalance, BigDecimal totalValue) {
        this.accountId = accountId;
        this.cashBalance = cashBalance;
        this.totalValue = totalValue;
        this.lastUpdated = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getAccountId() {
        return accountId;
    }
    
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
    
    public BigDecimal getCashBalance() {
        return cashBalance;
    }
    
    public void setCashBalance(BigDecimal cashBalance) {
        this.cashBalance = cashBalance;
        this.lastUpdated = LocalDateTime.now();
    }
    
    public BigDecimal getTotalValue() {
        return totalValue;
    }
    
    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
        this.lastUpdated = LocalDateTime.now();
    }
    
    public List<Position> getPositions() {
        return positions;
    }
    
    public void setPositions(List<Position> positions) {
        this.positions = positions;
    }
    
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    // Helper methods
    public void addPosition(Position position) {
        positions.add(position);
        position.setPortfolio(this);
    }
    
    public void removePosition(Position position) {
        positions.remove(position);
        position.setPortfolio(null);
    }
    
    @PreUpdate
    @PrePersist
    public void updateTimestamp() {
        this.lastUpdated = LocalDateTime.now();
    }
}