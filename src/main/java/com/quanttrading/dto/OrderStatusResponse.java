package com.quanttrading.dto;

import com.quanttrading.model.OrderStatus;

import java.time.LocalDateTime;

/**
 * Response DTO for order status information
 */
public class OrderStatusResponse {
    
    private String orderId;
    private OrderStatus status;
    private String statusMessage;
    private LocalDateTime lastUpdated;
    
    // Constructors
    public OrderStatusResponse() {}
    
    public OrderStatusResponse(String orderId, OrderStatus status) {
        this.orderId = orderId;
        this.status = status;
        this.lastUpdated = LocalDateTime.now();
    }
    
    public OrderStatusResponse(String orderId, OrderStatus status, String statusMessage) {
        this.orderId = orderId;
        this.status = status;
        this.statusMessage = statusMessage;
        this.lastUpdated = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public OrderStatus getStatus() {
        return status;
    }
    
    public void setStatus(OrderStatus status) {
        this.status = status;
    }
    
    public String getStatusMessage() {
        return statusMessage;
    }
    
    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
    
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}