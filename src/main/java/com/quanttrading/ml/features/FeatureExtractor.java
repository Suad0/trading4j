package com.quanttrading.ml.features;

import com.quanttrading.model.MarketData;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Extracts technical and statistical features from market data for ML models.
 */
public class FeatureExtractor {
    
    private final int lookbackPeriod;
    private final LinkedList<MarketData> historicalData;
    
    public FeatureExtractor(int lookbackPeriod) {
        this.lookbackPeriod = lookbackPeriod;
        this.historicalData = new LinkedList<>();
    }
    
    /**
     * Add new market data point to the historical buffer.
     */
    public void addData(MarketData data) {
        historicalData.add(data);
        // Keep only the required lookback period
        if (historicalData.size() > lookbackPeriod * 2) {
            historicalData.removeFirst();
        }
    }
    
    /**
     * Extract comprehensive features from current market data and history.
     */
    public Map<String, Double> extractFeatures(MarketData currentData) {
        Map<String, Double> features = new HashMap<>();
        
        if (historicalData.size() < lookbackPeriod) {
            return features; // Not enough data for feature extraction
        }
        
        // Get recent data for calculations
        List<MarketData> recentData = getRecentData(lookbackPeriod);
        
        // Price-based features
        extractPriceFeatures(features, currentData, recentData);
        
        // Volume-based features
        extractVolumeFeatures(features, currentData, recentData);
        
        // Technical indicators
        extractTechnicalIndicators(features, recentData);
        
        // Statistical features
        extractStatisticalFeatures(features, recentData);
        
        // Market microstructure features
        extractMicrostructureFeatures(features, currentData, recentData);
        
        // Volatility features
        extractVolatilityFeatures(features, recentData);
        
        return features;
    }
    
    private void extractPriceFeatures(Map<String, Double> features, MarketData current, List<MarketData> recent) {
        if (recent.isEmpty()) return;
        
        double currentPrice = current.getClose().doubleValue();
        double prevPrice = recent.get(recent.size() - 2).getClose().doubleValue();
        
        // Price returns
        features.put("return_1d", (currentPrice - prevPrice) / prevPrice);
        
        // Price relative to moving averages
        double sma5 = calculateSMA(recent, 5);
        double sma10 = calculateSMA(recent, 10);
        double sma20 = calculateSMA(recent, 20);
        
        features.put("price_vs_sma5", (currentPrice - sma5) / sma5);
        features.put("price_vs_sma10", (currentPrice - sma10) / sma10);
        features.put("price_vs_sma20", (currentPrice - sma20) / sma20);
        
        // Price position in range
        double[] range = getPriceRange(recent, 20);
        features.put("price_position", (currentPrice - range[0]) / (range[1] - range[0]));
        
        // Gap analysis
        if (recent.size() >= 2) {
            double prevClose = recent.get(recent.size() - 2).getClose().doubleValue();
            double currentOpen = current.getOpen().doubleValue();
            features.put("gap", (currentOpen - prevClose) / prevClose);
        }
    }
    
    private void extractVolumeFeatures(Map<String, Double> features, MarketData current, List<MarketData> recent) {
        if (recent.isEmpty()) return;
        
        double currentVolume = current.getVolume().doubleValue();
        
        // Volume relative to average
        double avgVolume = calculateVolumeAverage(recent, 20);
        features.put("volume_ratio", currentVolume / avgVolume);
        
        // Volume trend
        double avgVolume5 = calculateVolumeAverage(recent, 5);
        double avgVolume20 = calculateVolumeAverage(recent, 20);
        features.put("volume_trend", (avgVolume5 - avgVolume20) / avgVolume20);
        
        // Price-volume relationship
        if (recent.size() >= 2) {
            double priceChange = (current.getClose().doubleValue() - recent.get(recent.size() - 2).getClose().doubleValue());
            features.put("price_volume_corr", priceChange * currentVolume);
        }
    }
    
    private void extractTechnicalIndicators(Map<String, Double> features, List<MarketData> recent) {
        if (recent.size() < 14) return;
        
        // RSI
        double rsi = calculateRSI(recent, 14);
        features.put("rsi", rsi);
        features.put("rsi_overbought", rsi > 70 ? 1.0 : 0.0);
        features.put("rsi_oversold", rsi < 30 ? 1.0 : 0.0);
        
        // MACD
        double[] macd = calculateMACD(recent);
        features.put("macd", macd[0]);
        features.put("macd_signal", macd[1]);
        features.put("macd_histogram", macd[2]);
        features.put("macd_bullish", macd[0] > macd[1] ? 1.0 : 0.0);
        
        // Bollinger Bands
        double[] bb = calculateBollingerBands(recent, 20, 2.0);
        double currentPrice = recent.get(recent.size() - 1).getClose().doubleValue();
        features.put("bb_position", (currentPrice - bb[0]) / (bb[1] - bb[0])); // Position within bands
        features.put("bb_squeeze", (bb[1] - bb[0]) / bb[2]); // Band width relative to SMA
    }
    
    private void extractStatisticalFeatures(Map<String, Double> features, List<MarketData> recent) {
        if (recent.size() < 10) return;
        
        // Price statistics
        double[] prices = recent.stream().mapToDouble(md -> md.getClose().doubleValue()).toArray();
        DescriptiveStatistics stats = new DescriptiveStatistics(prices);
        
        features.put("price_mean", stats.getMean());
        features.put("price_std", stats.getStandardDeviation());
        features.put("price_skewness", stats.getSkewness());
        features.put("price_kurtosis", stats.getKurtosis());
        
        // Returns statistics
        double[] returns = calculateReturns(recent);
        DescriptiveStatistics returnStats = new DescriptiveStatistics(returns);
        
        features.put("return_mean", returnStats.getMean());
        features.put("return_std", returnStats.getStandardDeviation());
        features.put("return_skewness", returnStats.getSkewness());
        features.put("return_kurtosis", returnStats.getKurtosis());
    }
    
