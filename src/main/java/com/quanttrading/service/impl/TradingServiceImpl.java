package com.quanttrading.service.impl;

import com.quanttrading.client.AlpacaApiClient;
import com.quanttrading.dto.OrderRequest;
import com.quanttrading.dto.OrderResponse;
import com.quanttrading.exception.InsufficientFundsException;
import com.quanttrading.exception.InvalidOrderException;
import com.quanttrading.exception.TradingSystemException;
import com.quanttrading.model.OrderStatus;
import com.quanttrading.model.Portfolio;
import com.quanttrading.model.Position;
import com.quanttrading.model.Trade;
import com.quanttrading.model.TradeType;
import com.quanttrading.repository.TradeRepository;
import com.quanttrading.service.PortfolioService;
import com.quanttrading.service.TradingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of TradingService for trading operations
 */
@Service
@Transactional
public class TradingServiceImpl implements TradingService {
    
    private static final Logger logger = LoggerFactory.getLogger(TradingServiceImpl.class);
    
    @Value("${trading.default-account-id:default}")
    private String defaultAccountId;
    
    @Value("${trading.max-position-size:10000}")
    private BigDecimal maxPositionSize;
    
    @Value("${trading.risk-per-trade:0.02}")
    private BigDecimal riskPerTrade;
    
    private final AlpacaApiClient alpacaApiClient;
    private final PortfolioService portfolioService;
    private final TradeRepository tradeRepository;
    
    @Autowired
    public TradingServiceImpl(AlpacaApiClient alpacaApiClient,
                             PortfolioService portfolioService,
                             TradeRepository tradeRepository) {
        this.alpacaApiClient = alpacaApiClient;
        this.portfolioService = portfolioService;
        this.tradeRepository = tradeRepository;
    }
    
    @Override
    public OrderResponse executeBuyOrder(String symbol, BigDecimal quantity, String orderType) {
        return executeBuyOrder(symbol, quantity, orderType, null);
    }
    
    @Override
    public OrderResponse executeBuyOrder(String symbol, BigDecimal quantity, String orderType, BigDecimal limitPrice) {
        logger.info("Executing buy order - Symbol: {}, Quantity: {}, Type: {}, Limit: {}", 
                   symbol, quantity, orderType, limitPrice);
        
        // Validate order
        if (!validateOrder(symbol, quantity, TradeType.BUY, limitPrice)) {
            throw new InvalidOrderException("Order validation failed for buy order");
        }
        
        // Check sufficient funds
        BigDecimal estimatedPrice = limitPrice != null ? limitPrice : getCurrentPrice(symbol);
        if (!hasSufficientFunds(symbol, quantity, estimatedPrice)) {
            throw new InsufficientFundsException("Insufficient funds for buy order");
        }
        
        try {
            // Create order request
            OrderRequest orderRequest = new OrderRequest();
            orderRequest.setSymbol(symbol);
            orderRequest.setQuantity(quantity);
            orderRequest.setType(TradeType.BUY);
            orderRequest.setOrderType(orderType);
            orderRequest.setTimeInForce("day");
            if (limitPrice != null) {
                orderRequest.setLimitPrice(limitPrice);
            }
            
            // Execute order through Alpaca
            OrderResponse response = alpacaApiClient.placeOrder(orderRequest);
            
            // Record trade
            recordTrade(response, TradeType.BUY, estimatedPrice);
            
            // Update portfolio if order is filled
            if (OrderStatus.FILLED.equals(response.getStatus())) {
                portfolioService.updatePosition(symbol, quantity, response.getFilledPrice());
            }
            
            logger.info("Buy order executed successfully - Order ID: {}, Status: {}", 
                       response.getOrderId(), response.getStatus());
            
            return response;
            
        } catch (Exception e) {
            logger.error("Failed to execute buy order for symbol: {}", symbol, e);
            throw new TradingSystemException("Buy order execution failed", e);
        }
    }
    
