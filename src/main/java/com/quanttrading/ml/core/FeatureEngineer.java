package com.quanttrading.ml.core;

import com.quanttrading.model.MarketData;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Advanced feature engineering for ML models.
 * Extracts comprehensive technical, statistical, and market microstructure features.
 */
@Component
public class FeatureEngineer {
    
    private static final Logger logger = LoggerFactory.getLogger(FeatureEngineer.class);
    
    private final int maxHistorySize;
    private final Map<String, LinkedList<MarketData>> symbolHistory;
    
    public FeatureEngineer() {
        this.maxHistorySize = 200;
        this.symbolHistory = new HashMap<>();
    }
    
    /**
     * Add market data to the historical buffer for a symbol.
     */
    public void addMarketData(String symbol, MarketData data) {
        symbolHistory.computeIfAbsent(symbol, k -> new LinkedList<>()).add(data);
        
        LinkedList<MarketData> history = symbolHistory.get(symbol);
        if (history.size() > maxHistorySize) {
            history.removeFirst();
        }
    }
    
    /**
     * Extract comprehensive features for ML models.
     */
    public Map<String, Double> extractFeatures(String symbol, MarketData currentData) {
        Map<String, Double> features = new HashMap<>();
        
        LinkedList<MarketData> history = symbolHistory.get(symbol);
        if (history == null || history.size() < 20) {
            logger.debug("Insufficient history for feature extraction: {}", symbol);
            return features;
        }
        
        try {
            // Add current data to history for calculations
            List<MarketData> fullHistory = new ArrayList<>(history);
            fullHistory.add(currentData);
            
            // Extract different categories of features
            extractPriceFeatures(features, fullHistory);
            extractVolumeFeatures(features, fullHistory);
            extractTechnicalIndicators(features, fullHistory);
            extractStatisticalFeatures(features, fullHistory);
            extractVolatilityFeatures(features, fullHistory);
            extractMomentumFeatures(features, fullHistory);
            extractMarketMicrostructureFeatures(features, fullHistory);
            
        } catch (Exception e) {
            logger.error("Error extracting features for {}: {}", symbol, e.getMessage(), e);
        }
        
        return features;
    }
    
    /**
     * Extract price-based features.
     */
    private void extractPriceFeatures(Map<String, Double> features, List<MarketData> data) {
        if (data.size() < 2) return;
        
        MarketData current = data.get(data.size() - 1);
        double currentPrice = current.getClose().doubleValue();
        
        // Price returns
        MarketData previous = data.get(data.size() - 2);
        double prevPrice = previous.getClose().doubleValue();
        features.put("return_1d", (currentPrice - prevPrice) / prevPrice);
        
        // Moving averages and relative positions
        double[] periods = {5, 10, 20, 50};
        for (double period : periods) {
            if (data.size() >= period) {
                double sma = calculateSMA(data, (int) period);
                features.put("sma_" + (int) period, sma);
                features.put("price_vs_sma_" + (int) period, (currentPrice - sma) / sma);
            }
        }
        
        // Price position in recent range
        if (data.size() >= 20) {
            double[] range = getPriceRange(data, 20);
            if (range[1] > range[0]) {
                features.put("price_position_20d", (currentPrice - range[0]) / (range[1] - range[0]));
            }
        }
        
        // Distance from highs/lows
        if (data.size() >= 52) {
            double[] range52 = getPriceRange(data, 52);
            features.put("distance_from_52w_high", (range52[1] - currentPrice) / range52[1]);
            features.put("distance_from_52w_low", (currentPrice - range52[0]) / range52[0]);
        }
    }
    
