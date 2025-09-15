package com.quanttrading.service.impl;

import com.quanttrading.client.YFinanceApiClient;
import com.quanttrading.dto.HistoricalData;
import com.quanttrading.exception.TradingSystemException;
import com.quanttrading.model.MarketData;
import com.quanttrading.service.MarketDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Implementation of MarketDataService for market data operations
 */
@Service
public class MarketDataServiceImpl implements MarketDataService {
    
    private static final Logger logger = LoggerFactory.getLogger(MarketDataServiceImpl.class);
    
    private static final Pattern SYMBOL_PATTERN = Pattern.compile("^[A-Z]{1,5}$");
    private static final List<String> VALID_PERIODS = Arrays.asList(
        "1d", "5d", "1mo", "3mo", "6mo", "1y", "2y", "5y", "10y", "ytd", "max"
    );
    
    @Value("${market-data.cache-ttl-minutes:5}")
    private int cacheTtlMinutes;
    
    @Value("${market-data.rate-limit-delay-ms:100}")
    private long rateLimitDelayMs;
    
    private final YFinanceApiClient yFinanceApiClient;
    private final Map<String, Consumer<MarketData>> subscriptions = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastRequestTime = new ConcurrentHashMap<>();
    
    @Autowired
    public MarketDataServiceImpl(YFinanceApiClient yFinanceApiClient) {
        this.yFinanceApiClient = yFinanceApiClient;
    }
    
    @Override
    @Cacheable(value = "marketData", key = "#symbol")
    public Optional<MarketData> getCurrentData(String symbol) {
        logger.debug("Getting current market data for symbol: {}", symbol);
        
        if (!isValidSymbol(symbol)) {
            logger.warn("Invalid symbol format: {}", symbol);
            return Optional.empty();
        }
        
        try {
            // Apply rate limiting
            applyRateLimit(symbol);
            
            MarketData data = yFinanceApiClient.getCurrentQuote(symbol);
            if (data != null) {
                logger.debug("Retrieved market data for {}: price={}, volume={}", 
                           symbol, data.getPrice(), data.getVolume());
                return Optional.of(data);
            } else {
                logger.warn("No market data found for symbol: {}", symbol);
                return Optional.empty();
            }
            
        } catch (Exception e) {
            logger.error("Failed to get current market data for symbol: {}", symbol, e);
            return Optional.empty();
        }
    }
    
