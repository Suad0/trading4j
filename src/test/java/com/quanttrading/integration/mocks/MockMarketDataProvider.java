package com.quanttrading.integration.mocks;

import com.quanttrading.model.MarketData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Mock market data provider for integration tests.
 * Provides various market data scenarios for testing strategies.
 */
@Component
public class MockMarketDataProvider {

    private final Random random = new Random(42); // Fixed seed for reproducible tests
    private final Map<String, List<MarketData>> historicalData = new HashMap<>();
    private final Map<String, BigDecimal> currentPrices = new HashMap<>();

    /**
     * Setup mock market data for common test symbols
     */
    public void setupMockData() {
        // Initialize current prices
        currentPrices.put("AAPL", new BigDecimal("150.00"));
        currentPrices.put("GOOGL", new BigDecimal("2000.00"));
        currentPrices.put("MSFT", new BigDecimal("300.00"));
        currentPrices.put("TSLA", new BigDecimal("800.00"));
        currentPrices.put("AMZN", new BigDecimal("3000.00"));

        // Generate historical data for each symbol
        for (String symbol : currentPrices.keySet()) {
            historicalData.put(symbol, generateHistoricalData(symbol, 100));
        }
    }

    /**
     * Create bullish market data (upward trend)
     */
    public MarketData createBullishMarketData(String symbol) {
        BigDecimal basePrice = getCurrentPrice(symbol);
        BigDecimal price = basePrice.multiply(new BigDecimal("1.02")); // 2% increase
        
        MarketData data = new MarketData();
        data.setSymbol(symbol);
        data.setPrice(price);
        data.setOpen(basePrice);
        data.setHigh(price.multiply(new BigDecimal("1.01")));
        data.setLow(basePrice.multiply(new BigDecimal("0.99")));
        data.setVolume(new BigDecimal(1000000 + random.nextInt(500000)));
        data.setTimestamp(LocalDateTime.now());
        
        updateCurrentPrice(symbol, price);
        return data;
    }

    /**
     * Create bearish market data (downward trend)
     */
    public MarketData createBearishMarketData(String symbol) {
        BigDecimal basePrice = getCurrentPrice(symbol);
        BigDecimal price = basePrice.multiply(new BigDecimal("0.98")); // 2% decrease
        
        MarketData data = new MarketData();
        data.setSymbol(symbol);
        data.setPrice(price);
        data.setOpen(basePrice);
        data.setHigh(basePrice.multiply(new BigDecimal("1.01")));
        data.setLow(price.multiply(new BigDecimal("0.99")));
        data.setVolume(new BigDecimal(1200000 + random.nextInt(600000)));
        data.setTimestamp(LocalDateTime.now());
        
        updateCurrentPrice(symbol, price);
        return data;
    }

    /**
     * Create neutral market data (sideways movement)
     */
    public MarketData createNeutralMarketData(String symbol) {
        BigDecimal basePrice = getCurrentPrice(symbol);
        BigDecimal price = basePrice.multiply(new BigDecimal("1.001")); // 0.1% change
        
        MarketData data = new MarketData();
        data.setSymbol(symbol);
        data.setPrice(price);
        data.setOpen(basePrice);
        data.setHigh(basePrice.multiply(new BigDecimal("1.005")));
        data.setLow(basePrice.multiply(new BigDecimal("0.995")));
        data.setVolume(new BigDecimal(800000 + random.nextInt(400000)));
        data.setTimestamp(LocalDateTime.now());
        
        updateCurrentPrice(symbol, price);
        return data;
    }

    /**
     * Create volatile market data (high price swings)
     */
    public MarketData createVolatileMarketData(String symbol) {
        BigDecimal basePrice = getCurrentPrice(symbol);
        double volatilityFactor = 0.05 + random.nextDouble() * 0.05; // 5-10% volatility
        double priceChange = (random.nextDouble() - 0.5) * 2 * volatilityFactor;
        BigDecimal price = basePrice.multiply(BigDecimal.ONE.add(new BigDecimal(priceChange)));
        
        MarketData data = new MarketData();
        data.setSymbol(symbol);
        data.setPrice(price);
        data.setOpen(basePrice);
        data.setHigh(basePrice.max(price).multiply(new BigDecimal("1.02")));
        data.setLow(basePrice.min(price).multiply(new BigDecimal("0.98")));
        data.setVolume(new BigDecimal(1500000 + random.nextInt(1000000)));
        data.setTimestamp(LocalDateTime.now());
        
        updateCurrentPrice(symbol, price);
        return data;
    }

    /**
     * Create random market data
     */
    public MarketData createRandomMarketData(String symbol) {
        double scenario = random.nextDouble();
        if (scenario < 0.3) {
            return createBullishMarketData(symbol);
        } else if (scenario < 0.6) {
            return createBearishMarketData(symbol);
        } else if (scenario < 0.9) {
            return createNeutralMarketData(symbol);
        } else {
            return createVolatileMarketData(symbol);
        }
    }

    /**
     * Create historical market data for backtesting
     */
    public List<MarketData> createHistoricalMarketData(String symbol, int days) {
        if (historicalData.containsKey(symbol) && historicalData.get(symbol).size() >= days) {
            return historicalData.get(symbol).subList(0, days);
        }
        
        List<MarketData> data = generateHistoricalData(symbol, days);
        historicalData.put(symbol, data);
        return data;
    }

