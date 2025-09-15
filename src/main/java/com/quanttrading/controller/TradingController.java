package com.quanttrading.controller;

import com.quanttrading.dto.*;
import com.quanttrading.exception.InsufficientFundsException;
import com.quanttrading.exception.InvalidOrderException;
import com.quanttrading.exception.TradingSystemException;
import com.quanttrading.model.OrderStatus;
import com.quanttrading.model.Trade;
import com.quanttrading.model.TradeType;
import com.quanttrading.service.TradingService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for trading operations
 */
@RestController
@RequestMapping("/api/trading")
@CrossOrigin(origins = "*")
public class TradingController {
    
    private static final Logger logger = LoggerFactory.getLogger(TradingController.class);
    
    private final TradingService tradingService;
    
    @Autowired
    public TradingController(TradingService tradingService) {
        this.tradingService = tradingService;
    }
    
    /**
     * Execute a buy order
     * @param orderRequest Order details
     * @return Order response with order ID and status
     */
    @PostMapping("/buy")
    public ResponseEntity<OrderResponse> executeBuyOrder(@RequestBody OrderRequest orderRequest) {
        try {
            // Set the trade type for buy orders
            orderRequest.setType(TradeType.BUY);
            
            logger.info("Executing buy order for symbol: {}, quantity: {}", 
                       orderRequest.getSymbol(), orderRequest.getQuantity());
            
            OrderResponse response;
            if (orderRequest.getLimitPrice() != null) {
                response = tradingService.executeBuyOrder(
                    orderRequest.getSymbol(),
                    orderRequest.getQuantity(),
                    orderRequest.getOrderType(),
                    orderRequest.getLimitPrice()
                );
            } else {
                response = tradingService.executeBuyOrder(
                    orderRequest.getSymbol(),
                    orderRequest.getQuantity(),
                    orderRequest.getOrderType()
                );
            }
            
            logger.info("Buy order executed successfully - Order ID: {}", response.getOrderId());
            return ResponseEntity.ok(response);
            
        } catch (InvalidOrderException e) {
            logger.error("Invalid buy order request", e);
            return ResponseEntity.badRequest().build();
        } catch (InsufficientFundsException e) {
            logger.error("Insufficient funds for buy order", e);
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).build();
        } catch (TradingSystemException e) {
            logger.error("Trading system error during buy order", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Execute a sell order
     * @param orderRequest Order details
     * @return Order response with order ID and status
     */
    @PostMapping("/sell")
    public ResponseEntity<OrderResponse> executeSellOrder(@RequestBody OrderRequest orderRequest) {
        try {
            // Set the trade type for sell orders
            orderRequest.setType(TradeType.SELL);
            
            logger.info("Executing sell order for symbol: {}, quantity: {}", 
                       orderRequest.getSymbol(), orderRequest.getQuantity());
            
            OrderResponse response;
            if (orderRequest.getLimitPrice() != null) {
                response = tradingService.executeSellOrder(
                    orderRequest.getSymbol(),
                    orderRequest.getQuantity(),
                    orderRequest.getOrderType(),
                    orderRequest.getLimitPrice()
                );
            } else {
                response = tradingService.executeSellOrder(
                    orderRequest.getSymbol(),
                    orderRequest.getQuantity(),
                    orderRequest.getOrderType()
                );
            }
            
            logger.info("Sell order executed successfully - Order ID: {}", response.getOrderId());
            return ResponseEntity.ok(response);
            
        } catch (InvalidOrderException e) {
            logger.error("Invalid sell order request", e);
            return ResponseEntity.badRequest().build();
        } catch (InsufficientFundsException e) {
            logger.error("Insufficient shares for sell order", e);
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).build();
        } catch (TradingSystemException e) {
            logger.error("Trading system error during sell order", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get order status by order ID
     * @param orderId Order identifier
     * @return Order status information
     */
    @GetMapping("/orders/{orderId}/status")
    public ResponseEntity<OrderStatusResponse> getOrderStatus(@PathVariable String orderId) {
        try {
            logger.debug("Getting order status for order ID: {}", orderId);
            
            OrderStatus status = tradingService.getOrderStatus(orderId);
            OrderStatusResponse response = new OrderStatusResponse(orderId, status);
            
            logger.debug("Order status retrieved - Order ID: {}, Status: {}", orderId, status);
            return ResponseEntity.ok(response);
            
        } catch (TradingSystemException e) {
            logger.error("Error retrieving order status for order ID: {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Cancel an order
     * @param orderId Order identifier
     * @return Success status
     */
    @DeleteMapping("/orders/{orderId}")
    public ResponseEntity<Void> cancelOrder(@PathVariable String orderId) {
        try {
            logger.info("Cancelling order: {}", orderId);
            
            boolean cancelled = tradingService.cancelOrder(orderId);
            
            if (cancelled) {
                logger.info("Order cancelled successfully: {}", orderId);
                return ResponseEntity.ok().build();
            } else {
                logger.warn("Failed to cancel order: {}", orderId);
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            
        } catch (TradingSystemException e) {
            logger.error("Error cancelling order: {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get trade history
     * @param limit Optional limit for number of trades to return
     * @param symbol Optional symbol filter
     * @return List of trade history
     */
    @GetMapping("/history")
    public ResponseEntity<List<TradeHistoryResponse>> getTradeHistory(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String symbol) {
        try {
            logger.debug("Retrieving trade history - limit: {}, symbol: {}", limit, symbol);
            
            List<Trade> trades;
            
            if (symbol != null && !symbol.trim().isEmpty()) {
                trades = tradingService.getTradesBySymbol(symbol.toUpperCase());
            } else if (limit != null && limit > 0) {
                trades = tradingService.getRecentTrades(limit);
            } else {
                trades = tradingService.getTradeHistory();
            }
            
            List<TradeHistoryResponse> response = trades.stream()
                .map(this::convertToTradeHistoryResponse)
                .collect(Collectors.toList());
            
            logger.debug("Retrieved {} trades", response.size());
            return ResponseEntity.ok(response);
            
        } catch (TradingSystemException e) {
            logger.error("Error retrieving trade history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get trade history for a specific symbol
     * @param symbol Stock symbol
     * @return List of trades for the symbol
     */
    @GetMapping("/history/{symbol}")
    public ResponseEntity<List<TradeHistoryResponse>> getTradeHistoryBySymbol(@PathVariable String symbol) {
        try {
            logger.debug("Retrieving trade history for symbol: {}", symbol);
            
            List<Trade> trades = tradingService.getTradesBySymbol(symbol.toUpperCase());
            List<TradeHistoryResponse> response = trades.stream()
                .map(this::convertToTradeHistoryResponse)
                .collect(Collectors.toList());
            
            logger.debug("Retrieved {} trades for symbol: {}", response.size(), symbol);
            return ResponseEntity.ok(response);
            
        } catch (TradingSystemException e) {
            logger.error("Error retrieving trade history for symbol: {}", symbol, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get recent trades
     * @param limit Number of recent trades to return (default: 10)
     * @return List of recent trades
     */
    @GetMapping("/recent")
    public ResponseEntity<List<TradeHistoryResponse>> getRecentTrades(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            logger.debug("Retrieving {} recent trades", limit);
            
            if (limit <= 0 || limit > 100) {
                logger.warn("Invalid limit value: {}. Using default value of 10", limit);
                limit = 10;
            }
            
            List<Trade> trades = tradingService.getRecentTrades(limit);
            List<TradeHistoryResponse> response = trades.stream()
                .map(this::convertToTradeHistoryResponse)
                .collect(Collectors.toList());
            
            logger.debug("Retrieved {} recent trades", response.size());
            return ResponseEntity.ok(response);
            
        } catch (TradingSystemException e) {
            logger.error("Error retrieving recent trades", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Validate an order without executing it
     * @param orderRequest Order details to validate
     * @return Validation result
     */
    @PostMapping("/validate")
    public ResponseEntity<String> validateOrder(@Valid @RequestBody OrderRequest orderRequest) {
        try {
            logger.debug("Validating order for symbol: {}, quantity: {}, type: {}", 
                        orderRequest.getSymbol(), orderRequest.getQuantity(), orderRequest.getType());
            
            boolean isValid = tradingService.validateOrder(
                orderRequest.getSymbol(),
                orderRequest.getQuantity(),
                orderRequest.getType(),
                orderRequest.getLimitPrice()
            );
            
            if (isValid) {
                logger.debug("Order validation passed");
                return ResponseEntity.ok("Order is valid");
            } else {
                logger.debug("Order validation failed");
                return ResponseEntity.badRequest().body("Order validation failed");
            }
            
        } catch (Exception e) {
            logger.error("Error during order validation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Validation error: " + e.getMessage());
        }
    }
    
    /**
     * Convert Trade entity to TradeHistoryResponse DTO
     * @param trade Trade entity
     * @return TradeHistoryResponse DTO
     */
    private TradeHistoryResponse convertToTradeHistoryResponse(Trade trade) {
        return new TradeHistoryResponse(
            trade.getOrderId(),
            trade.getSymbol(),
            trade.getType(),
            trade.getQuantity(),
            trade.getPrice(),
            trade.getStatus(),
            trade.getExecutedAt(),
            trade.getStrategyName(),
            trade.getAccountId()
        );
    }
}