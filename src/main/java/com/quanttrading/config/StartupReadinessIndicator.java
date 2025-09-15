package com.quanttrading.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Component that tracks application startup and readiness status
 */
@Component("startupReadiness")
public class StartupReadinessIndicator {

    private static final Logger logger = LoggerFactory.getLogger(StartupReadinessIndicator.class);

    private final AtomicBoolean isReady = new AtomicBoolean(false);
    private final AtomicBoolean isStartupComplete = new AtomicBoolean(false);
    private final AtomicReference<String> startupStatus = new AtomicReference<>("INITIALIZING");
    private final AtomicReference<LocalDateTime> startupTime = new AtomicReference<>();
    private final AtomicReference<LocalDateTime> readyTime = new AtomicReference<>();
    private final AtomicReference<String> lastError = new AtomicReference<>();

    /**
     * Get health status as a map
     */
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> health = new HashMap<>();
        
        health.put("status", isReady.get() ? "UP" : "DOWN");
        health.put("ready", isReady.get());
        health.put("startupComplete", isStartupComplete.get());
        health.put("currentStatus", startupStatus.get());
        health.put("startupTime", startupTime.get());
        
        if (isReady.get()) {
            health.put("readyTime", readyTime.get());
        }
        
        if (lastError.get() != null) {
            health.put("lastError", lastError.get());
        }
        
        return health;
    }

    /**
     * Mark the application as starting up
     */
    public void markStartupBegin() {
        startupTime.set(LocalDateTime.now());
        startupStatus.set("STARTING");
        isReady.set(false);
        isStartupComplete.set(false);
        lastError.set(null);
        logger.info("Application startup initiated");
    }

    /**
     * Mark configuration validation as complete
     */
    public void markConfigurationValidated() {
        startupStatus.set("CONFIGURATION_VALIDATED");
        logger.debug("Configuration validation completed");
    }

    /**
     * Mark health checks as complete
     */
    public void markHealthChecksComplete() {
        startupStatus.set("HEALTH_CHECKS_COMPLETE");
        logger.debug("Health checks completed");
    }

    /**
     * Mark portfolio synchronization as complete
     */
    public void markPortfolioSyncComplete() {
        startupStatus.set("PORTFOLIO_SYNC_COMPLETE");
        logger.debug("Portfolio synchronization completed");
    }

    /**
     * Mark the application as ready to serve requests
     */
    public void markReady() {
        readyTime.set(LocalDateTime.now());
        startupStatus.set("READY");
        isReady.set(true);
        isStartupComplete.set(true);
        
        if (startupTime.get() != null) {
            long startupDurationMs = java.time.Duration.between(startupTime.get(), readyTime.get()).toMillis();
            logger.info("Application is ready to serve requests (startup took {} ms)", startupDurationMs);
        } else {
            logger.info("Application is ready to serve requests");
        }
    }

    /**
     * Mark startup as failed with error
     */
    public void markStartupFailed(String error) {
        startupStatus.set("STARTUP_FAILED");
        lastError.set(error);
        isReady.set(false);
        isStartupComplete.set(false);
        logger.error("Application startup failed: {}", error);
    }

    /**
     * Check if the application is ready
     */
    public boolean isReady() {
        return isReady.get();
    }

    /**
     * Check if startup is complete
     */
    public boolean isStartupComplete() {
        return isStartupComplete.get();
    }

    /**
     * Get current startup status
     */
    public String getStartupStatus() {
        return startupStatus.get();
    }

    /**
     * Get startup time
     */
    public LocalDateTime getStartupTime() {
        return startupTime.get();
    }

    /**
     * Get ready time
     */
    public LocalDateTime getReadyTime() {
        return readyTime.get();
    }

    /**
     * Get last error
     */
    public String getLastError() {
        return lastError.get();
    }
}