    /**
     * Extract volume-based features.
     */
    private void extractVolumeFeatures(Map<String, Double> features, List<MarketData> data) {
        if (data.size() < 10) return;
        
        MarketData current = data.get(data.size() - 1);
        double currentVolume = current.getVolume().doubleValue();
        
        // Volume moving averages
        double avgVol10 = calculateVolumeAverage(data, 10);
        double avgVol20 = calculateVolumeAverage(data, 20);
        
        features.put("volume_ratio_10d", avgVol10 > 0 ? currentVolume / avgVol10 : 1.0);
        features.put("volume_ratio_20d", avgVol20 > 0 ? currentVolume / avgVol20 : 1.0);
        
        // Volume trend
        if (avgVol20 > 0) {
            features.put("volume_trend", (avgVol10 - avgVol20) / avgVol20);
        }
        
        // Price-volume relationship
        if (data.size() >= 2) {
            double priceChange = features.getOrDefault("return_1d", 0.0);
            features.put("price_volume_correlation", priceChange * Math.log(1 + currentVolume / avgVol20));
        }
    }
    
    /**
     * Extract technical indicators.
     */
    private void extractTechnicalIndicators(Map<String, Double> features, List<MarketData> data) {
        if (data.size() < 14) return;
        
        // RSI
        double rsi = calculateRSI(data, 14);
        features.put("rsi", rsi);
        features.put("rsi_overbought", rsi > 70 ? 1.0 : 0.0);
        features.put("rsi_oversold", rsi < 30 ? 1.0 : 0.0);
        
        // MACD
        if (data.size() >= 26) {
            double[] macd = calculateMACD(data);
            features.put("macd", macd[0]);
            features.put("macd_signal", macd[1]);
            features.put("macd_histogram", macd[2]);
            features.put("macd_bullish", macd[0] > macd[1] ? 1.0 : 0.0);
        }
        
        // Bollinger Bands
        if (data.size() >= 20) {
            double[] bb = calculateBollingerBands(data, 20, 2.0);
            double currentPrice = data.get(data.size() - 1).getClose().doubleValue();
            if (bb[1] > bb[0]) {
                features.put("bb_position", (currentPrice - bb[0]) / (bb[1] - bb[0]));
                features.put("bb_width", (bb[1] - bb[0]) / bb[2]);
            }
        }
        
        // Stochastic Oscillator
        if (data.size() >= 14) {
            double stoch = calculateStochastic(data, 14);
            features.put("stochastic", stoch);
            features.put("stoch_overbought", stoch > 80 ? 1.0 : 0.0);
            features.put("stoch_oversold", stoch < 20 ? 1.0 : 0.0);
        }
    }
    
    /**
     * Extract statistical features.
     */
    private void extractStatisticalFeatures(Map<String, Double> features, List<MarketData> data) {
        if (data.size() < 20) return;
        
        // Price statistics
        double[] prices = data.stream()
                .skip(Math.max(0, data.size() - 20))
                .mapToDouble(md -> md.getClose().doubleValue())
                .toArray();
        
        DescriptiveStatistics priceStats = new DescriptiveStatistics(prices);
        features.put("price_mean_20d", priceStats.getMean());
        features.put("price_std_20d", priceStats.getStandardDeviation());
        features.put("price_skewness_20d", priceStats.getSkewness());
        features.put("price_kurtosis_20d", priceStats.getKurtosis());
        
        // Return statistics
        double[] returns = calculateReturns(data, 20);
        if (returns.length > 0) {
            DescriptiveStatistics returnStats = new DescriptiveStatistics(returns);
            features.put("return_mean_20d", returnStats.getMean());
            features.put("return_std_20d", returnStats.getStandardDeviation());
            features.put("return_skewness_20d", returnStats.getSkewness());
            features.put("return_kurtosis_20d", returnStats.getKurtosis());
        }
    }
    
