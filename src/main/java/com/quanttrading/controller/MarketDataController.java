package com.quanttrading.controller;

import com.quanttrading.dto.HistoricalData;
import com.quanttrading.dto.MarketDataResponse;
import com.quanttrading.dto.MarketStatusResponse;
import com.quanttrading.exception.TradingSystemException;
import com.quanttrading.model.MarketData;
import com.quanttrading.service.MarketDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * REST controller for market data operations
 */
@RestController
@RequestMapping("/api/market-data")
@CrossOrigin(origins = "*")
public class MarketDataController {
    
    private static final Logger logger = LoggerFactory.getLogger(MarketDataController.class);
    
    private final MarketDataService marketDataService;
    
    @Autowired
    public MarketDataController(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }
    
    /**
     * Get current market data for a symbol
     * @param symbol Stock symbol
     * @return Current market data
     */
    @GetMapping("/{symbol}")
    public ResponseEntity<MarketDataResponse> getCurrentMarketData(@PathVariable String symbol) {
        try {
            logger.debug("Getting current market data for symbol: {}", symbol);
            
            if (!marketDataService.isValidSymbol(symbol)) {
                logger.warn("Invalid symbol format: {}", symbol);
                return ResponseEntity.badRequest().build();
            }
            
            Optional<MarketData> marketData = marketDataService.getCurrentData(symbol.toUpperCase());
            
            if (marketData.isPresent()) {
                MarketDataResponse response = convertToMarketDataResponse(marketData.get());
                response.setMarketOpen(marketDataService.isMarketOpen());
                
                logger.debug("Market data retrieved for symbol: {}", symbol);
                
                // Set cache headers for 1 minute
                return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(1, TimeUnit.MINUTES))
                    .body(response);
            } else {
                logger.warn("No market data found for symbol: {}", symbol);
                return ResponseEntity.notFound().build();
            }
            
        } catch (TradingSystemException e) {
            logger.error("Error retrieving market data for symbol: {}", symbol, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get current market data for multiple symbols
     * @param symbols Comma-separated list of symbols
     * @return List of current market data
     */
    @GetMapping("/batch")
    public ResponseEntity<List<MarketDataResponse>> getCurrentMarketDataBatch(
            @RequestParam String symbols) {
        try {
            logger.debug("Getting current market data for symbols: {}", symbols);
            
            List<String> symbolList = Arrays.stream(symbols.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .collect(Collectors.toList());
            
            // Validate symbols
            List<String> invalidSymbols = symbolList.stream()
                .filter(symbol -> !marketDataService.isValidSymbol(symbol))
                .collect(Collectors.toList());
            
            if (!invalidSymbols.isEmpty()) {
                logger.warn("Invalid symbols found: {}", invalidSymbols);
                return ResponseEntity.badRequest().build();
            }
            
            List<MarketData> marketDataList = marketDataService.getCurrentData(symbolList);
            List<MarketDataResponse> response = marketDataList.stream()
                .map(this::convertToMarketDataResponse)
                .peek(data -> data.setMarketOpen(marketDataService.isMarketOpen()))
                .collect(Collectors.toList());
            
            logger.debug("Retrieved market data for {} symbols", response.size());
            
            // Set cache headers for 1 minute
            return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.MINUTES))
                .body(response);
            
        } catch (TradingSystemException e) {
            logger.error("Error retrieving batch market data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get historical market data for a symbol with date range
     * @param symbol Stock symbol
     * @param from Start date (YYYY-MM-DD)
     * @param to End date (YYYY-MM-DD)
     * @return List of historical data
     */
    @GetMapping("/{symbol}/history")
    public ResponseEntity<List<HistoricalData>> getHistoricalData(
            @PathVariable String symbol,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        try {
            logger.debug("Getting historical data for symbol: {} from {} to {}", symbol, from, to);
            
            if (!marketDataService.isValidSymbol(symbol)) {
                logger.warn("Invalid symbol format: {}", symbol);
                return ResponseEntity.badRequest().build();
            }
            
            if (from.isAfter(to)) {
                logger.warn("Invalid date range: from {} to {}", from, to);
                return ResponseEntity.badRequest().build();
            }
            
            List<HistoricalData> historicalData = marketDataService.getHistoricalData(
                symbol.toUpperCase(), from, to);
            
            logger.debug("Retrieved {} historical data points for symbol: {}", 
                        historicalData.size(), symbol);
            
            // Set cache headers for 5 minutes for historical data
            return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES))
                .body(historicalData);
            
        } catch (TradingSystemException e) {
            logger.error("Error retrieving historical data for symbol: {}", symbol, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get historical market data for a symbol with period
     * @param symbol Stock symbol
     * @param period Period (1d, 5d, 1mo, 3mo, 6mo, 1y, 2y, 5y, 10y, ytd, max)
     * @return List of historical data
     */
    @GetMapping("/{symbol}/history/{period}")
    public ResponseEntity<List<HistoricalData>> getHistoricalDataByPeriod(
            @PathVariable String symbol,
            @PathVariable String period) {
        try {
            logger.debug("Getting historical data for symbol: {} with period: {}", symbol, period);
            
            if (!marketDataService.isValidSymbol(symbol)) {
                logger.warn("Invalid symbol format: {}", symbol);
                return ResponseEntity.badRequest().build();
            }
            
            List<HistoricalData> historicalData = marketDataService.getHistoricalData(
                symbol.toUpperCase(), period);
            
            logger.debug("Retrieved {} historical data points for symbol: {} with period: {}", 
                        historicalData.size(), symbol, period);
            
            // Set cache headers for 5 minutes for historical data
            return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES))
                .body(historicalData);
            
        } catch (TradingSystemException e) {
            logger.error("Error retrieving historical data for symbol: {} with period: {}", 
                        symbol, period, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get market status and supported symbols
     * @return Market status information
     */
    @GetMapping("/status")
    public ResponseEntity<MarketStatusResponse> getMarketStatus() {
        try {
            logger.debug("Getting market status");
            
            boolean isOpen = marketDataService.isMarketOpen();
            String status = isOpen ? "OPEN" : "CLOSED";
            List<String> supportedSymbols = marketDataService.getSupportedSymbols();
            
            MarketStatusResponse response = new MarketStatusResponse(isOpen, status, supportedSymbols);
            
            logger.debug("Market status retrieved - Status: {}, Supported symbols: {}", 
                        status, supportedSymbols.size());
            
            // Set cache headers for 1 minute
            return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.MINUTES))
                .body(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving market status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Refresh market data cache for a symbol
     * @param symbol Stock symbol
     * @return Updated market data
     */
    @PostMapping("/{symbol}/refresh")
    public ResponseEntity<MarketDataResponse> refreshMarketData(@PathVariable String symbol) {
        try {
            logger.debug("Refreshing market data cache for symbol: {}", symbol);
            
            if (!marketDataService.isValidSymbol(symbol)) {
                logger.warn("Invalid symbol format: {}", symbol);
                return ResponseEntity.badRequest().build();
            }
            
            Optional<MarketData> marketData = marketDataService.refreshCache(symbol.toUpperCase());
            
            if (marketData.isPresent()) {
                MarketDataResponse response = convertToMarketDataResponse(marketData.get());
                response.setMarketOpen(marketDataService.isMarketOpen());
                
                logger.debug("Market data cache refreshed for symbol: {}", symbol);
                return ResponseEntity.ok(response);
            } else {
                logger.warn("No market data found after refresh for symbol: {}", symbol);
                return ResponseEntity.notFound().build();
            }
            
        } catch (TradingSystemException e) {
            logger.error("Error refreshing market data for symbol: {}", symbol, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Clear market data cache
     * @param symbol Optional symbol to clear specific cache entry
     * @return Success status
     */
    @DeleteMapping("/cache")
    public ResponseEntity<Void> clearCache(@RequestParam(required = false) String symbol) {
        try {
            if (symbol != null && !symbol.trim().isEmpty()) {
                logger.debug("Clearing cache for symbol: {}", symbol);
                
                if (!marketDataService.isValidSymbol(symbol)) {
                    logger.warn("Invalid symbol format: {}", symbol);
                    return ResponseEntity.badRequest().build();
                }
                
                marketDataService.clearCache(symbol.toUpperCase());
                logger.debug("Cache cleared for symbol: {}", symbol);
            } else {
                logger.debug("Clearing all market data cache");
                marketDataService.clearCache();
                logger.debug("All market data cache cleared");
            }
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            logger.error("Error clearing cache", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Check if market data is stale for a symbol
     * @param symbol Stock symbol
     * @param maxAge Maximum age in minutes (default: 5)
     * @return Stale status information
     */
    @GetMapping("/{symbol}/stale")
    public ResponseEntity<String> checkDataStale(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "5") int maxAge) {
        try {
            logger.debug("Checking if data is stale for symbol: {} (max age: {} minutes)", 
                        symbol, maxAge);
            
            if (!marketDataService.isValidSymbol(symbol)) {
                logger.warn("Invalid symbol format: {}", symbol);
                return ResponseEntity.badRequest().build();
            }
            
            if (maxAge <= 0 || maxAge > 1440) { // Max 24 hours
                logger.warn("Invalid max age value: {}", maxAge);
                return ResponseEntity.badRequest().body("Max age must be between 1 and 1440 minutes");
            }
            
            boolean isStale = marketDataService.isDataStale(symbol.toUpperCase(), maxAge);
            String message = String.format("Data for %s is %s", symbol.toUpperCase(), 
                                          isStale ? "stale" : "fresh");
            
            logger.debug("Data staleness check for symbol: {} - {}", symbol, 
                        isStale ? "stale" : "fresh");
            
            return ResponseEntity.ok(message);
            
        } catch (Exception e) {
            logger.error("Error checking data staleness for symbol: {}", symbol, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error checking data staleness");
        }
    }
    
    /**
     * Convert MarketData entity to MarketDataResponse DTO
     * @param marketData MarketData entity
     * @return MarketDataResponse DTO
     */
    private MarketDataResponse convertToMarketDataResponse(MarketData marketData) {
        return new MarketDataResponse(
            marketData.getSymbol(),
            marketData.getPrice(),
            marketData.getVolume(),
            marketData.getHigh(),
            marketData.getLow(),
            marketData.getOpen(),
            marketData.getTimestamp()
        );
    }
}