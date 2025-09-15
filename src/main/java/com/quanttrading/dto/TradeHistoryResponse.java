package com.quanttrading.dto;

import com.quanttrading.model.OrderStatus;
import com.quanttrading.model.TradeType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for trade history information
 */
public class TradeHistoryResponse {
    
    private String orderId;
    private String symbol;
    private TradeType type;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal totalValue;
    private OrderStatus status;
    private LocalDateTime executedAt;
    private String strategyName;
    private String accountId;
    
    // Constructors
    public TradeHistoryResponse() {}
    
    public TradeHistoryResponse(String orderId, String symbol, TradeType type, BigDecimal quantity,
                              BigDecimal price, OrderStatus status, LocalDateTime executedAt,
                              String strategyName, String accountId) {
        this.orderId = orderId;
        this.symbol = symbol;
        this.type = type;
        this.quantity = quantity;
        this.price = price;
        this.totalValue = quantity.multiply(price);
        this.status = status;
        this.executedAt = executedAt;
        this.strategyName = strategyName;
        this.accountId = accountId;
    }
    
    // Getters and Setters
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
    
    public TradeType getType() {
        return type;
    }
    
    public void setType(TradeType type) {
        this.type = type;
    }
    
    public BigDecimal getQuantity() {
        return quantity;
    }
    
    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
        if (this.price != null) {
            this.totalValue = quantity.multiply(this.price);
        }
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
        if (this.quantity != null) {
            this.totalValue = this.quantity.multiply(price);
        }
    }
    
    public BigDecimal getTotalValue() {
        return totalValue;
    }
    
    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }
    
    public OrderStatus getStatus() {
        return status;
    }
    
    public void setStatus(OrderStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getExecutedAt() {
        return executedAt;
    }
    
    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }
    
    public String getStrategyName() {
        return strategyName;
    }
    
    public void setStrategyName(String strategyName) {
        this.strategyName = strategyName;
    }
    
    public String getAccountId() {
        return accountId;
    }
    
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
}