package com.quanttrading.controller;

import com.quanttrading.dto.PerformanceMetrics;
import com.quanttrading.dto.PortfolioSummaryResponse;
import com.quanttrading.dto.PositionResponse;
import com.quanttrading.exception.TradingSystemException;
import com.quanttrading.model.Portfolio;
import com.quanttrading.model.Position;
import com.quanttrading.service.PortfolioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for portfolio management operations
 */
@RestController
@RequestMapping("/api/portfolio")
@CrossOrigin(origins = "*")
public class PortfolioController {
    
    private static final Logger logger = LoggerFactory.getLogger(PortfolioController.class);
    
    private final PortfolioService portfolioService;
    
    @Autowired
    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }
    
    /**
     * Get current portfolio summary
     * @return Portfolio summary with key metrics
     */
    @GetMapping
    public ResponseEntity<PortfolioSummaryResponse> getPortfolioSummary() {
        try {
            logger.debug("Retrieving portfolio summary");
            
            Portfolio portfolio = portfolioService.getCurrentPortfolio();
            PerformanceMetrics metrics = portfolioService.calculatePerformance();
            
            PortfolioSummaryResponse response = new PortfolioSummaryResponse(
                portfolio.getAccountId(),
                metrics.getTotalValue(),
                metrics.getCashBalance(),
                metrics.getPositionsValue(),
                metrics.getTotalUnrealizedPnL(),
                metrics.getPositionCount(),
                portfolio.getLastUpdated()
            );
            
            logger.debug("Portfolio summary retrieved successfully for account: {}", portfolio.getAccountId());
            return ResponseEntity.ok(response);
            
        } catch (TradingSystemException e) {
            logger.error("Error retrieving portfolio summary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get all portfolio positions
     * @return List of current positions
     */
    @GetMapping("/positions")
    public ResponseEntity<List<PositionResponse>> getPositions() {
        try {
            logger.debug("Retrieving all positions");
            
            List<Position> positions = portfolioService.getPositions();
            List<PositionResponse> response = positions.stream()
                .map(this::convertToPositionResponse)
                .collect(Collectors.toList());
            
            logger.debug("Retrieved {} positions", response.size());
            return ResponseEntity.ok(response);
            
        } catch (TradingSystemException e) {
            logger.error("Error retrieving positions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get active positions only (non-zero quantity)
     * @return List of active positions
     */
    @GetMapping("/positions/active")
    public ResponseEntity<List<PositionResponse>> getActivePositions() {
        try {
            logger.debug("Retrieving active positions");
            
            List<Position> positions = portfolioService.getActivePositions();
            List<PositionResponse> response = positions.stream()
                .map(this::convertToPositionResponse)
                .collect(Collectors.toList());
            
            logger.debug("Retrieved {} active positions", response.size());
            return ResponseEntity.ok(response);
            
        } catch (TradingSystemException e) {
            logger.error("Error retrieving active positions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get position for a specific symbol
     * @param symbol Stock symbol
     * @return Position information if found
     */
    @GetMapping("/positions/{symbol}")
    public ResponseEntity<PositionResponse> getPosition(@PathVariable String symbol) {
        try {
            logger.debug("Retrieving position for symbol: {}", symbol);
            
            return portfolioService.getPosition(symbol.toUpperCase())
                .map(position -> {
                    PositionResponse response = convertToPositionResponse(position);
                    logger.debug("Position found for symbol: {}", symbol);
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
                
        } catch (TradingSystemException e) {
            logger.error("Error retrieving position for symbol: {}", symbol, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get portfolio performance metrics
     * @return Detailed performance metrics
     */
    @GetMapping("/performance")
    public ResponseEntity<PerformanceMetrics> getPerformanceMetrics() {
        try {
            logger.debug("Retrieving performance metrics");
            
            PerformanceMetrics metrics = portfolioService.calculatePerformance();
            
            logger.debug("Performance metrics retrieved successfully");
            return ResponseEntity.ok(metrics);
            
        } catch (TradingSystemException e) {
            logger.error("Error retrieving performance metrics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Synchronize portfolio with external broker
     * @return Updated portfolio summary
     */
    @PostMapping("/sync")
    public ResponseEntity<PortfolioSummaryResponse> synchronizePortfolio() {
        try {
            logger.debug("Synchronizing portfolio with external broker");
            
            Portfolio portfolio = portfolioService.getCurrentPortfolio();
            portfolioService.synchronizePortfolio(portfolio.getAccountId());
            
            // Get updated portfolio data
            Portfolio updatedPortfolio = portfolioService.getCurrentPortfolio();
            PerformanceMetrics metrics = portfolioService.calculatePerformance();
            
            PortfolioSummaryResponse response = new PortfolioSummaryResponse(
                updatedPortfolio.getAccountId(),
                metrics.getTotalValue(),
                metrics.getCashBalance(),
                metrics.getPositionsValue(),
                metrics.getTotalUnrealizedPnL(),
                metrics.getPositionCount(),
                updatedPortfolio.getLastUpdated()
            );
            
            logger.info("Portfolio synchronized successfully");
            return ResponseEntity.ok(response);
            
        } catch (TradingSystemException e) {
            logger.error("Error synchronizing portfolio", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Convert Position entity to PositionResponse DTO
     * @param position Position entity
     * @return PositionResponse DTO
     */
    private PositionResponse convertToPositionResponse(Position position) {
        BigDecimal marketValue = BigDecimal.ZERO;
        BigDecimal unrealizedPnL = BigDecimal.ZERO;
        BigDecimal unrealizedPnLPercent = BigDecimal.ZERO;
        BigDecimal costBasis = position.getQuantity().multiply(position.getAveragePrice());
        
        if (position.getCurrentPrice() != null) {
            marketValue = position.getQuantity().multiply(position.getCurrentPrice());
            unrealizedPnL = marketValue.subtract(costBasis);
            
            if (costBasis.compareTo(BigDecimal.ZERO) > 0) {
                unrealizedPnLPercent = unrealizedPnL.divide(costBasis, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            }
        }
        
        return new PositionResponse(
            position.getSymbol(),
            position.getQuantity(),
            position.getAveragePrice(),
            position.getCurrentPrice(),
            marketValue,
            unrealizedPnL,
            unrealizedPnLPercent,
            costBasis,
            position.getLastUpdated()
        );
    }
}