package com.quanttrading.config;

import com.quanttrading.client.AlpacaApiClient;
import com.quanttrading.client.YFinanceApiClient;
import com.quanttrading.exception.TradingSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Performs system health checks during startup and runtime
 */
@Component
public class SystemHealthChecker {

    private static final Logger logger = LoggerFactory.getLogger(SystemHealthChecker.class);

    private final DataSource dataSource;
    private final AlpacaApiClient alpacaApiClient;
    private final YFinanceApiClient yFinanceApiClient;

    @Autowired
    public SystemHealthChecker(DataSource dataSource,
                             AlpacaApiClient alpacaApiClient,
                             YFinanceApiClient yFinanceApiClient) {
        this.dataSource = dataSource;
        this.alpacaApiClient = alpacaApiClient;
        this.yFinanceApiClient = yFinanceApiClient;
    }

    /**
     * Check database connectivity and basic operations
     */
    public void checkDatabaseHealth() {
        logger.debug("Checking database health...");

        try (Connection connection = dataSource.getConnection()) {
            if (!connection.isValid(5)) {
                throw new TradingSystemException("Database connection is not valid");
            }

            // Test basic query
            var statement = connection.createStatement();
            var resultSet = statement.executeQuery("SELECT 1");
            
            if (!resultSet.next() || resultSet.getInt(1) != 1) {
                throw new TradingSystemException("Database query test failed");
            }

            logger.debug("Database health check passed");

        } catch (SQLException e) {
            logger.error("Database health check failed", e);
            throw new TradingSystemException("Database health check failed", e);
        }
    }

    /**
     * Check external API connectivity
     */
    public void checkExternalApiHealth() {
        logger.debug("Checking external API health...");

        try {
            // Check Alpaca API connectivity
            checkAlpacaApiHealth();

            // Check yFinance API connectivity  
            checkYFinanceApiHealth();

            logger.debug("External API health check passed");

        } catch (Exception e) {
            logger.error("External API health check failed", e);
            // For demo purposes, we'll log the error but not fail startup
            logger.warn("Continuing startup despite external API health check failure");
        }
    }

    /**
     * Check overall system readiness
     */
    public void checkSystemReadiness() {
        logger.debug("Checking system readiness...");

        try {
            // Check available memory
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;

            double memoryUsagePercent = (double) usedMemory / maxMemory * 100;

            logger.info("Memory usage: {:.2f}% ({} MB used of {} MB max)", 
                       memoryUsagePercent, usedMemory / 1024 / 1024, maxMemory / 1024 / 1024);

            if (memoryUsagePercent > 90) {
                logger.warn("High memory usage detected: {:.2f}%", memoryUsagePercent);
            }

            // Check available disk space (for database file)
            java.io.File dbDir = new java.io.File("./data");
            if (!dbDir.exists()) {
                dbDir.mkdirs();
            }

            long freeSpace = dbDir.getFreeSpace();
            long totalSpace = dbDir.getTotalSpace();
            double diskUsagePercent = (double) (totalSpace - freeSpace) / totalSpace * 100;

            logger.info("Disk usage: {:.2f}% ({} MB free of {} MB total)", 
                       diskUsagePercent, freeSpace / 1024 / 1024, totalSpace / 1024 / 1024);

            if (diskUsagePercent > 95) {
                logger.warn("Low disk space detected: {:.2f}% used", diskUsagePercent);
            }

            logger.debug("System readiness check passed");

        } catch (Exception e) {
            logger.error("System readiness check failed", e);
            throw new TradingSystemException("System readiness check failed", e);
        }
    }

    private void checkAlpacaApiHealth() {
        try {
            logger.debug("Checking Alpaca API connectivity...");
            
            // Try to get account info to verify API connectivity
            alpacaApiClient.getAccountInfo();
            
            logger.debug("Alpaca API health check passed");
            
        } catch (Exception e) {
            logger.error("Alpaca API health check failed", e);
            throw new TradingSystemException("Alpaca API is not accessible", e);
        }
    }

    private void checkYFinanceApiHealth() {
        try {
            logger.debug("Checking yFinance API connectivity...");
            
            // Try to get market data for a common symbol to verify API connectivity
            yFinanceApiClient.getCurrentQuote("AAPL");
            
            logger.debug("yFinance API health check passed");
            
        } catch (Exception e) {
            logger.error("yFinance API health check failed", e);
            throw new TradingSystemException("yFinance API is not accessible", e);
        }
    }
}