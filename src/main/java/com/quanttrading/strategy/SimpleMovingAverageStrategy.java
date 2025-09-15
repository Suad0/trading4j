package com.quanttrading.strategy;

import com.quanttrading.model.MarketData;
import com.quanttrading.model.TradeType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Simple Moving Average (SMA) crossover trading strategy.
 * Generates buy signals when short SMA crosses above long SMA,
 * and sell signals when short SMA crosses below long SMA.
 */
@Component
public class SimpleMovingAverageStrategy extends AbstractTradingStrategy {
    
    private static final String SHORT_PERIOD_PARAM = "shortPeriod";
    private static final String LONG_PERIOD_PARAM = "longPeriod";
    private static final String QUANTITY_PARAM = "quantity";
    
    private final HistoricalMarketData historicalData;
    
    public SimpleMovingAverageStrategy() {
        this(createDefaultConfig(), new HistoricalMarketData());
    }
    
    public SimpleMovingAverageStrategy(StrategyConfig config, HistoricalMarketData historicalData) {
        super(config);
        this.historicalData = historicalData;
        validateConfiguration();
    }
    
    private static StrategyConfig createDefaultConfig() {
        return StrategyConfig.builder("SimpleMovingAverageStrategy")
                .parameter(SHORT_PERIOD_PARAM, 10)
                .parameter(LONG_PERIOD_PARAM, 20)
                .parameter(QUANTITY_PARAM, new BigDecimal("100"))
                .minConfidence(0.6)
                .build();
    }
    
    @Override
    public String getName() {
        return "SimpleMovingAverageStrategy";
    }
    
    @Override
    protected List<TradingSignal> doAnalyze(MarketData marketData) {
        // Add current market data to historical data
        historicalData.addMarketData(marketData);
        
        String symbol = marketData.getSymbol();
        int shortPeriod = getConfigParameter(SHORT_PERIOD_PARAM, Integer.class, 10);
        int longPeriod = getConfigParameter(LONG_PERIOD_PARAM, Integer.class, 20);
        
        // Check if we have enough historical data (need longPeriod + 1 for current and previous SMAs)
        if (!historicalData.hasEnoughData(symbol, longPeriod + 1)) {
            logger.debug("Not enough historical data for {}: need {}, have {}", 
                        symbol, longPeriod + 1, historicalData.getDataCount(symbol));
            return List.of();
        }
        
        // Calculate current SMAs (including the latest data point)
        BigDecimal currentShortSMA = calculateSMA(symbol, shortPeriod, 0);
        BigDecimal currentLongSMA = calculateSMA(symbol, longPeriod, 0);
        
        if (currentShortSMA == null || currentLongSMA == null) {
            logger.warn("Unable to calculate current SMAs for {}", symbol);
            return List.of();
        }
        
        // Calculate previous SMAs (excluding the latest data point)
        BigDecimal previousShortSMA = calculateSMA(symbol, shortPeriod, 1);
        BigDecimal previousLongSMA = calculateSMA(symbol, longPeriod, 1);
        
        if (previousShortSMA == null || previousLongSMA == null) {
            logger.debug("Not enough data to calculate previous SMAs for {}", symbol);
            return List.of();
        }
        

        
        // Detect crossover signals
        TradingSignal signal = detectCrossoverSignal(symbol, 
                currentShortSMA, currentLongSMA, 
                previousShortSMA, previousLongSMA, 
                marketData.getPrice());
        
        return signal != null ? List.of(signal) : List.of();
    }
    
    /**
     * Calculate Simple Moving Average for the last N periods.
     * @param symbol the symbol
     * @param periods number of periods
     * @return SMA value or null if insufficient data
     */
    private BigDecimal calculateSMA(String symbol, int periods) {
        return calculateSMA(symbol, periods, 0);
    }
    
    /**
     * Calculate Simple Moving Average for N periods, offset by a number of periods.
     * @param symbol the symbol
     * @param periods number of periods for SMA
     * @param offset number of periods to offset from the end (0 = most recent)
     * @return SMA value or null if insufficient data
     */
    private BigDecimal calculateSMA(String symbol, int periods, int offset) {
        List<BigDecimal> prices = historicalData.getPrices(symbol);
        
        if (prices.size() < periods + offset) {
            return null;
        }
        
        int endIndex = prices.size() - offset;
        int startIndex = endIndex - periods;
        
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = startIndex; i < endIndex; i++) {
            sum = sum.add(prices.get(i));
        }
        
