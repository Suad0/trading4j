package com.quanttrading.client;

import com.quanttrading.dto.OrderRequest;
import com.quanttrading.dto.OrderResponse;
import com.quanttrading.dto.AccountInfo;
import com.quanttrading.model.OrderStatus;
import com.quanttrading.model.Position;

import java.util.List;

/**
 * Interface for Alpaca API client operations
 */
public interface AlpacaApiClient {
    
    /**
     * Place a trading order
     * @param request Order details
     * @return Order response with order ID and status
     */
    OrderResponse placeOrder(OrderRequest request);
    
    /**
     * Get order status by order ID
     * @param orderId Order identifier
     * @return Current order status
     */
    OrderStatus getOrderStatus(String orderId);
    
    /**
     * Get account information
     * @return Account details including balance and buying power
     */
    AccountInfo getAccountInfo();
    
    /**
     * Get current positions
     * @return List of current positions
     */
    List<Position> getPositions();
}