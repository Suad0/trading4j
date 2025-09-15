package com.quanttrading.config;

import com.quanttrading.model.OrderStatus;
import com.quanttrading.model.Trade;
import com.quanttrading.model.TradeType;
import com.quanttrading.repository.TradeRepository;
import com.quanttrading.service.TradingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GracefulShutdownHandlerTest {

    @Mock
    private TradingService tradingService;

    @Mock
    private TradeRepository tradeRepository;

    @Mock
    private ContextClosedEvent contextClosedEvent;

    private GracefulShutdownHandler shutdownHandler;

    @BeforeEach
    void setUp() {
        shutdownHandler = new GracefulShutdownHandler(tradingService, tradeRepository);
        
        // Set configuration properties
        ReflectionTestUtils.setField(shutdownHandler, "shutdownTimeoutSeconds", 5);
        ReflectionTestUtils.setField(shutdownHandler, "cancelPendingOrders", true);
        ReflectionTestUtils.setField(shutdownHandler, "waitForFills", true);
    }

    @Test
    void handleContextClosed_NoActiveTrades() {
        // Given
        when(tradeRepository.findRecentTrades(any(LocalDateTime.class))).thenReturn(Collections.emptyList());

        // When
        shutdownHandler.handleContextClosed();

        // Then
        assertTrue(shutdownHandler.isShutdownInProgress());
        verify(tradeRepository).findRecentTrades(any(LocalDateTime.class));
        verify(tradingService, never()).cancelOrder(anyString());
    }

    @Test
    void handleContextClosed_WithActiveTrades() {
        // Given
        Trade pendingTrade = createTrade("order1", OrderStatus.SUBMITTED);
        Trade filledTrade = createTrade("order2", OrderStatus.FILLED);
        
        when(tradeRepository.findRecentTrades(any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(pendingTrade, filledTrade));
        when(tradingService.getOrderStatus("order1")).thenReturn(OrderStatus.SUBMITTED);
        when(tradingService.getOrderStatus("order2")).thenReturn(OrderStatus.FILLED);
        when(tradingService.cancelOrder("order1")).thenReturn(true);

        // When
        shutdownHandler.handleContextClosed();

        // Then
        assertTrue(shutdownHandler.isShutdownInProgress());
        verify(tradingService).cancelOrder("order1");
        verify(tradingService, never()).cancelOrder("order2");
    }

    @Test
    void handleContextClosed_CancelOrdersDisabled() {
        // Given
        ReflectionTestUtils.setField(shutdownHandler, "cancelPendingOrders", false);
        
        Trade pendingTrade = createTrade("order1", OrderStatus.SUBMITTED);
        when(tradeRepository.findRecentTrades(any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(pendingTrade));
        when(tradingService.getOrderStatus("order1")).thenReturn(OrderStatus.SUBMITTED);

        // When
        shutdownHandler.handleContextClosed();

        // Then
        assertTrue(shutdownHandler.isShutdownInProgress());
        verify(tradingService, never()).cancelOrder(anyString());
    }

    @Test
    void handleContextClosed_WaitForFillsDisabled() {
        // Given
        ReflectionTestUtils.setField(shutdownHandler, "waitForFills", false);
        
        Trade pendingTrade = createTrade("order1", OrderStatus.SUBMITTED);
        when(tradeRepository.findRecentTrades(any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(pendingTrade));
        when(tradingService.getOrderStatus("order1")).thenReturn(OrderStatus.SUBMITTED);
        when(tradingService.cancelOrder("order1")).thenReturn(true);

        // When
        shutdownHandler.handleContextClosed();

        // Then
        assertTrue(shutdownHandler.isShutdownInProgress());
        verify(tradingService).cancelOrder("order1");
        // Should not wait for fills since it's disabled
    }

    @Test
    void handleContextClosed_CancelOrderFailure() {
        // Given
        Trade pendingTrade = createTrade("order1", OrderStatus.SUBMITTED);
        when(tradeRepository.findRecentTrades(any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(pendingTrade));
        when(tradingService.getOrderStatus("order1")).thenReturn(OrderStatus.SUBMITTED);
        when(tradingService.cancelOrder("order1")).thenReturn(false);

        // When
        shutdownHandler.handleContextClosed();

        // Then
        assertTrue(shutdownHandler.isShutdownInProgress());
        verify(tradingService).cancelOrder("order1");
    }

    @Test
    void handleContextClosed_OrderStatusCheckFailure() {
        // Given
        Trade trade = createTrade("order1", OrderStatus.SUBMITTED);
        when(tradeRepository.findRecentTrades(any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(trade));
        when(tradingService.getOrderStatus("order1")).thenThrow(new RuntimeException("API error"));

        // When
        shutdownHandler.handleContextClosed();

        // Then
        assertTrue(shutdownHandler.isShutdownInProgress());
        verify(tradingService, never()).cancelOrder(anyString());
    }

    @Test
    void preDestroy_CallsGracefulShutdown() {
        // Given
        when(tradeRepository.findRecentTrades(any(LocalDateTime.class))).thenReturn(Collections.emptyList());

        // When
        shutdownHandler.preDestroy();

        // Then
        assertTrue(shutdownHandler.isShutdownInProgress());
    }

    @Test
    void preDestroy_SkipsIfShutdownAlreadyInProgress() {
        // Given
        ReflectionTestUtils.setField(shutdownHandler, "shutdownInProgress", true);

        // When
        shutdownHandler.preDestroy();

        // Then
        verify(tradeRepository, never()).findRecentTrades(any(LocalDateTime.class));
    }

    @Test
    void isShutdownInProgress_InitiallyFalse() {
        // When & Then
        assertFalse(shutdownHandler.isShutdownInProgress());
    }

    @Test
    void isShutdownInProgress_TrueAfterShutdown() {
        // Given
        when(tradeRepository.findRecentTrades(any(LocalDateTime.class))).thenReturn(Collections.emptyList());

        // When
        shutdownHandler.handleContextClosed();

        // Then
        assertTrue(shutdownHandler.isShutdownInProgress());
    }

    @Test
    void handleContextClosed_WithPartiallyFilledOrder() {
        // Given
        Trade partiallyFilledTrade = createTrade("order1", OrderStatus.PARTIALLY_FILLED);
        when(tradeRepository.findRecentTrades(any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(partiallyFilledTrade));
        when(tradingService.getOrderStatus("order1")).thenReturn(OrderStatus.PARTIALLY_FILLED);
        when(tradingService.cancelOrder("order1")).thenReturn(true);

        // When
        shutdownHandler.handleContextClosed();

        // Then
        assertTrue(shutdownHandler.isShutdownInProgress());
        verify(tradingService).cancelOrder("order1");
    }

    private Trade createTrade(String orderId, OrderStatus status) {
        Trade trade = new Trade();
        trade.setOrderId(orderId);
        trade.setSymbol("AAPL");
        trade.setType(TradeType.BUY);
        trade.setQuantity(BigDecimal.valueOf(10));
        trade.setPrice(BigDecimal.valueOf(150.00));
        trade.setStatus(status);
        trade.setExecutedAt(LocalDateTime.now());
        return trade;
    }
}