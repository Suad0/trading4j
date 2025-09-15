package com.quanttrading.service;

import com.quanttrading.dto.OrderRequest;
import com.quanttrading.dto.OrderResponse;
import com.quanttrading.model.OrderStatus;
import com.quanttrading.model.Trade;
import com.quanttrading.model.TradeType;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service interface for trading operations
 */
public interface TradingService {
    
    /**
     * Execute a buy order
     * @param symbol Stock symbol
     * @param quantity Quantity to buy
     * @param orderType Order type (MARKET, LIMIT, etc.)
     * @return Order response with order ID and status
     */
    OrderResponse executeBuyOrder(String symbol, BigDecimal quantity, String orderType);
    
    /**
     * Execute a buy order with limit price
     * @param symbol Stock symbol
     * @param quantity Quantity to buy
     * @param orderType Order type
     * @param limitPrice Limit price for the order
     * @return Order response with order ID and status
     */
    OrderResponse executeBuyOrder(String symbol, BigDecimal quantity, String orderType, BigDecimal limitPrice);
    
    /**
     * Execute a sell order
     * @param symbol Stock symbol
     * @param quantity Quantity to sell
     * @param orderType Order type (MARKET, LIMIT, etc.)
     * @return Order response with order ID and status
     */
    OrderResponse executeSellOrder(String symbol, BigDecimal quantity, String orderType);
    
    /**
     * Execute a sell order with limit price
     * @param symbol Stock symbol
     * @param quantity Quantity to sell
     * @param orderType Order type
     * @param limitPrice Limit price for the order
     * @return Order response with order ID and status
     */
    OrderResponse executeSellOrder(String symbol, BigDecimal quantity, String orderType, BigDecimal limitPrice);
    
    /**
     * Get order status by order ID
     * @param orderId Order identifier
     * @return Current order status
     */
    OrderStatus getOrderStatus(String orderId);
    
    /**
     * Get trade history for all accounts
     * @return List of all trades
     */
    List<Trade> getTradeHistory();
    
    /**
     * Get trade history for a specific account
     * @param accountId Account identifier
     * @return List of trades for the account
     */
    List<Trade> getTradeHistory(String accountId);
    
    /**
     * Get recent trades (last N trades)
     * @param limit Maximum number of trades to return
     * @return List of recent trades
     */
    List<Trade> getRecentTrades(int limit);
    
    /**
     * Get trades by symbol
     * @param symbol Stock symbol
     * @return List of trades for the symbol
     */
    List<Trade> getTradesBySymbol(String symbol);
    
    /**
     * Cancel an order
     * @param orderId Order identifier
     * @return True if cancellation was successful
     */
    boolean cancelOrder(String orderId);
    
    /**
     * Validate order before execution
     * @param symbol Stock symbol
     * @param quantity Quantity
     * @param tradeType Trade type (BUY/SELL)
     * @param price Price (optional for market orders)
     * @return True if order is valid
     */
    boolean validateOrder(String symbol, BigDecimal quantity, TradeType tradeType, BigDecimal price);
    
    /**
     * Check if sufficient funds are available for a buy order
     * @param symbol Stock symbol
     * @param quantity Quantity to buy
     * @param price Price per share
     * @return True if sufficient funds available
     */
    boolean hasSufficientFunds(String symbol, BigDecimal quantity, BigDecimal price);
    
    /**
     * Check if sufficient shares are available for a sell order
     * @param symbol Stock symbol
     * @param quantity Quantity to sell
     * @return True if sufficient shares available
     */
    boolean hasSufficientShares(String symbol, BigDecimal quantity);
}