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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradingServiceImplTest {
    
    @Mock
    private AlpacaApiClient alpacaApiClient;
    
    @Mock
    private PortfolioService portfolioService;
    
    @Mock
    private TradeRepository tradeRepository;
    
    @InjectMocks
    private TradingServiceImpl tradingService;
    
    private Portfolio testPortfolio;
    private Position testPosition;
    private OrderResponse testOrderResponse;
    private final String TEST_ACCOUNT_ID = "test-account";
    private final String TEST_SYMBOL = "AAPL";
    private final String TEST_ORDER_ID = "order-123";
    
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tradingService, "defaultAccountId", TEST_ACCOUNT_ID);
        ReflectionTestUtils.setField(tradingService, "maxPositionSize", new BigDecimal("10000"));
        ReflectionTestUtils.setField(tradingService, "riskPerTrade", new BigDecimal("0.02"));
        
        testPortfolio = new Portfolio(TEST_ACCOUNT_ID, new BigDecimal("10000.00"), new BigDecimal("15000.00"));
        
        testPosition = new Position(TEST_SYMBOL, new BigDecimal("100"), new BigDecimal("150.00"));
        testPosition.setCurrentPrice(new BigDecimal("155.00"));
        
        testOrderResponse = new OrderResponse();
        testOrderResponse.setOrderId(TEST_ORDER_ID);
        testOrderResponse.setSymbol(TEST_SYMBOL);
        testOrderResponse.setQuantity(new BigDecimal("10"));
        testOrderResponse.setStatus(OrderStatus.FILLED);
        testOrderResponse.setFilledPrice(new BigDecimal("155.00"));
    }
    
    @Test
    void executeBuyOrder_ShouldExecuteSuccessfully_WhenValidOrder() {
        // Given
        BigDecimal quantity = new BigDecimal("10");
        String orderType = "market";
        
        when(portfolioService.getCurrentPortfolio()).thenReturn(testPortfolio);
        when(alpacaApiClient.placeOrder(any(OrderRequest.class))).thenReturn(testOrderResponse);
        when(tradeRepository.save(any(Trade.class))).thenReturn(new Trade());
        
        // When
        OrderResponse result = tradingService.executeBuyOrder(TEST_SYMBOL, quantity, orderType);
        
        // Then
        assertNotNull(result);
        assertEquals(TEST_ORDER_ID, result.getOrderId());
        assertEquals(TEST_SYMBOL, result.getSymbol());
        assertEquals(OrderStatus.FILLED, result.getStatus());
        
        verify(alpacaApiClient).placeOrder(any(OrderRequest.class));
        verify(portfolioService).updatePosition(TEST_SYMBOL, quantity, testOrderResponse.getFilledPrice());
        verify(tradeRepository).save(any(Trade.class));
    }
    
    @Test
    void executeBuyOrder_ShouldThrowException_WhenInsufficientFunds() {
        // Given
        BigDecimal quantity = new BigDecimal("1000"); // Large quantity
        String orderType = "market";
        
        Portfolio poorPortfolio = new Portfolio(TEST_ACCOUNT_ID, new BigDecimal("100.00"), new BigDecimal("100.00"));
        when(portfolioService.getCurrentPortfolio()).thenReturn(poorPortfolio);
        
        // When & Then
        assertThrows(InsufficientFundsException.class, 
                    () -> tradingService.executeBuyOrder(TEST_SYMBOL, quantity, orderType));
        
        verify(alpacaApiClient, never()).placeOrder(any(OrderRequest.class));
    }
    
    @Test
    void executeBuyOrder_ShouldThrowException_WhenInvalidOrder() {
        // Given
        BigDecimal invalidQuantity = new BigDecimal("-10");
        String orderType = "market";
        
        // When & Then
        assertThrows(InvalidOrderException.class, 
                    () -> tradingService.executeBuyOrder(TEST_SYMBOL, invalidQuantity, orderType));
        
        verify(alpacaApiClient, never()).placeOrder(any(OrderRequest.class));
    }
    
    @Test
    void executeBuyOrder_WithLimitPrice_ShouldExecuteSuccessfully() {
        // Given
        BigDecimal quantity = new BigDecimal("10");
        String orderType = "limit";
        BigDecimal limitPrice = new BigDecimal("150.00");
        
        when(portfolioService.getCurrentPortfolio()).thenReturn(testPortfolio);
        when(alpacaApiClient.placeOrder(any(OrderRequest.class))).thenReturn(testOrderResponse);
        when(tradeRepository.save(any(Trade.class))).thenReturn(new Trade());
        
        // When
        OrderResponse result = tradingService.executeBuyOrder(TEST_SYMBOL, quantity, orderType, limitPrice);
        
        // Then
        assertNotNull(result);
        assertEquals(TEST_ORDER_ID, result.getOrderId());
        
        verify(alpacaApiClient).placeOrder(argThat(orderRequest -> 
            orderRequest.getLimitPrice().equals(limitPrice) && 
            orderRequest.getOrderType().equals(orderType)
        ));
    }
    
    @Test
    void executeSellOrder_ShouldExecuteSuccessfully_WhenValidOrder() {
        // Given
        BigDecimal quantity = new BigDecimal("10");
        String orderType = "market";
        
        when(portfolioService.getPosition(TEST_SYMBOL)).thenReturn(Optional.of(testPosition));
        when(alpacaApiClient.placeOrder(any(OrderRequest.class))).thenReturn(testOrderResponse);
        when(tradeRepository.save(any(Trade.class))).thenReturn(new Trade());
        
        // When
        OrderResponse result = tradingService.executeSellOrder(TEST_SYMBOL, quantity, orderType);
        
        // Then
        assertNotNull(result);
        assertEquals(TEST_ORDER_ID, result.getOrderId());
        assertEquals(TEST_SYMBOL, result.getSymbol());
        
        verify(alpacaApiClient).placeOrder(any(OrderRequest.class));
        verify(portfolioService).updatePosition(TEST_SYMBOL, quantity.negate(), testOrderResponse.getFilledPrice());
        verify(tradeRepository).save(any(Trade.class));
    }
    
    @Test
    void executeSellOrder_ShouldThrowException_WhenInsufficientShares() {
        // Given
        BigDecimal quantity = new BigDecimal("200"); // More than available
        String orderType = "market";
        
        when(portfolioService.getPosition(TEST_SYMBOL)).thenReturn(Optional.of(testPosition));
        
        // When & Then
        assertThrows(InvalidOrderException.class, 
                    () -> tradingService.executeSellOrder(TEST_SYMBOL, quantity, orderType));
        
        verify(alpacaApiClient, never()).placeOrder(any(OrderRequest.class));
    }
    
    @Test
    void executeSellOrder_ShouldThrowException_WhenNoPosition() {
        // Given
        BigDecimal quantity = new BigDecimal("10");
        String orderType = "market";
        
        when(portfolioService.getPosition(TEST_SYMBOL)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(InvalidOrderException.class, 
                    () -> tradingService.executeSellOrder(TEST_SYMBOL, quantity, orderType));
        
        verify(alpacaApiClient, never()).placeOrder(any(OrderRequest.class));
    }
    
    @Test
    void getOrderStatus_ShouldReturnStatus_WhenOrderExists() {
        // Given
        OrderStatus expectedStatus = OrderStatus.FILLED;
        when(alpacaApiClient.getOrderStatus(TEST_ORDER_ID)).thenReturn(expectedStatus);
        
        // When
        OrderStatus result = tradingService.getOrderStatus(TEST_ORDER_ID);
        
        // Then
        assertEquals(expectedStatus, result);
        verify(alpacaApiClient).getOrderStatus(TEST_ORDER_ID);
    }
    
    @Test
    void getOrderStatus_ShouldThrowException_WhenApiCallFails() {
        // Given
        when(alpacaApiClient.getOrderStatus(TEST_ORDER_ID)).thenThrow(new RuntimeException("API Error"));
        
        // When & Then
        assertThrows(TradingSystemException.class, () -> tradingService.getOrderStatus(TEST_ORDER_ID));
        verify(alpacaApiClient).getOrderStatus(TEST_ORDER_ID);
    }
    
    @Test
    void getTradeHistory_ShouldReturnAllTrades() {
        // Given
        List<Trade> expectedTrades = Arrays.asList(
            new Trade(TEST_ORDER_ID, TEST_SYMBOL, TradeType.BUY, new BigDecimal("10"), new BigDecimal("150.00"), OrderStatus.FILLED),
            new Trade("order-456", "GOOGL", TradeType.SELL, new BigDecimal("5"), new BigDecimal("2000.00"), OrderStatus.FILLED)
        );
        when(tradeRepository.findAllByOrderByExecutedAtDesc()).thenReturn(expectedTrades);
        
        // When
        List<Trade> result = tradingService.getTradeHistory();
        
        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(TEST_SYMBOL, result.get(0).getSymbol());
        verify(tradeRepository).findAllByOrderByExecutedAtDesc();
    }
    
    @Test
    void getTradeHistory_ByAccount_ShouldReturnAccountTrades() {
        // Given
        List<Trade> expectedTrades = Arrays.asList(
            new Trade(TEST_ORDER_ID, TEST_SYMBOL, TradeType.BUY, new BigDecimal("10"), new BigDecimal("150.00"), OrderStatus.FILLED)
        );
        when(tradeRepository.findByAccountIdOrderByExecutedAtDesc(TEST_ACCOUNT_ID)).thenReturn(expectedTrades);
        
        // When
        List<Trade> result = tradingService.getTradeHistory(TEST_ACCOUNT_ID);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TEST_SYMBOL, result.get(0).getSymbol());
        verify(tradeRepository).findByAccountIdOrderByExecutedAtDesc(TEST_ACCOUNT_ID);
    }
    
    @Test
    void getRecentTrades_ShouldReturnLimitedTrades() {
        // Given
        int limit = 5;
        List<Trade> expectedTrades = Arrays.asList(
            new Trade(TEST_ORDER_ID, TEST_SYMBOL, TradeType.BUY, new BigDecimal("10"), new BigDecimal("150.00"), OrderStatus.FILLED)
        );
        when(tradeRepository.findTopNByOrderByExecutedAtDesc(limit)).thenReturn(expectedTrades);
        
        // When
        List<Trade> result = tradingService.getRecentTrades(limit);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(tradeRepository).findTopNByOrderByExecutedAtDesc(limit);
    }
    
    @Test
    void getTradesBySymbol_ShouldReturnSymbolTrades() {
        // Given
        List<Trade> expectedTrades = Arrays.asList(
            new Trade(TEST_ORDER_ID, TEST_SYMBOL, TradeType.BUY, new BigDecimal("10"), new BigDecimal("150.00"), OrderStatus.FILLED)
        );
        when(tradeRepository.findBySymbolOrderByExecutedAtDesc(TEST_SYMBOL)).thenReturn(expectedTrades);
        
        // When
        List<Trade> result = tradingService.getTradesBySymbol(TEST_SYMBOL);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TEST_SYMBOL, result.get(0).getSymbol());
        verify(tradeRepository).findBySymbolOrderByExecutedAtDesc(TEST_SYMBOL);
    }
    
    @Test
    void cancelOrder_ShouldReturnTrue_WhenSuccessful() {
        // When
        boolean result = tradingService.cancelOrder(TEST_ORDER_ID);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void validateOrder_ShouldReturnTrue_WhenValidOrder() {
        // Given
        BigDecimal quantity = new BigDecimal("10");
        BigDecimal price = new BigDecimal("150.00");
        
        // When
        boolean result = tradingService.validateOrder(TEST_SYMBOL, quantity, TradeType.BUY, price);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void validateOrder_ShouldReturnFalse_WhenInvalidSymbol() {
        // Given
        String invalidSymbol = "";
        BigDecimal quantity = new BigDecimal("10");
        
        // When
        boolean result = tradingService.validateOrder(invalidSymbol, quantity, TradeType.BUY, null);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void validateOrder_ShouldReturnFalse_WhenInvalidQuantity() {
        // Given
        BigDecimal invalidQuantity = new BigDecimal("-10");
        
        // When
        boolean result = tradingService.validateOrder(TEST_SYMBOL, invalidQuantity, TradeType.BUY, null);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void validateOrder_ShouldReturnFalse_WhenQuantityExceedsLimit() {
        // Given
        BigDecimal largeQuantity = new BigDecimal("20000");
        
        // When
        boolean result = tradingService.validateOrder(TEST_SYMBOL, largeQuantity, TradeType.BUY, null);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void hasSufficientFunds_ShouldReturnTrue_WhenFundsAvailable() {
        // Given
        BigDecimal quantity = new BigDecimal("10");
        BigDecimal price = new BigDecimal("100.00");
        
        when(portfolioService.getCurrentPortfolio()).thenReturn(testPortfolio);
        
        // When
        boolean result = tradingService.hasSufficientFunds(TEST_SYMBOL, quantity, price);
        
        // Then
        assertTrue(result);
        verify(portfolioService).getCurrentPortfolio();
    }
    
    @Test
    void hasSufficientFunds_ShouldReturnFalse_WhenInsufficientFunds() {
        // Given
        BigDecimal quantity = new BigDecimal("1000");
        BigDecimal price = new BigDecimal("100.00");
        
        Portfolio poorPortfolio = new Portfolio(TEST_ACCOUNT_ID, new BigDecimal("100.00"), new BigDecimal("100.00"));
        when(portfolioService.getCurrentPortfolio()).thenReturn(poorPortfolio);
        
        // When
        boolean result = tradingService.hasSufficientFunds(TEST_SYMBOL, quantity, price);
        
        // Then
        assertFalse(result);
        verify(portfolioService).getCurrentPortfolio();
    }
    
    @Test
    void hasSufficientShares_ShouldReturnTrue_WhenSharesAvailable() {
        // Given
        BigDecimal quantity = new BigDecimal("50");
        
        when(portfolioService.getPosition(TEST_SYMBOL)).thenReturn(Optional.of(testPosition));
        
        // When
        boolean result = tradingService.hasSufficientShares(TEST_SYMBOL, quantity);
        
        // Then
        assertTrue(result);
        verify(portfolioService).getPosition(TEST_SYMBOL);
    }
    
    @Test
    void hasSufficientShares_ShouldReturnFalse_WhenInsufficientShares() {
        // Given
        BigDecimal quantity = new BigDecimal("200");
        
        when(portfolioService.getPosition(TEST_SYMBOL)).thenReturn(Optional.of(testPosition));
        
        // When
        boolean result = tradingService.hasSufficientShares(TEST_SYMBOL, quantity);
        
        // Then
        assertFalse(result);
        verify(portfolioService).getPosition(TEST_SYMBOL);
    }
    
    @Test
    void hasSufficientShares_ShouldReturnFalse_WhenNoPosition() {
        // Given
        BigDecimal quantity = new BigDecimal("10");
        
        when(portfolioService.getPosition(TEST_SYMBOL)).thenReturn(Optional.empty());
        
        // When
        boolean result = tradingService.hasSufficientShares(TEST_SYMBOL, quantity);
        
        // Then
        assertFalse(result);
        verify(portfolioService).getPosition(TEST_SYMBOL);
    }
}