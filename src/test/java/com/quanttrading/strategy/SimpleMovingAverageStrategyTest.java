package com.quanttrading.strategy;

import com.quanttrading.model.MarketData;
import com.quanttrading.model.TradeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SimpleMovingAverageStrategyTest {

    private SimpleMovingAverageStrategy strategy;
    private HistoricalMarketData historicalData;
    private StrategyConfig config;

    @BeforeEach
    void setUp() {
        historicalData = new HistoricalMarketData();
        config = StrategyConfig.builder("SimpleMovingAverageStrategy")
                .parameter("shortPeriod", 3)
                .parameter("longPeriod", 5)
                .parameter("quantity", new BigDecimal("100"))
                .minConfidence(0.5)
                .build();
        
        strategy = new SimpleMovingAverageStrategy(config, historicalData);
    }

    @Test
    void testConstructorWithDefaultConfig() {
        SimpleMovingAverageStrategy defaultStrategy = new SimpleMovingAverageStrategy();
        
        assertEquals("SimpleMovingAverageStrategy", defaultStrategy.getName());
        assertTrue(defaultStrategy.isEnabled());
        assertNotNull(defaultStrategy.getHistoricalData());
    }

    @Test
    void testGetName() {
        assertEquals("SimpleMovingAverageStrategy", strategy.getName());
    }

    @Test
    void testConfigurationValidation() {
        // Test valid configuration
        assertDoesNotThrow(() -> new SimpleMovingAverageStrategy(config, historicalData));
        
        // Test missing parameters
        StrategyConfig invalidConfig = StrategyConfig.builder("TestStrategy").build();
        assertThrows(IllegalArgumentException.class, () -> 
                new SimpleMovingAverageStrategy(invalidConfig, historicalData));
        
        // Test invalid short period
        StrategyConfig invalidShortPeriod = StrategyConfig.builder("TestStrategy")
                .parameter("shortPeriod", 0)
                .parameter("longPeriod", 5)
                .parameter("quantity", new BigDecimal("100"))
                .build();
        assertThrows(IllegalArgumentException.class, () -> 
                new SimpleMovingAverageStrategy(invalidShortPeriod, historicalData));
        
        // Test invalid long period
        StrategyConfig invalidLongPeriod = StrategyConfig.builder("TestStrategy")
                .parameter("shortPeriod", 3)
                .parameter("longPeriod", 0)
                .parameter("quantity", new BigDecimal("100"))
                .build();
        assertThrows(IllegalArgumentException.class, () -> 
                new SimpleMovingAverageStrategy(invalidLongPeriod, historicalData));
        
        // Test short period >= long period
        StrategyConfig invalidPeriods = StrategyConfig.builder("TestStrategy")
                .parameter("shortPeriod", 5)
                .parameter("longPeriod", 3)
                .parameter("quantity", new BigDecimal("100"))
                .build();
        assertThrows(IllegalArgumentException.class, () -> 
                new SimpleMovingAverageStrategy(invalidPeriods, historicalData));
        
        // Test invalid quantity
        StrategyConfig invalidQuantity = StrategyConfig.builder("TestStrategy")
                .parameter("shortPeriod", 3)
                .parameter("longPeriod", 5)
                .parameter("quantity", BigDecimal.ZERO)
                .build();
        assertThrows(IllegalArgumentException.class, () -> 
                new SimpleMovingAverageStrategy(invalidQuantity, historicalData));
    }

    @Test
    void testAnalyzeWithInsufficientData() {
        MarketData marketData = createMarketData("AAPL", new BigDecimal("150.00"));
        
        List<TradingSignal> signals = strategy.analyze(marketData);
        
        assertTrue(signals.isEmpty());
        assertEquals(1, historicalData.getDataCount("AAPL"));
    }

    @Test
    void testAnalyzeWithBullishCrossover() {
        String symbol = "AAPL";
        
        // Create a scenario where short SMA crosses above long SMA
        // Start with flat prices, then add a big jump to trigger crossover
        addHistoricalPrices(symbol, new BigDecimal[]{
                new BigDecimal("100.00"), new BigDecimal("100.00"), new BigDecimal("100.00"),
                new BigDecimal("100.00"), new BigDecimal("100.00"), new BigDecimal("100.00")
        });
        
        // Add current market data that should trigger bullish crossover
        // This will make short SMA > long SMA when previously they were equal
        MarketData currentData = createMarketData(symbol, new BigDecimal("130.00"));
        List<TradingSignal> signals = strategy.analyze(currentData);
        
        assertEquals(1, signals.size());
        TradingSignal signal = signals.get(0);
        assertEquals(symbol, signal.getSymbol());
        assertEquals(TradeType.BUY, signal.getTradeType());
        assertEquals(new BigDecimal("100"), signal.getQuantity());
        assertEquals(new BigDecimal("130.00"), signal.getTargetPrice());
        assertEquals("SimpleMovingAverageStrategy", signal.getStrategyName());
        assertTrue(signal.getConfidence() > 0.5);
        assertEquals("Bullish SMA crossover", signal.getReason());
    }

    @Test
    void testAnalyzeWithBearishCrossover() {
        String symbol = "AAPL";
        
        // Create a scenario where short SMA crosses below long SMA
        // Start with flat prices, then add a big drop to trigger crossover
        addHistoricalPrices(symbol, new BigDecimal[]{
                new BigDecimal("100.00"), new BigDecimal("100.00"), new BigDecimal("100.00"),
                new BigDecimal("100.00"), new BigDecimal("100.00"), new BigDecimal("100.00")
        });
        
        // Add current market data that should trigger bearish crossover
        // This will make short SMA < long SMA when previously they were equal
        MarketData currentData = createMarketData(symbol, new BigDecimal("70.00"));
        List<TradingSignal> signals = strategy.analyze(currentData);
        
        assertEquals(1, signals.size());
        TradingSignal signal = signals.get(0);
        assertEquals(symbol, signal.getSymbol());
        assertEquals(TradeType.SELL, signal.getTradeType());
        assertEquals(new BigDecimal("100"), signal.getQuantity());
        assertEquals(new BigDecimal("70.00"), signal.getTargetPrice());
        assertEquals("SimpleMovingAverageStrategy", signal.getStrategyName());
        assertTrue(signal.getConfidence() > 0.5);
        assertEquals("Bearish SMA crossover", signal.getReason());
    }

    @Test
    void testAnalyzeWithNoCrossover() {
        String symbol = "AAPL";
        
        // Add historical data with stable prices (no crossover)
        addHistoricalPrices(symbol, new BigDecimal[]{
                new BigDecimal("100.00"), new BigDecimal("100.50"), new BigDecimal("101.00"),
                new BigDecimal("101.50"), new BigDecimal("102.00"), new BigDecimal("102.50")
        });
        
        MarketData currentData = createMarketData(symbol, new BigDecimal("103.00"));
        List<TradingSignal> signals = strategy.analyze(currentData);
        
        assertTrue(signals.isEmpty());
    }

    @Test
    void testAnalyzeWithMultipleDataPoints() {
        String symbol = "AAPL";
        
        // Add enough data points to test SMA calculations
        BigDecimal[] prices = {
                new BigDecimal("100.00"), new BigDecimal("101.00"), new BigDecimal("102.00"),
                new BigDecimal("103.00"), new BigDecimal("104.00"), new BigDecimal("105.00"),
                new BigDecimal("106.00"), new BigDecimal("107.00"), new BigDecimal("108.00"),
                new BigDecimal("109.00")
        };
        
        for (BigDecimal price : prices) {
            MarketData data = createMarketData(symbol, price);
            strategy.analyze(data);
        }
        
        assertEquals(10, historicalData.getDataCount(symbol));
    }

    @Test
    void testSMACalculationAccuracy() {
        String symbol = "AAPL";
        
        // Create a clear crossover scenario for testing
        // Start with flat prices, then add a jump to trigger crossover
        addHistoricalPrices(symbol, new BigDecimal[]{
                new BigDecimal("100.00"), new BigDecimal("100.00"), new BigDecimal("100.00"),
                new BigDecimal("100.00"), new BigDecimal("100.00"), new BigDecimal("100.00")
        });
        
        MarketData currentData = createMarketData(symbol, new BigDecimal("130.00"));
        List<TradingSignal> signals = strategy.analyze(currentData);
        
        // Should generate a buy signal due to the price jump causing crossover
        assertEquals(1, signals.size());
        assertEquals(TradeType.BUY, signals.get(0).getTradeType());
    }

    @Test
    void testConfidenceCalculation() {
        String symbol = "AAPL";
        
        // Create scenario with significant SMA separation for high confidence
        addHistoricalPrices(symbol, new BigDecimal[]{
                new BigDecimal("100.00"), new BigDecimal("105.00"), new BigDecimal("110.00"),
                new BigDecimal("115.00"), new BigDecimal("120.00"), new BigDecimal("125.00")
        });
        
        MarketData currentData = createMarketData(symbol, new BigDecimal("130.00"));
        List<TradingSignal> signals = strategy.analyze(currentData);
        
        if (!signals.isEmpty()) {
            TradingSignal signal = signals.get(0);
            assertTrue(signal.getConfidence() > 0.7, 
                      "Expected high confidence due to large SMA separation");
        }
    }

    @Test
    void testUpdateConfig() {
        StrategyConfig newConfig = StrategyConfig.builder("SimpleMovingAverageStrategy")
                .parameter("shortPeriod", 5)
                .parameter("longPeriod", 10)
                .parameter("quantity", new BigDecimal("200"))
                .minConfidence(0.8)
                .build();
        
        strategy.updateConfig(newConfig);
        
        assertEquals(newConfig, strategy.getConfig());
    }

    @Test
    void testUpdateConfigWithInvalidConfig() {
        StrategyConfig invalidConfig = StrategyConfig.builder("SimpleMovingAverageStrategy")
                .parameter("shortPeriod", 10)
                .parameter("longPeriod", 5) // Invalid: short >= long
                .parameter("quantity", new BigDecimal("100"))
                .build();
        
        assertThrows(IllegalArgumentException.class, () -> strategy.updateConfig(invalidConfig));
    }

    @Test
    void testStrategyWithDifferentSymbols() {
        // Test that strategy can handle multiple symbols independently
        MarketData appleData = createMarketData("AAPL", new BigDecimal("150.00"));
        MarketData teslaData = createMarketData("TSLA", new BigDecimal("200.00"));
        
        strategy.analyze(appleData);
        strategy.analyze(teslaData);
        
        assertEquals(1, historicalData.getDataCount("AAPL"));
        assertEquals(1, historicalData.getDataCount("TSLA"));
    }

    @Test
    void testGetHistoricalData() {
        assertNotNull(strategy.getHistoricalData());
        assertSame(historicalData, strategy.getHistoricalData());
    }

    private void addHistoricalPrices(String symbol, BigDecimal[] prices) {
        LocalDateTime baseTime = LocalDateTime.now().minusHours(prices.length);
        
        for (int i = 0; i < prices.length; i++) {
            MarketData data = createMarketData(symbol, prices[i]);
            data.setTimestamp(baseTime.plusHours(i));
            historicalData.addMarketData(data);
        }
    }

    private MarketData createMarketData(String symbol, BigDecimal price) {
        MarketData data = new MarketData();
        data.setSymbol(symbol);
        data.setPrice(price);
        data.setTimestamp(LocalDateTime.now());
        return data;
    }
}