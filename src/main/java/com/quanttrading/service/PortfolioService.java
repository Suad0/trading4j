package com.quanttrading.service;

import com.quanttrading.dto.PerformanceMetrics;
import com.quanttrading.model.Portfolio;
import com.quanttrading.model.Position;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for portfolio management operations
 */
public interface PortfolioService {
    
    /**
     * Get the current portfolio for the default account
     * @return Current portfolio
     */
    Portfolio getCurrentPortfolio();
    
    /**
     * Get portfolio by account ID
     * @param accountId Account identifier
     * @return Portfolio if found
     */
    Optional<Portfolio> getPortfolio(String accountId);
    
    /**
     * Get all positions for the default account
     * @return List of positions
     */
    List<Position> getPositions();
    
    /**
     * Get positions for a specific account
     * @param accountId Account identifier
     * @return List of positions
     */
    List<Position> getPositions(String accountId);
    
    /**
     * Get active positions (non-zero quantity) for the default account
     * @return List of active positions
     */
    List<Position> getActivePositions();
    
    /**
     * Get active positions for a specific account
     * @param accountId Account identifier
     * @return List of active positions
     */
    List<Position> getActivePositions(String accountId);
    
    /**
     * Calculate performance metrics for the default account
     * @return Performance metrics
     */
    PerformanceMetrics calculatePerformance();
    
    /**
     * Calculate performance metrics for a specific account
     * @param accountId Account identifier
     * @return Performance metrics
     */
    PerformanceMetrics calculatePerformance(String accountId);
    
    /**
     * Update position after a trade
     * @param symbol Stock symbol
     * @param quantity Quantity traded (positive for buy, negative for sell)
     * @param price Trade price
     */
    void updatePosition(String symbol, BigDecimal quantity, BigDecimal price);
    
    /**
     * Update position for a specific account
     * @param accountId Account identifier
     * @param symbol Stock symbol
     * @param quantity Quantity traded
     * @param price Trade price
     */
    void updatePosition(String accountId, String symbol, BigDecimal quantity, BigDecimal price);
    
    /**
     * Update current prices for all positions
     * @param accountId Account identifier
     */
    void updateCurrentPrices(String accountId);
    
    /**
     * Synchronize portfolio with external broker
     * @param accountId Account identifier
     */
    void synchronizePortfolio(String accountId);
    
    /**
     * Create or update portfolio
     * @param portfolio Portfolio to save
     * @return Saved portfolio
     */
    Portfolio savePortfolio(Portfolio portfolio);
    
    /**
     * Get position by symbol for default account
     * @param symbol Stock symbol
     * @return Position if found
     */
    Optional<Position> getPosition(String symbol);
    
    /**
     * Get position by symbol for specific account
     * @param accountId Account identifier
     * @param symbol Stock symbol
     * @return Position if found
     */
    Optional<Position> getPosition(String accountId, String symbol);
}