package com.quanttrading.controller;

import com.quanttrading.config.GracefulShutdownHandler;
import com.quanttrading.config.StartupReadinessIndicator;
import com.quanttrading.service.StrategyService;
import com.quanttrading.service.PortfolioService;
import com.quanttrading.dto.PerformanceMetrics;
import com.quanttrading.service.StrategyPerformance;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Controller for monitoring and performance endpoints
 */
@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {

    private final StrategyService strategyService;
    private final PortfolioService portfolioService;
    private final StartupReadinessIndicator readinessIndicator;
    private final GracefulShutdownHandler shutdownHandler;

    public MonitoringController(StrategyService strategyService, 
                              PortfolioService portfolioService,
                              StartupReadinessIndicator readinessIndicator,
                              GracefulShutdownHandler shutdownHandler) {
        this.strategyService = strategyService;
        this.portfolioService = portfolioService;
        this.readinessIndicator = readinessIndicator;
        this.shutdownHandler = shutdownHandler;
    }

    /**
     * Get performance metrics for all strategies
     */
    @GetMapping("/strategies/performance")
    public ResponseEntity<Map<String, StrategyPerformance>> getStrategyPerformance() {
        var performance = strategyService.getStrategyPerformance();
        return ResponseEntity.ok(performance);
    }

    /**
     * Get performance metrics for a specific strategy
     */
    @GetMapping("/strategies/{strategyName}/performance")
    public ResponseEntity<StrategyPerformance> getStrategyPerformance(@PathVariable String strategyName) {
        var performance = strategyService.getStrategyPerformance(strategyName);
        
        return performance != null ? ResponseEntity.ok(performance) : ResponseEntity.notFound().build();
    }

    /**
     * Get overall portfolio performance metrics
     */
    @GetMapping("/portfolio/performance")
    public ResponseEntity<PerformanceMetrics> getPortfolioPerformance() {
        var performance = portfolioService.calculatePerformance();
        return ResponseEntity.ok(performance);
    }

    /**
     * Get system health summary
     */
    @GetMapping("/health/summary")
    public ResponseEntity<Map<String, Object>> getHealthSummary() {
        Map<String, Object> healthSummary = new HashMap<>();
        
        try {
            // Get basic portfolio info
            var portfolio = portfolioService.getCurrentPortfolio();
            var positions = portfolioService.getPositions();
            var performance = portfolioService.calculatePerformance();
            
            healthSummary.put("portfolio_value", portfolio.getTotalValue());
            healthSummary.put("cash_balance", portfolio.getCashBalance());
            healthSummary.put("active_positions", positions.size());
            healthSummary.put("daily_pnl", performance.getDailyPnL());
            healthSummary.put("total_pnl", performance.getTotalUnrealizedPnL());
            healthSummary.put("status", "healthy");
            
        } catch (Exception e) {
            healthSummary.put("status", "error");
            healthSummary.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(healthSummary);
    }

    /**
     * Get application readiness status
     */
    @GetMapping("/readiness")
    public ResponseEntity<Map<String, Object>> getReadinessStatus() {
        Map<String, Object> readiness = new HashMap<>();
        readiness.put("ready", readinessIndicator.isReady());
        readiness.put("startupComplete", readinessIndicator.isStartupComplete());
        readiness.put("status", readinessIndicator.getStartupStatus());
        readiness.put("startupTime", readinessIndicator.getStartupTime());
        readiness.put("readyTime", readinessIndicator.getReadyTime());
        readiness.put("shutdownInProgress", shutdownHandler.isShutdownInProgress());
        
        if (readinessIndicator.getLastError() != null) {
            readiness.put("lastError", readinessIndicator.getLastError());
        }
        
        return ResponseEntity.ok(readiness);
    }

    /**
     * Get system status including startup and shutdown state
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("application", "Quantitative Trading System");
        status.put("version", "1.0.0");
        status.put("status", readinessIndicator.isReady() ? "READY" : readinessIndicator.getStartupStatus());
        status.put("ready", readinessIndicator.isReady());
        status.put("shutdownInProgress", shutdownHandler.isShutdownInProgress());
        status.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(status);
    }

    /**
     * Get trading system statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getSystemStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            var positions = portfolioService.getPositions();
            var strategies = strategyService.getStrategyPerformance();
            
            stats.put("total_positions", positions.size());
            stats.put("total_strategies", strategies.size());
            stats.put("active_strategies", strategies.values().stream()
                .mapToInt(s -> s.isActive() ? 1 : 0)
                .sum());
            
            // Calculate total trades from all strategies
            int totalTrades = strategies.values().stream()
                .mapToInt(StrategyPerformance::getTotalTrades)
                .sum();
            stats.put("total_trades", totalTrades);
            
            // Calculate win rate across all strategies
            double totalWinRate = strategies.values().stream()
                .mapToDouble(StrategyPerformance::getWinRate)
                .average()
                .orElse(0.0);
            stats.put("average_win_rate", totalWinRate);
            
        } catch (Exception e) {
            stats.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(stats);
    }
}