    /**
     * Extract volatility features.
     */
    private void extractVolatilityFeatures(Map<String, Double> features, List<MarketData> data) {
        if (data.size() < 10) return;
        
        // Historical volatility (different periods)
        int[] periods = {5, 10, 20};
        for (int period : periods) {
            if (data.size() >= period) {
                double vol = calculateVolatility(data, period);
                features.put("volatility_" + period + "d", vol);
            }
        }
        
        // Volatility ratio (short vs long term)
        if (data.size() >= 20) {
            double vol5 = calculateVolatility(data, 5);
            double vol20 = calculateVolatility(data, 20);
            features.put("volatility_ratio", vol20 > 0 ? vol5 / vol20 : 1.0);
        }
        
        // Intraday volatility
        MarketData current = data.get(data.size() - 1);
        double intradayVol = (current.getHigh().doubleValue() - current.getLow().doubleValue()) 
                           / current.getClose().doubleValue();
        features.put("intraday_volatility", intradayVol);
    }
    
    /**
     * Extract momentum features.
     */
    private void extractMomentumFeatures(Map<String, Double> features, List<MarketData> data) {
        if (data.size() < 10) return;
        
        double currentPrice = data.get(data.size() - 1).getClose().doubleValue();
        
        // Price momentum over different periods
        int[] periods = {3, 5, 10, 20};
        for (int period : periods) {
            if (data.size() > period) {
                double pastPrice = data.get(data.size() - 1 - period).getClose().doubleValue();
                features.put("momentum_" + period + "d", (currentPrice - pastPrice) / pastPrice);
            }
        }
        
        // Rate of change
        if (data.size() >= 10) {
            double roc = calculateRateOfChange(data, 10);
            features.put("rate_of_change_10d", roc);
        }
    }
    
    /**
     * Extract market microstructure features.
     */
    private void extractMarketMicrostructureFeatures(Map<String, Double> features, List<MarketData> data) {
        MarketData current = data.get(data.size() - 1);
        
        // OHLC relationships
        double open = current.getOpen().doubleValue();
        double high = current.getHigh().doubleValue();
        double low = current.getLow().doubleValue();
        double close = current.getClose().doubleValue();
        
        // Body and shadow analysis
        double bodySize = Math.abs(close - open);
        double totalRange = high - low;
        
        if (totalRange > 0) {
            features.put("body_ratio", bodySize / totalRange);
            features.put("upper_shadow_ratio", (high - Math.max(open, close)) / totalRange);
            features.put("lower_shadow_ratio", (Math.min(open, close) - low) / totalRange);
        }
        
        // Close position within the day's range
        if (totalRange > 0) {
            features.put("close_position_in_range", (close - low) / totalRange);
        }
        
        // Gap analysis
        if (data.size() >= 2) {
            double prevClose = data.get(data.size() - 2).getClose().doubleValue();
            features.put("gap", (open - prevClose) / prevClose);
            features.put("gap_filled", Math.abs(features.get("gap")) < 0.001 ? 1.0 : 0.0);
        }
    }
    
    // Helper calculation methods
    
    private double calculateSMA(List<MarketData> data, int period) {
        return data.stream()
                .skip(Math.max(0, data.size() - period))
                .mapToDouble(md -> md.getClose().doubleValue())
                .average()
                .orElse(0.0);
    }
    
    private double calculateVolumeAverage(List<MarketData> data, int period) {
        return data.stream()
                .skip(Math.max(0, data.size() - period))
                .mapToDouble(md -> md.getVolume().doubleValue())
                .average()
                .orElse(0.0);
    }
    
    private double[] getPriceRange(List<MarketData> data, int period) {
        double min = data.stream()
                .skip(Math.max(0, data.size() - period))
                .mapToDouble(md -> md.getLow().doubleValue())
                .min()
                .orElse(0.0);
        
        double max = data.stream()
                .skip(Math.max(0, data.size() - period))
                .mapToDouble(md -> md.getHigh().doubleValue())
                .max()
                .orElse(0.0);
        
        return new double[]{min, max};
    }
    
