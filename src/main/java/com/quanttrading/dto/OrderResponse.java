package com.quanttrading.dto;

import com.quanttrading.model.OrderStatus;
import com.quanttrading.model.TradeType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for order placement responses
 */
public class OrderResponse {
    
    private String orderId;
    private String symbol;
    private BigDecimal quantity;
    private TradeType type;
    private String orderType;
    private OrderStatus status;
    private BigDecimal filledPrice;
    private BigDecimal filledQuantity;
    private LocalDateTime submittedAt;
    private LocalDateTime filledAt;
    private String message;
    
    // Constructors
    public OrderResponse() {}
    
    public OrderResponse(String orderId, String symbol, BigDecimal quantity, TradeType type, OrderStatus status) {
        this.orderId = orderId;
        this.symbol = symbol;
        this.quantity = quantity;
        this.type = type;
        this.status = status;
        this.submittedAt = LocalDateTime.now();
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
    
    public OrderStatus getStatus() {
        return status;
    }
    
    public void setStatus(OrderStatus status) {
        this.status = status;
    }
    
    public BigDecimal getFilledPrice() {
        return filledPrice;
    }
    
    public void setFilledPrice(BigDecimal filledPrice) {
        this.filledPrice = filledPrice;
    }
    
    public BigDecimal getFilledQuantity() {
        return filledQuantity;
    }
    
    public void setFilledQuantity(BigDecimal filledQuantity) {
        this.filledQuantity = filledQuantity;
    }
    
    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }
    
    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
    
    public LocalDateTime getFilledAt() {
        return filledAt;
    }
    
    public void setFilledAt(LocalDateTime filledAt) {
        this.filledAt = filledAt;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}