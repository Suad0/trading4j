package com.quanttrading.service;

import com.quanttrading.dto.HistoricalData;
import com.quanttrading.model.MarketData;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Service interface for market data operations
 */
public interface MarketDataService {
    
    /**
     * Get current market data for a symbol
     * @param symbol Stock symbol
     * @return Current market data
     */
    Optional<MarketData> getCurrentData(String symbol);
    
    /**
     * Get current market data for multiple symbols
     * @param symbols List of stock symbols
     * @return List of current market data
     */
    List<MarketData> getCurrentData(List<String> symbols);
    
    /**
     * Get historical market data for a symbol
     * @param symbol Stock symbol
     * @param from Start date
     * @param to End date
     * @return List of historical data
     */
    List<HistoricalData> getHistoricalData(String symbol, LocalDate from, LocalDate to);
    
    /**
     * Get historical market data with period specification
     * @param symbol Stock symbol
     * @param period Period (1d, 5d, 1mo, 3mo, 6mo, 1y, 2y, 5y, 10y, ytd, max)
     * @return List of historical data
     */
    List<HistoricalData> getHistoricalData(String symbol, String period);
    
    /**
     * Subscribe to real-time market data updates
     * @param symbol Stock symbol
     * @param callback Callback function to handle updates
     */
    void subscribeToRealTimeData(String symbol, Consumer<MarketData> callback);
    
    /**
     * Unsubscribe from real-time market data updates
     * @param symbol Stock symbol
     */
    void unsubscribeFromRealTimeData(String symbol);
    
    /**
     * Get cached market data if available
     * @param symbol Stock symbol
     * @return Cached market data if available
     */
    Optional<MarketData> getCachedData(String symbol);
    
    /**
     * Refresh market data cache for a symbol
     * @param symbol Stock symbol
     * @return Updated market data
     */
    Optional<MarketData> refreshCache(String symbol);
    
    /**
     * Refresh market data cache for multiple symbols
     * @param symbols List of stock symbols
     */
    void refreshCache(List<String> symbols);
    
    /**
     * Clear market data cache
     */
    void clearCache();
    
    /**
     * Clear market data cache for a specific symbol
     * @param symbol Stock symbol
     */
    void clearCache(String symbol);
    
    /**
     * Check if market data is stale (older than threshold)
     * @param symbol Stock symbol
     * @param maxAgeMinutes Maximum age in minutes
     * @return True if data is stale
     */
    boolean isDataStale(String symbol, int maxAgeMinutes);
    
    /**
     * Validate symbol format
     * @param symbol Stock symbol
     * @return True if symbol is valid
     */
    boolean isValidSymbol(String symbol);
    
    /**
     * Get market status (open/closed)
     * @return True if market is open
     */
    boolean isMarketOpen();
    
    /**
     * Get list of supported symbols
     * @return List of supported symbols
     */
    List<String> getSupportedSymbols();
}