    private double calculateRSI(List<MarketData> data, int period) {
        if (data.size() < period + 1) return 50.0;
        
        double gainSum = 0.0;
        double lossSum = 0.0;
        
        for (int i = Math.max(1, data.size() - period); i < data.size(); i++) {
            double change = data.get(i).getClose().doubleValue() - data.get(i - 1).getClose().doubleValue();
            if (change > 0) {
                gainSum += change;
            } else {
                lossSum -= change;
            }
        }
        
        double avgGain = gainSum / period;
        double avgLoss = lossSum / period;
        
        if (avgLoss == 0) return 100.0;
        
        double rs = avgGain / avgLoss;
        return 100.0 - (100.0 / (1.0 + rs));
    }
    
    private double[] calculateMACD(List<MarketData> data) {
        if (data.size() < 26) return new double[]{0.0, 0.0, 0.0};
        
        double ema12 = calculateEMA(data, 12);
        double ema26 = calculateEMA(data, 26);
        double macd = ema12 - ema26;
        
        // Simplified signal line calculation
        double signal = calculateEMA(data, 9);
        double histogram = macd - signal;
        
        return new double[]{macd, signal, histogram};
    }
    
    private double calculateEMA(List<MarketData> data, int period) {
        if (data.size() < period) return 0.0;
        
        double multiplier = 2.0 / (period + 1);
        double ema = data.get(data.size() - period).getClose().doubleValue();
        
        for (int i = data.size() - period + 1; i < data.size(); i++) {
            ema = (data.get(i).getClose().doubleValue() * multiplier) + (ema * (1 - multiplier));
        }
        
        return ema;
    }
    
    private double[] calculateBollingerBands(List<MarketData> data, int period, double stdDev) {
        if (data.size() < period) return new double[]{0.0, 0.0, 0.0};
        
        double sma = calculateSMA(data, period);
        
        double[] prices = data.stream()
                .skip(Math.max(0, data.size() - period))
                .mapToDouble(md -> md.getClose().doubleValue())
                .toArray();
        
        DescriptiveStatistics stats = new DescriptiveStatistics(prices);
        double std = stats.getStandardDeviation();
        
        double lowerBand = sma - (stdDev * std);
        double upperBand = sma + (stdDev * std);
        
        return new double[]{lowerBand, upperBand, sma};
    }
    
    private double calculateStochastic(List<MarketData> data, int period) {
        if (data.size() < period) return 50.0;
        
        double[] range = getPriceRange(data, period);
        double currentClose = data.get(data.size() - 1).getClose().doubleValue();
        
        if (range[1] == range[0]) return 50.0;
        
        return ((currentClose - range[0]) / (range[1] - range[0])) * 100.0;
    }
    
    private double[] calculateReturns(List<MarketData> data, int period) {
        if (data.size() < Math.min(period, 2)) return new double[0];
        
        int start = Math.max(0, data.size() - period);
        double[] returns = new double[data.size() - start - 1];
        
        for (int i = start + 1; i < data.size(); i++) {
            double curr = data.get(i).getClose().doubleValue();
            double prev = data.get(i - 1).getClose().doubleValue();
            returns[i - start - 1] = (curr - prev) / prev;
        }
        
        return returns;
    }
    
    private double calculateVolatility(List<MarketData> data, int period) {
        double[] returns = calculateReturns(data, period);
        if (returns.length == 0) return 0.0;
        
        DescriptiveStatistics stats = new DescriptiveStatistics(returns);
        return stats.getStandardDeviation() * Math.sqrt(252); // Annualized
    }
    
    private double calculateRateOfChange(List<MarketData> data, int period) {
        if (data.size() <= period) return 0.0;
        
        double current = data.get(data.size() - 1).getClose().doubleValue();
        double past = data.get(data.size() - 1 - period).getClose().doubleValue();
        
        return (current - past) / past;
    }
    
    /**
     * Get the number of data points available for a symbol.
     */
    public int getHistorySize(String symbol) {
        LinkedList<MarketData> history = symbolHistory.get(symbol);
        return history != null ? history.size() : 0;
    }
    
    /**
     * Clear history for a symbol (useful for testing or reset).
     */
    public void clearHistory(String symbol) {
        symbolHistory.remove(symbol);
    }
}