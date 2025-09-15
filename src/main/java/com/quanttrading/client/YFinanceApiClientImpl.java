package com.quanttrading.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quanttrading.dto.HistoricalData;
import com.quanttrading.exception.ApiConnectionException;
import com.quanttrading.model.MarketData;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of yFinance API client with rate limiting and caching
 */
@Component
public class YFinanceApiClientImpl implements YFinanceApiClient {
    
    private static final Logger logger = LoggerFactory.getLogger(YFinanceApiClientImpl.class);
    private final String baseUrl;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Bucket rateLimitBucket;
    private final Map<String, MarketData> quoteCache = new ConcurrentHashMap<>();
    private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION_MS = 60000; // 1 minute cache
    
    @Autowired
    public YFinanceApiClientImpl(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this(restTemplate, objectMapper, "https://query1.finance.yahoo.com");
    }
    
    // Constructor for testing with custom base URL
    public YFinanceApiClientImpl(RestTemplate restTemplate, ObjectMapper objectMapper, String baseUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        
        // Rate limiting: 100 requests per minute
        Bandwidth limit = Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));
        this.rateLimitBucket = Bucket.builder()
            .addLimit(limit)
            .build();
    }
    
    @Override
    @Cacheable(value = "marketData", key = "#symbol")
    @Retryable(value = {ApiConnectionException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public MarketData getCurrentQuote(String symbol) {
        logger.debug("Getting current quote for symbol: {}", symbol);
        
        // Check cache first
        MarketData cachedData = getCachedQuote(symbol);
        if (cachedData != null) {
            return cachedData;
        }
        
        // Rate limiting
        if (!rateLimitBucket.tryConsume(1)) {
            logger.warn("Rate limit exceeded for yFinance API");
            throw new ApiConnectionException("Rate limit exceeded");
        }
        
        try {
            String url = baseUrl + "/v8/finance/chart/" + symbol;
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            MarketData marketData = parseCurrentQuote(response.getBody(), symbol);
            
            // Cache the result
            cacheQuote(symbol, marketData);
            
            return marketData;
            
        } catch (HttpClientErrorException e) {
            logger.error("HTTP error getting quote for {}: {} - {}", symbol, e.getStatusCode(), e.getResponseBodyAsString());
            throw new ApiConnectionException("Failed to get quote for " + symbol + ": " + e.getResponseBodyAsString(), e);
        } catch (ResourceAccessException e) {
            logger.error("Connection error getting quote for {}", symbol, e);
            throw new ApiConnectionException("Failed to connect to yFinance API", e);
        } catch (Exception e) {
            logger.error("Unexpected error getting quote for {}", symbol, e);
            throw new ApiConnectionException("Unexpected error getting quote for " + symbol, e);
        }
    }
    
    @Override
    @Cacheable(value = "historicalData", key = "#symbol + '_' + #from + '_' + #to")
    @Retryable(value = {ApiConnectionException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public List<HistoricalData> getHistoricalData(String symbol, LocalDate from, LocalDate to) {
        logger.debug("Getting historical data for symbol: {} from {} to {}", symbol, from, to);
        
        // Rate limiting
        if (!rateLimitBucket.tryConsume(1)) {
            logger.warn("Rate limit exceeded for yFinance API");
            throw new ApiConnectionException("Rate limit exceeded");
        }
        
        try {
            long fromTimestamp = from.atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC);
            long toTimestamp = to.atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC);
            
            String url = String.format("%s/v8/finance/chart/%s?period1=%d&period2=%d&interval=1d",
                baseUrl, symbol, fromTimestamp, toTimestamp);
            
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            return parseHistoricalData(response.getBody(), symbol);
            
        } catch (HttpClientErrorException e) {
            logger.error("HTTP error getting historical data for {}: {} - {}", symbol, e.getStatusCode(), e.getResponseBodyAsString());
            throw new ApiConnectionException("Failed to get historical data for " + symbol + ": " + e.getResponseBodyAsString(), e);
        } catch (ResourceAccessException e) {
            logger.error("Connection error getting historical data for {}", symbol, e);
            throw new ApiConnectionException("Failed to connect to yFinance API", e);
        } catch (Exception e) {
            logger.error("Unexpected error getting historical data for {}", symbol, e);
            throw new ApiConnectionException("Unexpected error getting historical data for " + symbol, e);
        }
    }
    
    @Override
    @Cacheable(value = "historicalData", key = "#symbol + '_' + #period")
    @Retryable(value = {ApiConnectionException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public List<HistoricalData> getHistoricalData(String symbol, String period) {
        logger.debug("Getting historical data for symbol: {} with period: {}", symbol, period);
        
        // Rate limiting
        if (!rateLimitBucket.tryConsume(1)) {
            logger.warn("Rate limit exceeded for yFinance API");
            throw new ApiConnectionException("Rate limit exceeded");
        }
        
        try {
            String url = String.format("%s/v8/finance/chart/%s?range=%s&interval=1d",
                baseUrl, symbol, period);
            
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            return parseHistoricalData(response.getBody(), symbol);
            
        } catch (HttpClientErrorException e) {
            logger.error("HTTP error getting historical data for {}: {} - {}", symbol, e.getStatusCode(), e.getResponseBodyAsString());
            throw new ApiConnectionException("Failed to get historical data for " + symbol + ": " + e.getResponseBodyAsString(), e);
        } catch (ResourceAccessException e) {
            logger.error("Connection error getting historical data for {}", symbol, e);
            throw new ApiConnectionException("Failed to connect to yFinance API", e);
        } catch (Exception e) {
            logger.error("Unexpected error getting historical data for {}", symbol, e);
            throw new ApiConnectionException("Unexpected error getting historical data for " + symbol, e);
        }
    }
    
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        return headers;
    }
    
    private MarketData getCachedQuote(String symbol) {
        Long timestamp = cacheTimestamps.get(symbol);
        if (timestamp != null && (System.currentTimeMillis() - timestamp) < CACHE_DURATION_MS) {
            return quoteCache.get(symbol);
        }
        return null;
    }
    
    private void cacheQuote(String symbol, MarketData marketData) {
        quoteCache.put(symbol, marketData);
        cacheTimestamps.put(symbol, System.currentTimeMillis());
    }
    
    private MarketData parseCurrentQuote(String responseBody, String symbol) {
        try {
            JsonNode json = objectMapper.readTree(responseBody);
            JsonNode chart = json.get("chart");
            
            if (chart == null || !chart.has("result") || chart.get("result").size() == 0) {
                throw new ApiConnectionException("Invalid response format from yFinance");
            }
            
            JsonNode result = chart.get("result").get(0);
            JsonNode meta = result.get("meta");
            JsonNode indicators = result.get("indicators");
            
            if (indicators == null || !indicators.has("quote") || indicators.get("quote").size() == 0) {
                throw new ApiConnectionException("No quote data available");
            }
            
            JsonNode quote = indicators.get("quote").get(0);
            JsonNode timestamps = result.get("timestamp");
            
            if (timestamps == null || timestamps.size() == 0) {
                throw new ApiConnectionException("No timestamp data available");
            }
            
            // Get the latest data point
            int lastIndex = timestamps.size() - 1;
            
            MarketData marketData = new MarketData();
            marketData.setSymbol(symbol);
            marketData.setTimestamp(LocalDateTime.now());
            
            // Get current price from meta (most recent)
            if (meta.has("regularMarketPrice")) {
                marketData.setPrice(new BigDecimal(meta.get("regularMarketPrice").asText()));
            }
            
            // Get OHLCV data from the latest point
            if (quote.has("open") && quote.get("open").get(lastIndex) != null && !quote.get("open").get(lastIndex).isNull()) {
                marketData.setOpen(new BigDecimal(quote.get("open").get(lastIndex).asText()));
            }
            
            if (quote.has("high") && quote.get("high").get(lastIndex) != null && !quote.get("high").get(lastIndex).isNull()) {
                marketData.setHigh(new BigDecimal(quote.get("high").get(lastIndex).asText()));
            }
            
            if (quote.has("low") && quote.get("low").get(lastIndex) != null && !quote.get("low").get(lastIndex).isNull()) {
                marketData.setLow(new BigDecimal(quote.get("low").get(lastIndex).asText()));
            }
            
            if (quote.has("close") && quote.get("close").get(lastIndex) != null && !quote.get("close").get(lastIndex).isNull()) {
                BigDecimal closePrice = new BigDecimal(quote.get("close").get(lastIndex).asText());
                if (marketData.getPrice() == null) {
                    marketData.setPrice(closePrice);
                }
            }
            
            if (quote.has("volume") && quote.get("volume").get(lastIndex) != null && !quote.get("volume").get(lastIndex).isNull()) {
                marketData.setVolume(new BigDecimal(quote.get("volume").get(lastIndex).asText()));
            }
            
            return marketData;
            
        } catch (Exception e) {
            logger.error("Error parsing current quote response", e);
            throw new ApiConnectionException("Failed to parse quote response", e);
        }
    }
    
    private List<HistoricalData> parseHistoricalData(String responseBody, String symbol) {
        try {
            JsonNode json = objectMapper.readTree(responseBody);
            JsonNode chart = json.get("chart");
            
            if (chart == null || !chart.has("result") || chart.get("result").size() == 0) {
                throw new ApiConnectionException("Invalid response format from yFinance");
            }
            
            JsonNode result = chart.get("result").get(0);
            JsonNode timestamps = result.get("timestamp");
            JsonNode indicators = result.get("indicators");
            
            if (timestamps == null || indicators == null || !indicators.has("quote")) {
                throw new ApiConnectionException("Missing data in yFinance response");
            }
            
            JsonNode quote = indicators.get("quote").get(0);
            JsonNode opens = quote.get("open");
            JsonNode highs = quote.get("high");
            JsonNode lows = quote.get("low");
            JsonNode closes = quote.get("close");
            JsonNode volumes = quote.get("volume");
            
            List<HistoricalData> historicalDataList = new ArrayList<>();
            
            for (int i = 0; i < timestamps.size(); i++) {
                long timestamp = timestamps.get(i).asLong();
                LocalDate date = LocalDate.ofEpochDay(timestamp / 86400);
                
                HistoricalData data = new HistoricalData();
                data.setSymbol(symbol);
                data.setDate(date);
                
                if (opens != null && !opens.get(i).isNull()) {
                    data.setOpen(new BigDecimal(opens.get(i).asText()));
                }
                
                if (highs != null && !highs.get(i).isNull()) {
                    data.setHigh(new BigDecimal(highs.get(i).asText()));
                }
                
                if (lows != null && !lows.get(i).isNull()) {
                    data.setLow(new BigDecimal(lows.get(i).asText()));
                }
                
                if (closes != null && !closes.get(i).isNull()) {
                    BigDecimal closePrice = new BigDecimal(closes.get(i).asText());
                    data.setClose(closePrice);
                    data.setAdjustedClose(closePrice); // Simplified - would need adjclose from response
                }
                
                if (volumes != null && !volumes.get(i).isNull()) {
                    data.setVolume(volumes.get(i).asLong());
                }
                
                // Only add data points with valid close prices
                if (data.getClose() != null) {
                    historicalDataList.add(data);
                }
            }
            
            return historicalDataList;
            
        } catch (Exception e) {
            logger.error("Error parsing historical data response", e);
            throw new ApiConnectionException("Failed to parse historical data response", e);
        }
    }
}