    @Override
    public List<MarketData> getCurrentData(List<String> symbols) {
        logger.debug("Getting current market data for {} symbols", symbols.size());
        
        return symbols.stream()
                .filter(this::isValidSymbol)
                .map(this::getCurrentData)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<HistoricalData> getHistoricalData(String symbol, LocalDate from, LocalDate to) {
        logger.debug("Getting historical data for symbol: {} from {} to {}", symbol, from, to);
        
        if (!isValidSymbol(symbol)) {
            throw new TradingSystemException("Invalid symbol format: " + symbol);
        }
        
        if (from.isAfter(to)) {
            throw new TradingSystemException("Start date must be before end date");
        }
        
        try {
            // Apply rate limiting
            applyRateLimit(symbol);
            
            // Convert date range to period string for yFinance API
            String period = calculatePeriod(from, to);
            List<HistoricalData> data = yFinanceApiClient.getHistoricalData(symbol, period);
            
            logger.debug("Retrieved {} historical data points for symbol: {}", data.size(), symbol);
            return data;
            
        } catch (Exception e) {
            logger.error("Failed to get historical data for symbol: {}", symbol, e);
            throw new TradingSystemException("Failed to retrieve historical data", e);
        }
    }
    
    @Override
    public List<HistoricalData> getHistoricalData(String symbol, String period) {
        logger.debug("Getting historical data for symbol: {} with period: {}", symbol, period);
        
        if (!isValidSymbol(symbol)) {
            throw new TradingSystemException("Invalid symbol format: " + symbol);
        }
        
        if (!VALID_PERIODS.contains(period)) {
            throw new TradingSystemException("Invalid period: " + period);
        }
        
        try {
            // Apply rate limiting
            applyRateLimit(symbol);
            
            List<HistoricalData> data = yFinanceApiClient.getHistoricalData(symbol, period);
            
            logger.debug("Retrieved {} historical data points for symbol: {} with period: {}", 
                        data.size(), symbol, period);
            return data;
            
        } catch (Exception e) {
            logger.error("Failed to get historical data for symbol: {} with period: {}", symbol, period, e);
            throw new TradingSystemException("Failed to retrieve historical data", e);
        }
    }
    
    @Override
    public void subscribeToRealTimeData(String symbol, Consumer<MarketData> callback) {
        logger.info("Subscribing to real-time data for symbol: {}", symbol);
        
        if (!isValidSymbol(symbol)) {
            throw new TradingSystemException("Invalid symbol format: " + symbol);
        }
        
        subscriptions.put(symbol, callback);
        
        // In a real implementation, this would establish a WebSocket connection
        // For now, we'll just log the subscription
        logger.info("Real-time subscription established for symbol: {}", symbol);
    }
    
    @Override
    public void unsubscribeFromRealTimeData(String symbol) {
        logger.info("Unsubscribing from real-time data for symbol: {}", symbol);
        
        subscriptions.remove(symbol);
        
        // In a real implementation, this would close the WebSocket connection
        logger.info("Real-time subscription removed for symbol: {}", symbol);
    }
    
    @Override
    @Cacheable(value = "marketData", key = "#symbol")
    public Optional<MarketData> getCachedData(String symbol) {
        logger.debug("Getting cached market data for symbol: {}", symbol);
        
        // This method relies on Spring's caching mechanism
        // The actual data retrieval is handled by getCurrentData
        return getCurrentData(symbol);
    }
    
    @Override
    @CacheEvict(value = "marketData", key = "#symbol")
    public Optional<MarketData> refreshCache(String symbol) {
        logger.debug("Refreshing cache for symbol: {}", symbol);
        
        // Evict the cache entry and get fresh data
        return getCurrentData(symbol);
    }
    
    @Override
    public void refreshCache(List<String> symbols) {
        logger.debug("Refreshing cache for {} symbols", symbols.size());
        
        symbols.forEach(this::refreshCache);
    }
    
    @Override
    @CacheEvict(value = "marketData", allEntries = true)
    public void clearCache() {
        logger.info("Clearing all market data cache");
    }
    
    @Override
    @CacheEvict(value = "marketData", key = "#symbol")
    public void clearCache(String symbol) {
        logger.debug("Clearing cache for symbol: {}", symbol);
    }
    
    @Override
    public boolean isDataStale(String symbol, int maxAgeMinutes) {
        logger.debug("Checking if data is stale for symbol: {} (max age: {} minutes)", symbol, maxAgeMinutes);
        
        Optional<MarketData> data = getCachedData(symbol);
        if (data.isEmpty()) {
            return true;
        }
        
        LocalDateTime dataTime = data.get().getTimestamp();
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(maxAgeMinutes);
        
        boolean isStale = dataTime.isBefore(threshold);
        logger.debug("Data for symbol {} is {}", symbol, isStale ? "stale" : "fresh");
        
        return isStale;
    }
    
    @Override
    public boolean isValidSymbol(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return false;
        }
        
        String trimmedSymbol = symbol.trim();
        boolean valid = SYMBOL_PATTERN.matcher(trimmedSymbol).matches();
        
        if (!valid) {
            logger.debug("Invalid symbol format: {}", symbol);
        }
        
        return valid;
    }
    
    @Override
    public boolean isMarketOpen() {
        // Simplified market hours check (NYSE: 9:30 AM - 4:00 PM ET)
        LocalTime now = LocalTime.now();
        LocalTime marketOpen = LocalTime.of(9, 30);
        LocalTime marketClose = LocalTime.of(16, 0);
        
        boolean isOpen = now.isAfter(marketOpen) && now.isBefore(marketClose);
        logger.debug("Market is currently {}", isOpen ? "open" : "closed");
        
        return isOpen;
    }
    
    @Override
    public List<String> getSupportedSymbols() {
        // In a real implementation, this would return a list of supported symbols
        // For now, return a sample list
        return Arrays.asList("AAPL", "GOOGL", "MSFT", "AMZN", "TSLA", "META", "NVDA", "NFLX");
    }
    
    // Private helper methods
    
    private void applyRateLimit(String symbol) {
        LocalDateTime lastRequest = lastRequestTime.get(symbol);
        if (lastRequest != null) {
            long timeSinceLastRequest = java.time.Duration.between(lastRequest, LocalDateTime.now()).toMillis();
            if (timeSinceLastRequest < rateLimitDelayMs) {
                try {
                    long sleepTime = rateLimitDelayMs - timeSinceLastRequest;
                    logger.debug("Rate limiting: sleeping for {} ms for symbol: {}", sleepTime, symbol);
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Rate limiting sleep interrupted for symbol: {}", symbol);
                }
            }
        }
        lastRequestTime.put(symbol, LocalDateTime.now());
    }
    
    private String calculatePeriod(LocalDate from, LocalDate to) {
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(from, to);
        
        if (daysBetween <= 5) {
            return "5d";
        } else if (daysBetween <= 30) {
            return "1mo";
        } else if (daysBetween <= 90) {
            return "3mo";
        } else if (daysBetween <= 180) {
            return "6mo";
        } else if (daysBetween <= 365) {
            return "1y";
        } else if (daysBetween <= 730) {
            return "2y";
        } else if (daysBetween <= 1825) {
            return "5y";
        } else {
            return "10y";
        }
    }
}