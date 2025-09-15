package com.quanttrading.config;

import com.quanttrading.model.OrderStatus;
import com.quanttrading.model.Trade;
import com.quanttrading.repository.TradeRepository;
import com.quanttrading.service.TradingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Handles graceful shutdown of the application, ensuring active trades are properly handled
 */
@Component
public class GracefulShutdownHandler {

    private static final Logger logger = LoggerFactory.getLogger(GracefulShutdownHandler.class);

    @Value("${trading.shutdown.timeout-seconds:30}")
    private int shutdownTimeoutSeconds;

    @Value("${trading.shutdown.cancel-pending-orders:true}")
    private boolean cancelPendingOrders;

    @Value("${trading.shutdown.wait-for-fills:true}")
    private boolean waitForFills;

    private final TradingService tradingService;
    private final TradeRepository tradeRepository;

    private volatile boolean shutdownInProgress = false;

    @Autowired
    public GracefulShutdownHandler(TradingService tradingService,
                                 TradeRepository tradeRepository) {
        this.tradingService = tradingService;
        this.tradeRepository = tradeRepository;
    }

    @EventListener(ContextClosedEvent.class)
    public void handleContextClosed() {
        logger.info("Application shutdown initiated - handling active trades...");
        performGracefulShutdown();
    }

    @PreDestroy
    public void preDestroy() {
        if (!shutdownInProgress) {
            logger.info("PreDestroy called - performing graceful shutdown...");
            performGracefulShutdown();
        }
    }

    private void performGracefulShutdown() {
        shutdownInProgress = true;
        
        try {
            logger.info("Starting graceful shutdown process...");

            // Get all active trades (pending orders)
            List<Trade> activeTrades = getActiveTrades();
            
            if (activeTrades.isEmpty()) {
                logger.info("No active trades found - shutdown can proceed immediately");
                return;
            }

            logger.info("Found {} active trades to handle during shutdown", activeTrades.size());

            // Handle active trades based on configuration
            if (cancelPendingOrders) {
                cancelPendingOrders(activeTrades);
            }

            if (waitForFills) {
                waitForOrderFills(activeTrades);
            }

            // Final status check
            logFinalTradeStatus();

            logger.info("Graceful shutdown process completed");

        } catch (Exception e) {
            logger.error("Error during graceful shutdown", e);
        }
    }

    private List<Trade> getActiveTrades() {
        try {
            // Get recent trades that might still be pending
            List<Trade> recentTrades = tradeRepository.findRecentTrades(LocalDateTime.now().minusHours(1));
            
            return recentTrades.stream()
                    .filter(trade -> isPendingOrder(trade))
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            logger.error("Error retrieving active trades", e);
            return List.of();
        }
    }

    private boolean isPendingOrder(Trade trade) {
        try {
            if (trade.getOrderId() == null) {
                return false;
            }

            OrderStatus status = tradingService.getOrderStatus(trade.getOrderId());
            return status == OrderStatus.PENDING || 
                   status == OrderStatus.SUBMITTED || 
                   status == OrderStatus.PARTIALLY_FILLED;
                   
        } catch (Exception e) {
            logger.warn("Could not check status for order: {}", trade.getOrderId(), e);
            return false;
        }
    }

    private void cancelPendingOrders(List<Trade> activeTrades) {
        logger.info("Cancelling {} pending orders...", activeTrades.size());

        List<CompletableFuture<Void>> cancellationFutures = activeTrades.stream()
                .map(trade -> CompletableFuture.runAsync(() -> {
                    try {
                        boolean cancelled = tradingService.cancelOrder(trade.getOrderId());
                        if (cancelled) {
                            logger.info("Successfully cancelled order: {}", trade.getOrderId());
                        } else {
                            logger.warn("Failed to cancel order: {}", trade.getOrderId());
                        }
                    } catch (Exception e) {
                        logger.error("Error cancelling order: {}", trade.getOrderId(), e);
                    }
                }))
                .collect(Collectors.toList());

        try {
            // Wait for all cancellations to complete with timeout
            CompletableFuture<Void> allCancellations = CompletableFuture.allOf(
                    cancellationFutures.toArray(new CompletableFuture[0]));
            
            allCancellations.get(shutdownTimeoutSeconds / 2, TimeUnit.SECONDS);
            logger.info("Order cancellation process completed");
            
        } catch (Exception e) {
            logger.warn("Order cancellation process timed out or failed", e);
        }
    }

    private void waitForOrderFills(List<Trade> activeTrades) {
        logger.info("Waiting for order fills (timeout: {} seconds)...", shutdownTimeoutSeconds);

        long startTime = System.currentTimeMillis();
        long timeoutMillis = shutdownTimeoutSeconds * 1000L;

        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            try {
                List<Trade> stillPending = activeTrades.stream()
                        .filter(this::isPendingOrder)
                        .collect(Collectors.toList());

                if (stillPending.isEmpty()) {
                    logger.info("All orders have been filled or cancelled");
                    break;
                }

                logger.debug("Still waiting for {} orders to complete", stillPending.size());
                Thread.sleep(1000); // Wait 1 second before checking again

            } catch (InterruptedException e) {
                logger.warn("Wait for order fills interrupted", e);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Error while waiting for order fills", e);
                break;
            }
        }

        if (System.currentTimeMillis() - startTime >= timeoutMillis) {
            logger.warn("Timeout reached while waiting for order fills");
        }
    }

    private void logFinalTradeStatus() {
        try {
            List<Trade> recentTrades = tradeRepository.findRecentTrades(LocalDateTime.now().minusHours(1));
            
            long pendingCount = recentTrades.stream()
                    .filter(this::isPendingOrder)
                    .count();

            if (pendingCount > 0) {
                logger.warn("Shutdown completed with {} orders still pending", pendingCount);
            } else {
                logger.info("Shutdown completed with no pending orders");
            }

        } catch (Exception e) {
            logger.error("Error logging final trade status", e);
        }
    }

    /**
     * Check if shutdown is in progress
     */
    public boolean isShutdownInProgress() {
        return shutdownInProgress;
    }
}