    /**
     * Generate historical data with realistic price movements
     */
    private List<MarketData> generateHistoricalData(String symbol, int days) {
        List<MarketData> data = new ArrayList<>();
        BigDecimal basePrice = getCurrentPrice(symbol);
        BigDecimal currentPrice = basePrice;
        
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        
        for (int i = 0; i < days; i++) {
            LocalDateTime date = startDate.plusDays(i);
            
            // Generate realistic price movement
            double dailyReturn = generateDailyReturn();
            BigDecimal newPrice = currentPrice.multiply(BigDecimal.ONE.add(new BigDecimal(dailyReturn)));
            
            // Calculate OHLC
            BigDecimal open = currentPrice;
            BigDecimal close = newPrice;
            BigDecimal high = open.max(close).multiply(new BigDecimal(1 + random.nextDouble() * 0.02));
            BigDecimal low = open.min(close).multiply(new BigDecimal(1 - random.nextDouble() * 0.02));
            
            MarketData dayData = new MarketData();
            dayData.setSymbol(symbol);
            dayData.setPrice(close);
            dayData.setOpen(open);
            dayData.setHigh(high);
            dayData.setLow(low);
            dayData.setVolume(new BigDecimal(500000 + random.nextInt(1000000)));
            dayData.setTimestamp(date);
            
            data.add(dayData);
            currentPrice = newPrice;
        }
        
        return data;
    }

    /**
     * Generate realistic daily return using normal distribution
     */
    private double generateDailyReturn() {
        // Use Box-Muller transform to generate normal distribution
        double u1 = random.nextDouble();
        double u2 = random.nextDouble();
        double z = Math.sqrt(-2 * Math.log(u1)) * Math.cos(2 * Math.PI * u2);
        
        // Scale to realistic daily volatility (1% daily standard deviation)
        return z * 0.01;
    }

    /**
     * Create market data with specific moving average setup
     */
    public List<MarketData> createMovingAverageTestData(String symbol, boolean bullishCrossover) {
        List<MarketData> data = new ArrayList<>();
        BigDecimal basePrice = getCurrentPrice(symbol);
        
        // Create data that will trigger moving average crossover
        for (int i = 0; i < 25; i++) {
            BigDecimal price;
            if (bullishCrossover) {
                // Price increases towards the end to create bullish crossover
                price = basePrice.multiply(new BigDecimal(0.95 + (i * 0.004))); // Gradual increase
            } else {
                // Price decreases towards the end to create bearish crossover
                price = basePrice.multiply(new BigDecimal(1.05 - (i * 0.004))); // Gradual decrease
            }
            
            MarketData dayData = new MarketData();
            dayData.setSymbol(symbol);
            dayData.setPrice(price);
            dayData.setOpen(price.multiply(new BigDecimal("0.999")));
            dayData.setHigh(price.multiply(new BigDecimal("1.001")));
            dayData.setLow(price.multiply(new BigDecimal("0.998")));
            dayData.setVolume(new BigDecimal(1000000));
            dayData.setTimestamp(LocalDateTime.now().minusDays(25 - i));
            
            data.add(dayData);
        }
        
        return data;
    }

    /**
     * Create market data for gap up/down scenarios
     */
    public MarketData createGapMarketData(String symbol, boolean gapUp, double gapPercentage) {
        BigDecimal basePrice = getCurrentPrice(symbol);
        BigDecimal gapMultiplier = BigDecimal.ONE.add(new BigDecimal(gapUp ? gapPercentage : -gapPercentage));
        BigDecimal openPrice = basePrice.multiply(gapMultiplier);
        BigDecimal closePrice = openPrice.multiply(new BigDecimal(0.995 + random.nextDouble() * 0.01));
        
        MarketData data = new MarketData();
        data.setSymbol(symbol);
        data.setPrice(closePrice);
        data.setOpen(openPrice);
        data.setHigh(openPrice.max(closePrice).multiply(new BigDecimal("1.005")));
        data.setLow(openPrice.min(closePrice).multiply(new BigDecimal("0.995")));
        data.setVolume(new BigDecimal(2000000 + random.nextInt(1000000))); // Higher volume on gaps
        data.setTimestamp(LocalDateTime.now());
        
        updateCurrentPrice(symbol, closePrice);
        return data;
    }

    /**
     * Get current price for a symbol
     */
    private BigDecimal getCurrentPrice(String symbol) {
        return currentPrices.getOrDefault(symbol, new BigDecimal("100.00"));
    }

    /**
     * Update current price for a symbol
     */
    private void updateCurrentPrice(String symbol, BigDecimal price) {
        currentPrices.put(symbol, price.setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * Reset all prices to initial values
     */
    public void resetPrices() {
        setupMockData();
    }

    /**
     * Get historical data for a symbol
     */
    public List<MarketData> getHistoricalData(String symbol) {
        return historicalData.getOrDefault(symbol, new ArrayList<>());
    }

    /**
     * Create market data with specific price
     */
    public MarketData createMarketDataWithPrice(String symbol, BigDecimal price) {
        MarketData data = new MarketData();
        data.setSymbol(symbol);
        data.setPrice(price);
        data.setOpen(price.multiply(new BigDecimal("0.999")));
        data.setHigh(price.multiply(new BigDecimal("1.002")));
        data.setLow(price.multiply(new BigDecimal("0.998")));
        data.setVolume(new BigDecimal(1000000));
        data.setTimestamp(LocalDateTime.now());
        
        updateCurrentPrice(symbol, price);
        return data;
    }
}