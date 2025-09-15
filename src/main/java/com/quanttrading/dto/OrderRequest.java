package com.quanttrading.dto;

import com.quanttrading.model.TradeType;
import com.quanttrading.validation.ValidOrderType;
import com.quanttrading.validation.ValidSymbol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * DTO for order placement requests
 */
public class OrderRequest {
    
    @NotBlank(message = "Symbol is required")
    @ValidSymbol
    private String symbol;
    
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private BigDecimal quantity;
    
    @NotNull(message = "Trade type is required")
    private TradeType type;
    
    @NotBlank(message = "Order type is required")
    @ValidOrderType
    private String orderType; // "market", "limit", "stop"
    
    private BigDecimal limitPrice; // Required for limit orders
    
    private BigDecimal stopPrice; // Required for stop orders
    
    private String timeInForce = "day"; // "day", "gtc", "ioc", "fok"
    
    // Constructors
    public OrderRequest() {}
    
    public OrderRequest(String symbol, BigDecimal quantity, TradeType type, String orderType) {
        this.symbol = symbol;
        this.quantity = quantity;
        this.type = type;
        this.orderType = orderType;
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
    
    public TradeType getType() {
        return type;
    }
    
    public void setType(TradeType type) {
        this.type = type;
    }
    
    public String getOrderType() {
        return orderType;
    }
    
    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }
    
    public BigDecimal getLimitPrice() {
        return limitPrice;
    }
    
    public void setLimitPrice(BigDecimal limitPrice) {
        this.limitPrice = limitPrice;
    }
    
    public BigDecimal getStopPrice() {
        return stopPrice;
    }
    
    public void setStopPrice(BigDecimal stopPrice) {
        this.stopPrice = stopPrice;
    }
    
    public String getTimeInForce() {
        return timeInForce;
    }
    
    public void setTimeInForce(String timeInForce) {
        this.timeInForce = timeInForce;
    }
}