    @Override
    public OrderResponse executeSellOrder(String symbol, BigDecimal quantity, String orderType) {
        return executeSellOrder(symbol, quantity, orderType, null);
    }
    
    @Override
    public OrderResponse executeSellOrder(String symbol, BigDecimal quantity, String orderType, BigDecimal limitPrice) {
        logger.info("Executing sell order - Symbol: {}, Quantity: {}, Type: {}, Limit: {}", 
                   symbol, quantity, orderType, limitPrice);
        
        // Validate order
        if (!validateOrder(symbol, quantity, TradeType.SELL, limitPrice)) {
            throw new InvalidOrderException("Order validation failed for sell order");
        }
        
        // Check sufficient shares
        if (!hasSufficientShares(symbol, quantity)) {
            throw new InvalidOrderException("Insufficient shares for sell order");
        }
        
        try {
            // Create order request
            OrderRequest orderRequest = new OrderRequest();
            orderRequest.setSymbol(symbol);
            orderRequest.setQuantity(quantity);
            orderRequest.setType(TradeType.SELL);
            orderRequest.setOrderType(orderType);
            orderRequest.setTimeInForce("day");
            if (limitPrice != null) {
                orderRequest.setLimitPrice(limitPrice);
            }
            
            // Execute order through Alpaca
            OrderResponse response = alpacaApiClient.placeOrder(orderRequest);
            
            // Record trade
            BigDecimal estimatedPrice = limitPrice != null ? limitPrice : getCurrentPrice(symbol);
            recordTrade(response, TradeType.SELL, estimatedPrice);
            
            // Update portfolio if order is filled
            if (OrderStatus.FILLED.equals(response.getStatus())) {
                portfolioService.updatePosition(symbol, quantity.negate(), response.getFilledPrice());
            }
            
            logger.info("Sell order executed successfully - Order ID: {}, Status: {}", 
                       response.getOrderId(), response.getStatus());
            
            return response;
            
        } catch (Exception e) {
            logger.error("Failed to execute sell order for symbol: {}", symbol, e);
            throw new TradingSystemException("Sell order execution failed", e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public OrderStatus getOrderStatus(String orderId) {
        logger.debug("Getting order status for order ID: {}", orderId);
        
        try {
            return alpacaApiClient.getOrderStatus(orderId);
        } catch (Exception e) {
            logger.error("Failed to get order status for order ID: {}", orderId, e);
            throw new TradingSystemException("Failed to retrieve order status", e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Trade> getTradeHistory() {
        logger.debug("Retrieving all trade history");
        return tradeRepository.findAllByOrderByExecutedAtDesc();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Trade> getTradeHistory(String accountId) {
        logger.debug("Retrieving trade history for account: {}", accountId);
        return tradeRepository.findByAccountIdOrderByExecutedAtDesc(accountId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Trade> getRecentTrades(int limit) {
        logger.debug("Retrieving {} recent trades", limit);
        return tradeRepository.findTopNByOrderByExecutedAtDesc(limit);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Trade> getTradesBySymbol(String symbol) {
        logger.debug("Retrieving trades for symbol: {}", symbol);
        return tradeRepository.findBySymbolOrderByExecutedAtDesc(symbol);
    }
    
    @Override
    public boolean cancelOrder(String orderId) {
        logger.info("Cancelling order: {}", orderId);
        
        try {
            // This would typically call the Alpaca API to cancel the order
            // For now, we'll just log the action
            logger.info("Order cancellation requested for order ID: {}", orderId);
            return true;
        } catch (Exception e) {
            logger.error("Failed to cancel order: {}", orderId, e);
            return false;
        }
    }
    
    @Override
    public boolean validateOrder(String symbol, BigDecimal quantity, TradeType tradeType, BigDecimal price) {
        logger.debug("Validating order - Symbol: {}, Quantity: {}, Type: {}, Price: {}", 
                    symbol, quantity, tradeType, price);
        
        // Basic validation
        if (symbol == null || symbol.trim().isEmpty()) {
            logger.warn("Order validation failed: Symbol is null or empty");
            return false;
        }
        
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            logger.warn("Order validation failed: Invalid quantity: {}", quantity);
            return false;
        }
        
        if (tradeType == null) {
            logger.warn("Order validation failed: Trade type is null");
            return false;
        }
        
        // Check position size limits
        if (quantity.compareTo(maxPositionSize) > 0) {
            logger.warn("Order validation failed: Quantity {} exceeds max position size {}", 
                       quantity, maxPositionSize);
            return false;
        }
        
        // Validate limit price if provided
        if (price != null && price.compareTo(BigDecimal.ZERO) <= 0) {
            logger.warn("Order validation failed: Invalid price: {}", price);
            return false;
        }
        
        logger.debug("Order validation passed");
        return true;
    }
    
    @Override
    public boolean hasSufficientFunds(String symbol, BigDecimal quantity, BigDecimal price) {
        logger.debug("Checking sufficient funds for {} shares of {} at ${}", quantity, symbol, price);
        
        try {
            Portfolio portfolio = portfolioService.getCurrentPortfolio();
            BigDecimal requiredAmount = quantity.multiply(price);
            BigDecimal availableCash = portfolio.getCashBalance();
            
            boolean sufficient = availableCash.compareTo(requiredAmount) >= 0;
            
            if (!sufficient) {
                logger.warn("Insufficient funds - Required: {}, Available: {}", requiredAmount, availableCash);
            }
            
            return sufficient;
            
        } catch (Exception e) {
            logger.error("Error checking sufficient funds", e);
            return false;
        }
    }
    
    @Override
    public boolean hasSufficientShares(String symbol, BigDecimal quantity) {
        logger.debug("Checking sufficient shares for {} shares of {}", quantity, symbol);
        
        try {
            Optional<Position> position = portfolioService.getPosition(symbol);
            
            if (position.isEmpty()) {
                logger.warn("No position found for symbol: {}", symbol);
                return false;
            }
            
            BigDecimal availableShares = position.get().getQuantity();
            boolean sufficient = availableShares.compareTo(quantity) >= 0;
            
            if (!sufficient) {
                logger.warn("Insufficient shares - Required: {}, Available: {}", quantity, availableShares);
            }
            
            return sufficient;
            
        } catch (Exception e) {
            logger.error("Error checking sufficient shares", e);
            return false;
        }
    }
    
    // Private helper methods
    
    private void recordTrade(OrderResponse orderResponse, TradeType tradeType, BigDecimal estimatedPrice) {
        try {
            Trade trade = new Trade();
            trade.setOrderId(orderResponse.getOrderId());
            trade.setSymbol(orderResponse.getSymbol());
            trade.setType(tradeType);
            trade.setQuantity(orderResponse.getQuantity());
            trade.setPrice(orderResponse.getFilledPrice() != null ? orderResponse.getFilledPrice() : estimatedPrice);
            trade.setStatus(orderResponse.getStatus());
            trade.setExecutedAt(LocalDateTime.now());
            trade.setAccountId(defaultAccountId);
            
            tradeRepository.save(trade);
            logger.debug("Trade recorded - Order ID: {}, Symbol: {}, Type: {}", 
                        orderResponse.getOrderId(), orderResponse.getSymbol(), tradeType);
            
        } catch (Exception e) {
            logger.error("Failed to record trade for order: {}", orderResponse.getOrderId(), e);
            // Don't throw exception here as the trade execution was successful
        }
    }
    
    private BigDecimal getCurrentPrice(String symbol) {
        // This would typically call the market data service to get current price
        // For now, we'll return a default value
        logger.debug("Getting current price for symbol: {} (using default value)", symbol);
        return new BigDecimal("100.00"); // Default price for testing
    }
}