package com.quanttrading.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quanttrading.dto.OrderRequest;
import com.quanttrading.dto.OrderResponse;
import com.quanttrading.exception.InsufficientFundsException;
import com.quanttrading.exception.InvalidOrderException;
import com.quanttrading.exception.TradingSystemException;
import com.quanttrading.model.OrderStatus;
import com.quanttrading.model.Trade;
import com.quanttrading.model.TradeType;
import com.quanttrading.service.TradingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TradingController.class)
class TradingControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private TradingService tradingService;
    
    private OrderRequest buyOrderRequest;
    private OrderRequest sellOrderRequest;
    private OrderResponse orderResponse;
    private Trade testTrade1;
    private Trade testTrade2;
    
    @BeforeEach
    void setUp() {
        // Create test buy order request
        buyOrderRequest = new OrderRequest();
        buyOrderRequest.setSymbol("AAPL");
        buyOrderRequest.setQuantity(new BigDecimal("10"));
        buyOrderRequest.setType(TradeType.BUY);
        buyOrderRequest.setOrderType("market");
        
        // Create test sell order request
        sellOrderRequest = new OrderRequest();
        sellOrderRequest.setSymbol("GOOGL");
        sellOrderRequest.setQuantity(new BigDecimal("5"));
        sellOrderRequest.setType(TradeType.SELL);
        sellOrderRequest.setOrderType("limit");
        sellOrderRequest.setLimitPrice(new BigDecimal("2100.00"));
        
        // Create test order response
        orderResponse = new OrderResponse();
        orderResponse.setOrderId("order-123");
        orderResponse.setSymbol("AAPL");
        orderResponse.setQuantity(new BigDecimal("10"));
        orderResponse.setType(TradeType.BUY);
        orderResponse.setStatus(OrderStatus.FILLED);
        orderResponse.setFilledPrice(new BigDecimal("150.00"));
        orderResponse.setSubmittedAt(LocalDateTime.now());
        
        // Create test trades
        testTrade1 = new Trade("order-123", "AAPL", TradeType.BUY, new BigDecimal("10"), new BigDecimal("150.00"), OrderStatus.FILLED);
        testTrade1.setExecutedAt(LocalDateTime.now());
        testTrade1.setAccountId("test-account");
        
        testTrade2 = new Trade("order-456", "GOOGL", TradeType.SELL, new BigDecimal("5"), new BigDecimal("2100.00"), OrderStatus.FILLED);
        testTrade2.setExecutedAt(LocalDateTime.now());
        testTrade2.setAccountId("test-account");
    }
    
    @Test
    void executeBuyOrder_MarketOrder_Success() throws Exception {
        // Given
        when(tradingService.executeBuyOrder(anyString(), any(BigDecimal.class), anyString()))
            .thenReturn(orderResponse);
        
        // When & Then
        mockMvc.perform(post("/api/trades/buy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buyOrderRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("order-123"))
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.quantity").value(10))
                .andExpect(jsonPath("$.type").value("BUY"))
                .andExpect(jsonPath("$.status").value("FILLED"))
                .andExpect(jsonPath("$.filledPrice").value(150.00));
        
        verify(tradingService).executeBuyOrder("AAPL", new BigDecimal("10"), "market");
    }
    
    @Test
    void executeBuyOrder_LimitOrder_Success() throws Exception {
        // Given
        buyOrderRequest.setOrderType("limit");
        buyOrderRequest.setLimitPrice(new BigDecimal("149.00"));
        
        when(tradingService.executeBuyOrder(anyString(), any(BigDecimal.class), anyString(), any(BigDecimal.class)))
            .thenReturn(orderResponse);
        
        // When & Then
        mockMvc.perform(post("/api/trades/buy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buyOrderRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("order-123"));
        
        verify(tradingService).executeBuyOrder("AAPL", new BigDecimal("10"), "limit", new BigDecimal("149.00"));
    }
    
    @Test
    void executeBuyOrder_InvalidOrder() throws Exception {
        // Given
        when(tradingService.executeBuyOrder(anyString(), any(BigDecimal.class), anyString()))
            .thenThrow(new InvalidOrderException("Invalid order"));
        
        // When & Then
        mockMvc.perform(post("/api/trades/buy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buyOrderRequest)))
                .andExpect(status().isBadRequest());
        
        verify(tradingService).executeBuyOrder("AAPL", new BigDecimal("10"), "market");
    }
    
    @Test
    void executeBuyOrder_InsufficientFunds() throws Exception {
        // Given
        when(tradingService.executeBuyOrder(anyString(), any(BigDecimal.class), anyString()))
            .thenThrow(new InsufficientFundsException("Insufficient funds"));
        
        // When & Then
        mockMvc.perform(post("/api/trades/buy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buyOrderRequest)))
                .andExpect(status().isPaymentRequired());
        
        verify(tradingService).executeBuyOrder("AAPL", new BigDecimal("10"), "market");
    }
    
    @Test
    void executeSellOrder_LimitOrder_Success() throws Exception {
        // Given
        OrderResponse sellResponse = new OrderResponse();
        sellResponse.setOrderId("order-456");
        sellResponse.setSymbol("GOOGL");
        sellResponse.setQuantity(new BigDecimal("5"));
        sellResponse.setType(TradeType.SELL);
        sellResponse.setStatus(OrderStatus.PENDING);
        
        when(tradingService.executeSellOrder(anyString(), any(BigDecimal.class), anyString(), any(BigDecimal.class)))
            .thenReturn(sellResponse);
        
        // When & Then
        mockMvc.perform(post("/api/trades/sell")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sellOrderRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("order-456"))
                .andExpect(jsonPath("$.symbol").value("GOOGL"))
                .andExpect(jsonPath("$.quantity").value(5))
                .andExpect(jsonPath("$.type").value("SELL"))
                .andExpect(jsonPath("$.status").value("PENDING"));
        
        verify(tradingService).executeSellOrder("GOOGL", new BigDecimal("5"), "limit", new BigDecimal("2100.00"));
    }
    
    @Test
    void executeSellOrder_MarketOrder_Success() throws Exception {
        // Given
        sellOrderRequest.setOrderType("market");
        sellOrderRequest.setLimitPrice(null);
        
        OrderResponse sellResponse = new OrderResponse();
        sellResponse.setOrderId("order-456");
        sellResponse.setSymbol("GOOGL");
        sellResponse.setType(TradeType.SELL);
        sellResponse.setStatus(OrderStatus.FILLED);
        
        when(tradingService.executeSellOrder(anyString(), any(BigDecimal.class), anyString()))
            .thenReturn(sellResponse);
        
        // When & Then
        mockMvc.perform(post("/api/trades/sell")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sellOrderRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("order-456"));
        
        verify(tradingService).executeSellOrder("GOOGL", new BigDecimal("5"), "market");
    }
    
    @Test
    void getOrderStatus_Success() throws Exception {
        // Given
        when(tradingService.getOrderStatus("order-123")).thenReturn(OrderStatus.FILLED);
        
        // When & Then
        mockMvc.perform(get("/api/trades/orders/order-123/status")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("order-123"))
                .andExpect(jsonPath("$.status").value("FILLED"));
        
        verify(tradingService).getOrderStatus("order-123");
    }
    
    @Test
    void getOrderStatus_ServiceException() throws Exception {
        // Given
        when(tradingService.getOrderStatus("order-123"))
            .thenThrow(new TradingSystemException("Order not found"));
        
        // When & Then
        mockMvc.perform(get("/api/trades/orders/order-123/status")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
        
        verify(tradingService).getOrderStatus("order-123");
    }
    
    @Test
    void cancelOrder_Success() throws Exception {
        // Given
        when(tradingService.cancelOrder("order-123")).thenReturn(true);
        
        // When & Then
        mockMvc.perform(delete("/api/trades/orders/order-123")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        
        verify(tradingService).cancelOrder("order-123");
    }
    
    @Test
    void cancelOrder_Failed() throws Exception {
        // Given
        when(tradingService.cancelOrder("order-123")).thenReturn(false);
        
        // When & Then
        mockMvc.perform(delete("/api/trades/orders/order-123")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
        
        verify(tradingService).cancelOrder("order-123");
    }
    
    @Test
    void getTradeHistory_All_Success() throws Exception {
        // Given
        List<Trade> trades = Arrays.asList(testTrade1, testTrade2);
        when(tradingService.getTradeHistory()).thenReturn(trades);
        
        // When & Then
        mockMvc.perform(get("/api/trades/history")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].orderId").value("order-123"))
                .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$[0].type").value("BUY"))
                .andExpect(jsonPath("$[1].orderId").value("order-456"))
                .andExpect(jsonPath("$[1].symbol").value("GOOGL"))
                .andExpect(jsonPath("$[1].type").value("SELL"));
        
        verify(tradingService).getTradeHistory();
    }
    
    @Test
    void getTradeHistory_WithLimit_Success() throws Exception {
        // Given
        List<Trade> trades = Arrays.asList(testTrade1);
        when(tradingService.getRecentTrades(1)).thenReturn(trades);
        
        // When & Then
        mockMvc.perform(get("/api/trades/history?limit=1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].orderId").value("order-123"));
        
        verify(tradingService).getRecentTrades(1);
    }
    
    @Test
    void getTradeHistory_WithSymbol_Success() throws Exception {
        // Given
        List<Trade> trades = Arrays.asList(testTrade1);
        when(tradingService.getTradesBySymbol("AAPL")).thenReturn(trades);
        
        // When & Then
        mockMvc.perform(get("/api/trades/history?symbol=aapl")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].symbol").value("AAPL"));
        
        verify(tradingService).getTradesBySymbol("AAPL");
    }
    
    @Test
    void getTradeHistoryBySymbol_Success() throws Exception {
        // Given
        List<Trade> trades = Arrays.asList(testTrade2);
        when(tradingService.getTradesBySymbol("GOOGL")).thenReturn(trades);
        
        // When & Then
        mockMvc.perform(get("/api/trades/history/googl")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].symbol").value("GOOGL"));
        
        verify(tradingService).getTradesBySymbol("GOOGL");
    }
    
    @Test
    void getRecentTrades_Success() throws Exception {
        // Given
        List<Trade> trades = Arrays.asList(testTrade1, testTrade2);
        when(tradingService.getRecentTrades(10)).thenReturn(trades);
        
        // When & Then
        mockMvc.perform(get("/api/trades/recent")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        
        verify(tradingService).getRecentTrades(10);
    }
    
    @Test
    void getRecentTrades_WithCustomLimit_Success() throws Exception {
        // Given
        List<Trade> trades = Arrays.asList(testTrade1);
        when(tradingService.getRecentTrades(5)).thenReturn(trades);
        
        // When & Then
        mockMvc.perform(get("/api/trades/recent?limit=5")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        
        verify(tradingService).getRecentTrades(5);
    }
    
    @Test
    void validateOrder_Valid_Success() throws Exception {
        // Given
        when(tradingService.validateOrder(anyString(), any(BigDecimal.class), any(TradeType.class), any()))
            .thenReturn(true);
        
        // When & Then
        mockMvc.perform(post("/api/trades/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buyOrderRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("Order is valid"));
        
        verify(tradingService).validateOrder("AAPL", new BigDecimal("10"), TradeType.BUY, null);
    }
    
    @Test
    void validateOrder_Invalid_BadRequest() throws Exception {
        // Given
        when(tradingService.validateOrder(anyString(), any(BigDecimal.class), any(TradeType.class), any()))
            .thenReturn(false);
        
        // When & Then
        mockMvc.perform(post("/api/trades/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buyOrderRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Order validation failed"));
        
        verify(tradingService).validateOrder("AAPL", new BigDecimal("10"), TradeType.BUY, null);
    }
    
    @Test
    void executeBuyOrder_ValidationError_BadRequest() throws Exception {
        // Given - Invalid order request (negative quantity)
        buyOrderRequest.setQuantity(new BigDecimal("-10"));
        
        // When & Then
        mockMvc.perform(post("/api/trades/buy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buyOrderRequest)))
                .andExpect(status().isBadRequest());
        
        // Verify service was not called due to validation failure
        verify(tradingService, never()).executeBuyOrder(anyString(), any(BigDecimal.class), anyString());
    }
    
    @Test
    void executeSellOrder_TradingSystemException() throws Exception {
        // Given
        when(tradingService.executeSellOrder(anyString(), any(BigDecimal.class), anyString(), any(BigDecimal.class)))
            .thenThrow(new TradingSystemException("System error"));
        
        // When & Then
        mockMvc.perform(post("/api/trades/sell")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sellOrderRequest)))
                .andExpect(status().isInternalServerError());
        
        verify(tradingService).executeSellOrder("GOOGL", new BigDecimal("5"), "limit", new BigDecimal("2100.00"));
    }
}