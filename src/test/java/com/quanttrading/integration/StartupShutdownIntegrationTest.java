package com.quanttrading.integration;

import com.quanttrading.config.GracefulShutdownHandler;
import com.quanttrading.config.StartupReadinessIndicator;
import com.quanttrading.model.OrderStatus;
import com.quanttrading.model.Trade;
import com.quanttrading.model.TradeType;
import com.quanttrading.repository.TradeRepository;
import com.quanttrading.service.TradingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "trading.startup.portfolio-sync-enabled=false",
        "trading.shutdown.timeout-seconds=2",
        "trading.shutdown.cancel-pending-orders=true",
        "trading.shutdown.wait-for-fills=false"
})
class StartupShutdownIntegrationTest {

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Autowired
    private StartupReadinessIndicator readinessIndicator;

    @Autowired
    private GracefulShutdownHandler shutdownHandler;

    @MockBean
    private TradingService tradingService;

    @Autowired
    private TradeRepository tradeRepository;

    @Test
    void applicationStartup_ShouldCompleteSuccessfully() {
        // Wait for application to be ready
        await().atMost(10, TimeUnit.SECONDS)
               .until(() -> readinessIndicator.isReady());

        // Then
        assertTrue(readinessIndicator.isReady());
        assertTrue(readinessIndicator.isStartupComplete());
        assertEquals("READY", readinessIndicator.getStartupStatus());
        assertNotNull(readinessIndicator.getStartupTime());
        assertNotNull(readinessIndicator.getReadyTime());
        assertNull(readinessIndicator.getLastError());
        assertFalse(shutdownHandler.isShutdownInProgress());
    }

    @Test
    void applicationShutdown_WithActiveTrades_ShouldHandleGracefully() {
        // Given - wait for startup to complete
        await().atMost(10, TimeUnit.SECONDS)
               .until(() -> readinessIndicator.isReady());

        // Create some test trades
        Trade pendingTrade = createAndSaveTrade("order1", OrderStatus.SUBMITTED);
        Trade filledTrade = createAndSaveTrade("order2", OrderStatus.FILLED);

        // Mock trading service responses
        when(tradingService.getOrderStatus("order1")).thenReturn(OrderStatus.SUBMITTED);
        when(tradingService.getOrderStatus("order2")).thenReturn(OrderStatus.FILLED);
        when(tradingService.cancelOrder("order1")).thenReturn(true);

        // When - trigger application shutdown
        new Thread(() -> {
            try {
                Thread.sleep(100); // Small delay to ensure test setup is complete
                applicationContext.close();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        // Then - wait for shutdown to be initiated
        await().atMost(5, TimeUnit.SECONDS)
               .until(() -> shutdownHandler.isShutdownInProgress());

        assertTrue(shutdownHandler.isShutdownInProgress());
    }

    @Test
    void applicationShutdown_WithoutActiveTrades_ShouldCompleteQuickly() {
        // Given - wait for startup to complete
        await().atMost(10, TimeUnit.SECONDS)
               .until(() -> readinessIndicator.isReady());

        // Ensure no active trades
        tradeRepository.deleteAll();

        // When - trigger application shutdown
        long startTime = System.currentTimeMillis();
        new Thread(() -> {
            try {
                Thread.sleep(100);
                applicationContext.close();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        // Then - shutdown should complete quickly
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> shutdownHandler.isShutdownInProgress());

        long shutdownTime = System.currentTimeMillis() - startTime;
        assertTrue(shutdownTime < 2000, "Shutdown should complete quickly without active trades");
    }

    @Test
    void startupReadinessIndicator_ShouldProvideCorrectHealthStatus() {
        // Wait for application to be ready
        await().atMost(10, TimeUnit.SECONDS)
               .until(() -> readinessIndicator.isReady());

        // When
        var health = readinessIndicator.getHealthStatus();

        // Then
        assertEquals("UP", health.get("status"));
        assertEquals("READY", health.get("currentStatus"));
        assertTrue((Boolean) health.get("startupComplete"));
        assertNotNull(health.get("startupTime"));
        assertNotNull(health.get("readyTime"));
    }

    @Test
    void gracefulShutdown_ShouldNotAffectReadinessIndicator() {
        // Given - wait for startup to complete
        await().atMost(10, TimeUnit.SECONDS)
               .until(() -> readinessIndicator.isReady());

        assertTrue(readinessIndicator.isReady());
        String initialStatus = readinessIndicator.getStartupStatus();

        // When - trigger shutdown handler directly (not full app shutdown)
        shutdownHandler.preDestroy();

        // Then - readiness indicator should remain unchanged
        assertTrue(readinessIndicator.isReady());
        assertEquals(initialStatus, readinessIndicator.getStartupStatus());
        assertTrue(shutdownHandler.isShutdownInProgress());
    }

    @Test
    void multipleShutdownCalls_ShouldBeIdempotent() {
        // Given - wait for startup to complete
        await().atMost(10, TimeUnit.SECONDS)
               .until(() -> readinessIndicator.isReady());

        // When - call shutdown multiple times
        shutdownHandler.preDestroy();
        boolean firstShutdownStatus = shutdownHandler.isShutdownInProgress();
        
        shutdownHandler.preDestroy();
        boolean secondShutdownStatus = shutdownHandler.isShutdownInProgress();

        // Then - should be idempotent
        assertTrue(firstShutdownStatus);
        assertTrue(secondShutdownStatus);
        assertEquals(firstShutdownStatus, secondShutdownStatus);
    }

    private Trade createAndSaveTrade(String orderId, OrderStatus status) {
        Trade trade = new Trade();
        trade.setOrderId(orderId);
        trade.setSymbol("AAPL");
        trade.setType(TradeType.BUY);
        trade.setQuantity(BigDecimal.valueOf(10));
        trade.setPrice(BigDecimal.valueOf(150.00));
        trade.setStatus(status);
        trade.setExecutedAt(LocalDateTime.now());
        trade.setAccountId("test-account");
        
        return tradeRepository.save(trade);
    }
}