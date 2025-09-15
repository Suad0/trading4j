package com.quanttrading.config;

import com.quanttrading.service.PortfolioService;
import com.quanttrading.exception.TradingSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Handles application startup initialization tasks
 */
@Component
public class ApplicationStartupInitializer {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationStartupInitializer.class);

    @Value("${trading.default-account-id:default}")
    private String defaultAccountId;

    @Value("${trading.startup.portfolio-sync-enabled:true}")
    private boolean portfolioSyncEnabled;

    @Value("${trading.startup.portfolio-sync-timeout:30}")
    private int portfolioSyncTimeoutSeconds;

    private final PortfolioService portfolioService;
    private final SystemHealthChecker systemHealthChecker;
    private final StartupReadinessIndicator readinessIndicator;

    @Autowired
    public ApplicationStartupInitializer(PortfolioService portfolioService,
                                       SystemHealthChecker systemHealthChecker,
                                       StartupReadinessIndicator readinessIndicator) {
        this.portfolioService = portfolioService;
        this.systemHealthChecker = systemHealthChecker;
        this.readinessIndicator = readinessIndicator;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(100) // Run after configuration validation
    public void initializeApplication() {
        logger.info("Starting application initialization...");
        readinessIndicator.markStartupBegin();

        try {
            // Perform startup health checks
            performStartupHealthChecks();
            readinessIndicator.markHealthChecksComplete();

            // Initialize portfolio synchronization
            if (portfolioSyncEnabled) {
                initializePortfolioSynchronization();
                readinessIndicator.markPortfolioSyncComplete();
            } else {
                logger.info("Portfolio synchronization disabled by configuration");
                readinessIndicator.markPortfolioSyncComplete();
            }

            // Mark application as ready
            readinessIndicator.markReady();
            logger.info("Application initialization completed successfully");

        } catch (Exception e) {
            logger.error("Application initialization failed", e);
            readinessIndicator.markStartupFailed(e.getMessage());
            throw new TradingSystemException("Application startup failed", e);
        }
    }

    private void performStartupHealthChecks() {
        logger.info("Performing startup health checks...");

        try {
            // Check database connectivity
            systemHealthChecker.checkDatabaseHealth();
            logger.info("Database health check passed");

            // Check external API connectivity
            systemHealthChecker.checkExternalApiHealth();
            logger.info("External API health check passed");

            // Check system readiness
            systemHealthChecker.checkSystemReadiness();
            logger.info("System readiness check passed");

        } catch (Exception e) {
            logger.error("Startup health checks failed", e);
            throw new TradingSystemException("Startup health checks failed", e);
        }
    }

    private void initializePortfolioSynchronization() {
        logger.info("Initializing portfolio synchronization for account: {}", defaultAccountId);

        try {
            // First, ensure default portfolio exists
            ensureDefaultPortfolioExists();
            
            // Perform portfolio synchronization asynchronously with timeout
            CompletableFuture<Void> syncFuture = CompletableFuture.runAsync(() -> {
                try {
                    portfolioService.synchronizePortfolio(defaultAccountId);
                    logger.info("Portfolio synchronization completed successfully");
                } catch (Exception e) {
                    logger.error("Portfolio synchronization failed", e);
                    throw new RuntimeException("Portfolio synchronization failed", e);
                }
            });

            // Wait for synchronization with timeout
            syncFuture.get(portfolioSyncTimeoutSeconds, TimeUnit.SECONDS);

        } catch (Exception e) {
            logger.error("Portfolio synchronization initialization failed", e);
            // Don't fail startup for portfolio sync issues, just log the error
            logger.warn("Continuing startup without portfolio synchronization");
        }
    }
    
    private void ensureDefaultPortfolioExists() {
        try {
            // Check if default portfolio exists
            portfolioService.getPortfolio(defaultAccountId);
            logger.info("Default portfolio already exists for account: {}", defaultAccountId);
        } catch (Exception e) {
            // Portfolio doesn't exist, create it
            logger.info("Creating default portfolio for account: {}", defaultAccountId);
            try {
                // Note: Portfolio initialization logic should be implemented in the service
                logger.warn("Portfolio initialization method not available - portfolio will be created on first trade");
            } catch (Exception createException) {
                logger.error("Failed to create default portfolio", createException);
            }
        }
    }
}