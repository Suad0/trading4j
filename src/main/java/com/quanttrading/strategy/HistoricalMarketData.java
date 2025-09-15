package com.quanttrading.strategy;

import com.quanttrading.model.MarketData;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Container for historical market data used by trading strategies.
 */
public class HistoricalMarketData {
    
    private final Map<String, List<MarketData>> historicalData = new ConcurrentHashMap<>();
    private final int maxHistorySize;
    
    public HistoricalMarketData() {
        this(1000); // Default to keeping 1000 data points
    }
    
    public HistoricalMarketData(int maxHistorySize) {
        this.maxHistorySize = maxHistorySize;
    }
    
    /**
     * Add market data for a symbol.
     * @param marketData market data to add
     */
    public void addMarketData(MarketData marketData) {
        if (marketData == null || marketData.getSymbol() == null) {
            return;
        }
        
        String symbol = marketData.getSymbol();
        historicalData.computeIfAbsent(symbol, k -> new ArrayList<>()).add(marketData);
        
        // Maintain maximum history size
        List<MarketData> data = historicalData.get(symbol);
        if (data.size() > maxHistorySize) {
            data.remove(0); // Remove oldest data point
        }
        
        // Keep data sorted by timestamp
        data.sort(Comparator.comparing(MarketData::getTimestamp));
    }
    
    /**
     * Get historical data for a symbol.
     * @param symbol the symbol
     * @return list of historical market data, sorted by timestamp
     */
    public List<MarketData> getHistoricalData(String symbol) {
        return new ArrayList<>(historicalData.getOrDefault(symbol, List.of()));
    }
    
    /**
     * Get the most recent market data for a symbol.
     * @param symbol the symbol
     * @return most recent market data, or null if none available
     */
    public MarketData getLatestData(String symbol) {
        List<MarketData> data = historicalData.get(symbol);
        if (data == null || data.isEmpty()) {
            return null;
        }
        return data.get(data.size() - 1);
    }
    
    /**
     * Get historical prices for a symbol.
     * @param symbol the symbol
     * @return list of prices in chronological order
     */
    public List<BigDecimal> getPrices(String symbol) {
        return getHistoricalData(symbol).stream()
                .map(MarketData::getPrice)
                .filter(Objects::nonNull)
                .toList();
    }
    
    /**
     * Get the last N prices for a symbol.
     * @param symbol the symbol
     * @param count number of prices to retrieve
     * @return list of the last N prices
     */
    public List<BigDecimal> getLastPrices(String symbol, int count) {
        List<BigDecimal> prices = getPrices(symbol);
        if (prices.size() <= count) {
            return prices;
        }
        return prices.subList(prices.size() - count, prices.size());
    }
    
    /**
     * Check if we have enough historical data for a symbol.
     * @param symbol the symbol
     * @param requiredCount minimum number of data points required
     * @return true if we have enough data
     */
    public boolean hasEnoughData(String symbol, int requiredCount) {
        List<MarketData> data = historicalData.get(symbol);
        return data != null && data.size() >= requiredCount;
    }
    
    /**
     * Get the number of data points for a symbol.
     * @param symbol the symbol
     * @return number of data points
     */
    public int getDataCount(String symbol) {
        List<MarketData> data = historicalData.get(symbol);
        return data != null ? data.size() : 0;
    }
    
    /**
     * Clear all historical data.
     */
    public void clear() {
        historicalData.clear();
    }
    
    /**
     * Clear historical data for a specific symbol.
     * @param symbol the symbol
     */
    public void clear(String symbol) {
        historicalData.remove(symbol);
    }
    
    /**
     * Get all symbols that have historical data.
     * @return set of symbols
     */
    public Set<String> getSymbols() {
        return new HashSet<>(historicalData.keySet());
    }
}