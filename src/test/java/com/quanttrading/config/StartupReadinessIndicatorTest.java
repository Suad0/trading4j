package com.quanttrading.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Map;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class StartupReadinessIndicatorTest {

    private StartupReadinessIndicator readinessIndicator;

    @BeforeEach
    void setUp() {
        readinessIndicator = new StartupReadinessIndicator();
    }

    @Test
    void initialState() {
        // When & Then
        assertFalse(readinessIndicator.isReady());
        assertFalse(readinessIndicator.isStartupComplete());
        assertEquals("INITIALIZING", readinessIndicator.getStartupStatus());
        assertNull(readinessIndicator.getStartupTime());
        assertNull(readinessIndicator.getReadyTime());
        assertNull(readinessIndicator.getLastError());
    }

    @Test
    void getHealthStatus_InitialState() {
        // When
        Map<String, Object> health = readinessIndicator.getHealthStatus();

        // Then
        assertEquals("DOWN", health.get("status"));
        assertEquals("INITIALIZING", health.get("currentStatus"));
        assertFalse((Boolean) health.get("startupComplete"));
    }

    @Test
    void markStartupBegin() {
        // When
        readinessIndicator.markStartupBegin();

        // Then
        assertFalse(readinessIndicator.isReady());
        assertFalse(readinessIndicator.isStartupComplete());
        assertEquals("STARTING", readinessIndicator.getStartupStatus());
        assertNotNull(readinessIndicator.getStartupTime());
        assertNull(readinessIndicator.getReadyTime());
        assertNull(readinessIndicator.getLastError());
    }

    @Test
    void markConfigurationValidated() {
        // Given
        readinessIndicator.markStartupBegin();

        // When
        readinessIndicator.markConfigurationValidated();

        // Then
        assertEquals("CONFIGURATION_VALIDATED", readinessIndicator.getStartupStatus());
        assertFalse(readinessIndicator.isReady());
    }

    @Test
    void markHealthChecksComplete() {
        // Given
        readinessIndicator.markStartupBegin();

        // When
        readinessIndicator.markHealthChecksComplete();

        // Then
        assertEquals("HEALTH_CHECKS_COMPLETE", readinessIndicator.getStartupStatus());
        assertFalse(readinessIndicator.isReady());
    }

    @Test
    void markPortfolioSyncComplete() {
        // Given
        readinessIndicator.markStartupBegin();

        // When
        readinessIndicator.markPortfolioSyncComplete();

        // Then
        assertEquals("PORTFOLIO_SYNC_COMPLETE", readinessIndicator.getStartupStatus());
        assertFalse(readinessIndicator.isReady());
    }

    @Test
    void markReady() {
        // Given
        readinessIndicator.markStartupBegin();

        // When
        readinessIndicator.markReady();

        // Then
        assertTrue(readinessIndicator.isReady());
        assertTrue(readinessIndicator.isStartupComplete());
        assertEquals("READY", readinessIndicator.getStartupStatus());
        assertNotNull(readinessIndicator.getStartupTime());
        assertNotNull(readinessIndicator.getReadyTime());
        assertNull(readinessIndicator.getLastError());
    }

    @Test
    void markStartupFailed() {
        // Given
        readinessIndicator.markStartupBegin();
        String errorMessage = "Database connection failed";

        // When
        readinessIndicator.markStartupFailed(errorMessage);

        // Then
        assertFalse(readinessIndicator.isReady());
        assertFalse(readinessIndicator.isStartupComplete());
        assertEquals("STARTUP_FAILED", readinessIndicator.getStartupStatus());
        assertEquals(errorMessage, readinessIndicator.getLastError());
    }

    @Test
    void getHealthStatus_ReadyState() {
        // Given
        readinessIndicator.markStartupBegin();
        readinessIndicator.markReady();

        // When
        Map<String, Object> health = readinessIndicator.getHealthStatus();

        // Then
        assertEquals("UP", health.get("status"));
        assertEquals("READY", health.get("currentStatus"));
        assertTrue((Boolean) health.get("startupComplete"));
        assertNotNull(health.get("startupTime"));
        assertNotNull(health.get("readyTime"));
    }

    @Test
    void getHealthStatus_FailedState() {
        // Given
        readinessIndicator.markStartupBegin();
        String errorMessage = "Configuration validation failed";
        readinessIndicator.markStartupFailed(errorMessage);

        // When
        Map<String, Object> health = readinessIndicator.getHealthStatus();

        // Then
        assertEquals("DOWN", health.get("status"));
        assertEquals("STARTUP_FAILED", health.get("currentStatus"));
        assertFalse((Boolean) health.get("startupComplete"));
        assertEquals(errorMessage, health.get("lastError"));
    }

    @Test
    void completeStartupFlow() {
        // Given
        LocalDateTime beforeStartup = LocalDateTime.now();

        // When - simulate complete startup flow
        readinessIndicator.markStartupBegin();
        readinessIndicator.markConfigurationValidated();
        readinessIndicator.markHealthChecksComplete();
        readinessIndicator.markPortfolioSyncComplete();
        readinessIndicator.markReady();

        LocalDateTime afterReady = LocalDateTime.now();

        // Then
        assertTrue(readinessIndicator.isReady());
        assertTrue(readinessIndicator.isStartupComplete());
        assertEquals("READY", readinessIndicator.getStartupStatus());
        
        LocalDateTime startupTime = readinessIndicator.getStartupTime();
        LocalDateTime readyTime = readinessIndicator.getReadyTime();
        
        assertNotNull(startupTime);
        assertNotNull(readyTime);
        assertTrue(startupTime.isAfter(beforeStartup) || startupTime.isEqual(beforeStartup));
        assertTrue(readyTime.isBefore(afterReady) || readyTime.isEqual(afterReady));
        assertTrue(readyTime.isAfter(startupTime) || readyTime.isEqual(startupTime));
        
        assertNull(readinessIndicator.getLastError());
    }

    @Test
    void markReadyWithoutStartupTime() {
        // When - mark ready without calling markStartupBegin first
        readinessIndicator.markReady();

        // Then
        assertTrue(readinessIndicator.isReady());
        assertTrue(readinessIndicator.isStartupComplete());
        assertEquals("READY", readinessIndicator.getStartupStatus());
        assertNull(readinessIndicator.getStartupTime());
        assertNotNull(readinessIndicator.getReadyTime());
    }
}