    private void extractMicrostructureFeatures(Map<String, Double> features, MarketData current, List<MarketData> recent) {
        // Bid-ask spread (simplified - using high/low as proxy)
        double spread = current.getHigh().subtract(current.getLow()).doubleValue();
        double midPrice = current.getHigh().add(current.getLow()).divide(BigDecimal.valueOf(2), RoundingMode.HALF_UP).doubleValue();
        if (midPrice > 0) {
            features.put("bid_ask_spread", spread / midPrice);
        }
        
        // Intraday price range
        double high = current.getHigh().doubleValue();
        double low = current.getLow().doubleValue();
        double close = current.getClose().doubleValue();
        
        features.put("intraday_range", (high - low) / close);
        features.put("close_position", (close - low) / (high - low));
        
        // Body vs shadow analysis
        double open = current.getOpen().doubleValue();
        double bodySize = Math.abs(close - open);
        double totalRange = high - low;
        
        features.put("body_ratio", totalRange > 0 ? bodySize / totalRange : 0);
        features.put("upper_shadow", (high - Math.max(open, close)) / close);
        features.put("lower_shadow", (Math.min(open, close) - low) / close);
    }
    
    private void extractVolatilityFeatures(Map<String, Double> features, List<MarketData> recent) {
        if (recent.size() < 20) return;
        
        // Historical volatility
        double[] returns = calculateReturns(recent);
        DescriptiveStatistics stats = new DescriptiveStatistics(returns);
        double volatility = stats.getStandardDeviation() * Math.sqrt(252); // Annualized
        
        features.put("historical_volatility", volatility);
        
        // Volatility regimes
        double[] vol10 = calculateVolatility(recent, 10);
        double[] vol20 = calculateVolatility(recent, 20);
        
        features.put("vol_10d", vol10[vol10.length - 1]);
        features.put("vol_20d", vol20[vol20.length - 1]);
        features.put("vol_ratio", vol10[vol10.length - 1] / vol20[vol20.length - 1]);
        
        // Volatility clustering
        features.put("vol_clustering", calculateVolatilityClustering(returns));
    }
    
    // Helper methods for calculations
    private double calculateSMA(List<MarketData> data, int period) {
        if (data.size() < period) return 0.0;
        
        return data.stream()
                .skip(Math.max(0, data.size() - period))
                .mapToDouble(md -> md.getClose().doubleValue())
                .average()
                .orElse(0.0);
    }
    
    private double calculateVolumeAverage(List<MarketData> data, int period) {
        if (data.size() < period) return 0.0;
        
        return data.stream()
                .skip(Math.max(0, data.size() - period))
                .mapToDouble(md -> md.getVolume().doubleValue())
                .average()
                .orElse(0.0);
    }
    
    private double[] getPriceRange(List<MarketData> data, int period) {
        if (data.size() < period) return new double[]{0.0, 0.0};
        
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
        
        // For simplicity, using SMA for signal line (typically EMA)
        double signal = calculateSMA(data, 9);
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
    
    private double[] calculateReturns(List<MarketData> data) {
        if (data.size() < 2) return new double[0];
        
        double[] returns = new double[data.size() - 1];
        for (int i = 1; i < data.size(); i++) {
            double curr = data.get(i).getClose().doubleValue();
            double prev = data.get(i - 1).getClose().doubleValue();
            returns[i - 1] = (curr - prev) / prev;
        }
        return returns;
    }
    
    private double[] calculateVolatility(List<MarketData> data, int period) {
        double[] returns = calculateReturns(data);
        if (returns.length < period) return new double[0];
        
        double[] volatilities = new double[returns.length - period + 1];
        for (int i = period - 1; i < returns.length; i++) {
            double[] subset = Arrays.copyOfRange(returns, i - period + 1, i + 1);
            DescriptiveStatistics stats = new DescriptiveStatistics(subset);
            volatilities[i - period + 1] = stats.getStandardDeviation();
        }
        return volatilities;
    }
    
    private double calculateVolatilityClustering(double[] returns) {
        if (returns.length < 10) return 0.0;
        
        // Simple measure: correlation between squared returns and lagged squared returns
        double[] squaredReturns = Arrays.stream(returns).map(r -> r * r).toArray();
        
        double sum1 = 0.0, sum2 = 0.0, sum12 = 0.0, sum1Sq = 0.0, sum2Sq = 0.0;
        int n = squaredReturns.length - 1;
        
        for (int i = 0; i < n; i++) {
            double x = squaredReturns[i];
            double y = squaredReturns[i + 1];
            
            sum1 += x;
            sum2 += y;
            sum12 += x * y;
            sum1Sq += x * x;
            sum2Sq += y * y;
        }
        
        double correlation = (n * sum12 - sum1 * sum2) / 
                Math.sqrt((n * sum1Sq - sum1 * sum1) * (n * sum2Sq - sum2 * sum2));
        
        return Double.isNaN(correlation) ? 0.0 : correlation;
    }
    
    private List<MarketData> getRecentData(int count) {
        int size = historicalData.size();
        int start = Math.max(0, size - count);
        return new ArrayList<>(historicalData.subList(start, size));
    }
}