        return sum.divide(BigDecimal.valueOf(periods), 4, RoundingMode.HALF_UP);
    }
    
    /**
     * Detect crossover signals based on current and previous SMA values.
     */
    private TradingSignal detectCrossoverSignal(String symbol, 
                                              BigDecimal currentShortSMA, BigDecimal currentLongSMA,
                                              BigDecimal previousShortSMA, BigDecimal previousLongSMA,
                                              BigDecimal currentPrice) {
        
        // Check for bullish crossover (short SMA crosses above long SMA)
        boolean previousShortBelowLong = previousShortSMA.compareTo(previousLongSMA) <= 0;
        boolean currentShortAboveLong = currentShortSMA.compareTo(currentLongSMA) > 0;
        boolean bullishCrossover = previousShortBelowLong && currentShortAboveLong;
        
        // Check for bearish crossover (short SMA crosses below long SMA)
        boolean previousShortAboveLong = previousShortSMA.compareTo(previousLongSMA) >= 0;
        boolean currentShortBelowLong = currentShortSMA.compareTo(currentLongSMA) < 0;
        boolean bearishCrossover = previousShortAboveLong && currentShortBelowLong;
        
        if (bullishCrossover) {
            logger.info("Bullish SMA crossover detected for {}: Short SMA {} > Long SMA {}", 
                       symbol, currentShortSMA, currentLongSMA);
            
            return createTradingSignal(symbol, TradeType.BUY, currentPrice, 
                                     "Bullish SMA crossover", calculateConfidence(currentShortSMA, currentLongSMA));
        } else if (bearishCrossover) {
            logger.info("Bearish SMA crossover detected for {}: Short SMA {} < Long SMA {}", 
                       symbol, currentShortSMA, currentLongSMA);
            
            return createTradingSignal(symbol, TradeType.SELL, currentPrice, 
                                     "Bearish SMA crossover", calculateConfidence(currentLongSMA, currentShortSMA));
        }
        
        return null;
    }
    
    /**
     * Create a trading signal with appropriate parameters.
     */
    private TradingSignal createTradingSignal(String symbol, TradeType tradeType, 
                                            BigDecimal currentPrice, String reason, double confidence) {
        BigDecimal quantity = getConfigParameter(QUANTITY_PARAM, BigDecimal.class, new BigDecimal("100"));
        
        return TradingSignal.builder()
                .symbol(symbol)
                .tradeType(tradeType)
                .quantity(quantity)
                .targetPrice(currentPrice)
                .strategyName(getName())
                .confidence(confidence)
                .reason(reason)
                .build();
    }
    
    /**
     * Calculate confidence based on the magnitude of SMA separation.
     * Higher separation indicates stronger signal.
     */
    private double calculateConfidence(BigDecimal strongerSMA, BigDecimal weakerSMA) {
        if (strongerSMA.compareTo(BigDecimal.ZERO) == 0) {
            return 0.5; // Default confidence
        }
        
        BigDecimal separation = strongerSMA.subtract(weakerSMA).abs();
        BigDecimal percentageSeparation = separation.divide(strongerSMA, 4, RoundingMode.HALF_UP);
        
        // Convert percentage separation to confidence (0.5 to 1.0)
        // 1% separation = 0.6 confidence, 5% separation = 1.0 confidence
        double confidence = 0.5 + Math.min(percentageSeparation.doubleValue() * 10, 0.5);
        
        return Math.min(confidence, 1.0);
    }
    
    /**
     * Validate strategy configuration parameters.
     */
    private void validateConfiguration() {
        validateRequiredParameters(SHORT_PERIOD_PARAM, LONG_PERIOD_PARAM, QUANTITY_PARAM);
        
        Integer shortPeriod = getConfigParameter(SHORT_PERIOD_PARAM, Integer.class, null);
        Integer longPeriod = getConfigParameter(LONG_PERIOD_PARAM, Integer.class, null);
        BigDecimal quantity = getConfigParameter(QUANTITY_PARAM, BigDecimal.class, null);
        
        if (shortPeriod == null || shortPeriod <= 0) {
            throw new IllegalArgumentException("Short period must be positive");
        }
        
        if (longPeriod == null || longPeriod <= 0) {
            throw new IllegalArgumentException("Long period must be positive");
        }
        
        if (shortPeriod >= longPeriod) {
            throw new IllegalArgumentException("Short period must be less than long period");
        }
        
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }
    
    @Override
    public void updateConfig(StrategyConfig config) {
        super.updateConfig(config);
        validateConfiguration();
    }
    
    /**
     * Get the historical market data container (for testing purposes).
     */
    public HistoricalMarketData getHistoricalData() {
        return historicalData;
    }
}