package com.quanttrading.config;

import com.quanttrading.exception.TradingSystemException;
import com.quanttrading.service.PortfolioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationStartupInitializerTest {

    @Mock
    private PortfolioService portfolioService;

    @Mock
    private SystemHealthChecker systemHealthChecker;

    @Mock
    private StartupReadinessIndicator readinessIndicator;

    @Mock
    private ApplicationReadyEvent applicationReadyEvent;

    private ApplicationStartupInitializer startupInitializer;

    @BeforeEach
    void setUp() {
        startupInitializer = new ApplicationStartupInitializer(
                portfolioService, systemHealthChecker, readinessIndicator);
        
        // Set configuration properties
        ReflectionTestUtils.setField(startupInitializer, "defaultAccountId", "test-account");
        ReflectionTestUtils.setField(startupInitializer, "portfolioSyncEnabled", true);
        ReflectionTestUtils.setField(startupInitializer, "portfolioSyncTimeoutSeconds", 5);
    }

    @Test
    void initializeApplication_Success() {
        // Given
        doNothing().when(systemHealthChecker).checkDatabaseHealth();
        doNothing().when(systemHealthChecker).checkExternalApiHealth();
        doNothing().when(systemHealthChecker).checkSystemReadiness();
        doNothing().when(portfolioService).synchronizePortfolio("test-account");

        // When
        startupInitializer.initializeApplication();

        // Then
        verify(readinessIndicator).markStartupBegin();
        verify(systemHealthChecker).checkDatabaseHealth();
        verify(systemHealthChecker).checkExternalApiHealth();
        verify(systemHealthChecker).checkSystemReadiness();
        verify(readinessIndicator).markHealthChecksComplete();
        verify(portfolioService).synchronizePortfolio("test-account");
        verify(readinessIndicator).markPortfolioSyncComplete();
        verify(readinessIndicator).markReady();
    }

    @Test
    void initializeApplication_HealthCheckFailure() {
        // Given
        doThrow(new TradingSystemException("Database connection failed"))
                .when(systemHealthChecker).checkDatabaseHealth();

        // When & Then
        TradingSystemException exception = assertThrows(TradingSystemException.class, () -> {
            startupInitializer.initializeApplication();
        });

        assertEquals("Application startup failed", exception.getMessage());
        verify(readinessIndicator).markStartupBegin();
        verify(readinessIndicator).markStartupFailed("Database connection failed");
        verify(readinessIndicator, never()).markReady();
    }

    @Test
    void initializeApplication_PortfolioSyncFailure() {
        // Given
        doNothing().when(systemHealthChecker).checkDatabaseHealth();
        doNothing().when(systemHealthChecker).checkExternalApiHealth();
        doNothing().when(systemHealthChecker).checkSystemReadiness();
        doThrow(new RuntimeException("Portfolio sync failed"))
                .when(portfolioService).synchronizePortfolio("test-account");

        // When
        startupInitializer.initializeApplication();

        // Then - should continue despite portfolio sync failure
        verify(readinessIndicator).markStartupBegin();
        verify(readinessIndicator).markHealthChecksComplete();
        verify(readinessIndicator).markPortfolioSyncComplete();
        verify(readinessIndicator).markReady();
    }

    @Test
    void initializeApplication_PortfolioSyncDisabled() {
        // Given
        ReflectionTestUtils.setField(startupInitializer, "portfolioSyncEnabled", false);
        doNothing().when(systemHealthChecker).checkDatabaseHealth();
        doNothing().when(systemHealthChecker).checkExternalApiHealth();
        doNothing().when(systemHealthChecker).checkSystemReadiness();

        // When
        startupInitializer.initializeApplication();

        // Then
        verify(readinessIndicator).markStartupBegin();
        verify(readinessIndicator).markHealthChecksComplete();
        verify(portfolioService, never()).synchronizePortfolio(anyString());
        verify(readinessIndicator).markPortfolioSyncComplete();
        verify(readinessIndicator).markReady();
    }

    @Test
    void initializeApplication_ExternalApiHealthCheckFailure() {
        // Given
        doNothing().when(systemHealthChecker).checkDatabaseHealth();
        doThrow(new TradingSystemException("External API not accessible"))
                .when(systemHealthChecker).checkExternalApiHealth();

        // When & Then
        TradingSystemException exception = assertThrows(TradingSystemException.class, () -> {
            startupInitializer.initializeApplication();
        });

        assertEquals("Application startup failed", exception.getMessage());
        verify(readinessIndicator).markStartupBegin();
        verify(readinessIndicator).markStartupFailed("External API not accessible");
        verify(portfolioService, never()).synchronizePortfolio(anyString());
        verify(readinessIndicator, never()).markReady();
    }

    @Test
    void initializeApplication_SystemReadinessCheckFailure() {
        // Given
        doNothing().when(systemHealthChecker).checkDatabaseHealth();
        doNothing().when(systemHealthChecker).checkExternalApiHealth();
        doThrow(new TradingSystemException("System not ready"))
                .when(systemHealthChecker).checkSystemReadiness();

        // When & Then
        TradingSystemException exception = assertThrows(TradingSystemException.class, () -> {
            startupInitializer.initializeApplication();
        });

        assertEquals("Application startup failed", exception.getMessage());
        verify(readinessIndicator).markStartupBegin();
        verify(readinessIndicator).markStartupFailed("System not ready");
        verify(portfolioService, never()).synchronizePortfolio(anyString());
        verify(readinessIndicator, never()).markReady();
    }
}