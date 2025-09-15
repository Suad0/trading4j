package com.quanttrading.ml;

import com.quanttrading.model.MarketData;
import com.quanttrading.model.TradeType;
import com.quanttrading.strategy.AbstractTradingStrategy;
import com.quanttrading.strategy.StrategyConfig;
import com.quanttrading.strategy.TradingSignal;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Simplified ML-enhanced trading strategy that works with the current codebase.
 * Uses statistical analysis and simple machine learning concepts without heavy dependencies.
 */
@Component
public class SimplifiedMLStrategy extends AbstractTradingStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(SimplifiedMLStrategy.class);
    
    private final LinkedList<MarketData> historicalData = new LinkedList<>();
    private final int maxHistorySize = 200;
    private final int minDataForAnalysis = 50;
    
    // Performance tracking
    private int totalPredictions = 0;
    private int correctPredictions = 0;
    private final LinkedList<Double> recentReturns = new LinkedList<>();
    
    public SimplifiedMLStrategy() {
        super(createDefaultConfig());
        logger.info("Initialized Simplified ML Strategy");
    }
    
    @Override
    public String getName() {
        return "Simplified_ML_Strategy";
    }
    
    @Override
    protected List<TradingSignal> doAnalyze(MarketData marketData) {
        List<TradingSignal> signals = new ArrayList<>();
        
        try {
            // Add data to history
            addMarketData(marketData);
            
            // Need sufficient data for analysis
            if (historicalData.size() < minDataForAnalysis) {
                logger.debug("Insufficient historical data: {} < {}", historicalData.size(), minDataForAnalysis);
                return signals;
            }
            
            // Extract features and make prediction
            Map<String, Double> features = extractSimpleFeatures(marketData);
            MLPrediction prediction = makePrediction(marketData, features);
            
            if (prediction != null && prediction.getConfidence() >= config.getMinConfidence()) {
                TradingSignal signal = generateTradingSignal(marketData, prediction, features);
                if (signal != null) {
                    signals.add(signal);
                    totalPredictions++;
                    
                    logger.info("Generated ML signal: {} {} at {} (confidence: {:.2f})",
                               signal.getTradeType(), signal.getSymbol(), 
                               signal.getTargetPrice(), signal.getConfidence());
                }
            }
            
        } catch (Exception e) {
            logger.error("Error in simplified ML analysis for {}: {}", marketData.getSymbol(), e.getMessage(), e);
        }
        
        return signals;
    }
    
    /**
     * Add market data to historical buffer.
     */
    private void addMarketData(MarketData data) {
        historicalData.add(data);
        if (historicalData.size() > maxHistorySize) {
            historicalData.removeFirst();
        }
    }
    
    /**
     * Extract simple but effective features for ML analysis.
     */
    private Map<String, Double> extractSimpleFeatures(MarketData current) {
        Map<String, Double> features = new HashMap<>();
        
        if (historicalData.size() < 20) {
            return features;
        }
        
        try {
            List<MarketData> recent = new ArrayList<>(historicalData);
            
            // Price-based features
            double currentPrice = current.getClose().doubleValue();
            
            // Moving averages
            double sma5 = calculateSMA(recent, 5);
            double sma10 = calculateSMA(recent, 10);
            double sma20 = calculateSMA(recent, 20);
            
            features.put("price_vs_sma5", (currentPrice - sma5) / sma5);
            features.put("price_vs_sma10", (currentPrice - sma10) / sma10);
            features.put("price_vs_sma20", (currentPrice - sma20) / sma20);
            
            // Price momentum
            if (recent.size() >= 5) {
                double price5ago = recent.get(recent.size() - 5).getClose().doubleValue();
                features.put("momentum_5d", (currentPrice - price5ago) / price5ago);
            }
            
            // Volatility
            double[] returns = calculateReturns(recent, 20);
            if (returns.length > 0) {
                DescriptiveStatistics stats = new DescriptiveStatistics(returns);
                features.put("volatility", stats.getStandardDeviation());
                features.put("skewness", stats.getSkewness());
            }
            
            // Volume features
            double currentVolume = current.getVolume().doubleValue();
            double avgVolume = calculateVolumeAverage(recent, 20);
            features.put("volume_ratio", avgVolume > 0 ? currentVolume / avgVolume : 1.0);
            
            // Price position in recent range
            double[] priceRange = getPriceRange(recent, 20);
            if (priceRange[1] > priceRange[0]) {
                features.put("price_position", (currentPrice - priceRange[0]) / (priceRange[1] - priceRange[0]));
            }
            
            // Simple RSI calculation
            double rsi = calculateSimpleRSI(recent, 14);
            features.put("rsi", rsi);
            features.put("rsi_overbought", rsi > 70 ? 1.0 : 0.0);
            features.put("rsi_oversold", rsi < 30 ? 1.0 : 0.0);
            
        } catch (Exception e) {
            logger.error("Error extracting features: {}", e.getMessage(), e);
        }
        
        return features;
    }
    
    /**
     * Make a simple ML prediction based on extracted features.
     */
    private MLPrediction makePrediction(MarketData current, Map<String, Double> features) {
        try {
            if (features.isEmpty()) {
                return null;
            }
            
            // Simple rule-based ML simulation
            double bullishScore = 0.0;
            double bearishScore = 0.0;
            
            // Trend indicators
            Double priceVsSma20 = features.get("price_vs_sma20");
            if (priceVsSma20 != null) {
                if (priceVsSma20 > 0.02) bullishScore += 0.3;
                else if (priceVsSma20 < -0.02) bearishScore += 0.3;
            }
            
            // Momentum
            Double momentum = features.get("momentum_5d");
            if (momentum != null) {
                if (momentum > 0.01) bullishScore += 0.2;
                else if (momentum < -0.01) bearishScore += 0.2;
            }
            
            // RSI
            Double rsi = features.get("rsi");
            if (rsi != null) {
                if (rsi < 30) bullishScore += 0.2; // Oversold
                else if (rsi > 70) bearishScore += 0.2; // Overbought
            }
            
            // Volume confirmation
            Double volumeRatio = features.get("volume_ratio");
            if (volumeRatio != null && volumeRatio > 1.2) {
                bullishScore += 0.1;
            }
            
            // Price position
            Double pricePosition = features.get("price_position");
            if (pricePosition != null) {
                if (pricePosition > 0.8) bearishScore += 0.1; // Near high
                else if (pricePosition < 0.2) bullishScore += 0.1; // Near low
            }
            
            // Determine direction and confidence
            MLPrediction.Direction direction;
            double confidence;
            
            if (bullishScore > bearishScore && bullishScore > 0.3) {
                direction = MLPrediction.Direction.UP;
                confidence = Math.min(0.9, bullishScore);
            } else if (bearishScore > bullishScore && bearishScore > 0.3) {
                direction = MLPrediction.Direction.DOWN;
                confidence = Math.min(0.9, bearishScore);
            } else {
                direction = MLPrediction.Direction.SIDEWAYS;
                confidence = 0.5;
            }
            
            // Create prediction
            Map<String, Object> predictions = new HashMap<>();
            predictions.put("direction", direction);
            predictions.put("prob_up", bullishScore);
            predictions.put("prob_down", bearishScore);
            predictions.put("bullish_score", bullishScore);
            predictions.put("bearish_score", bearishScore);
            
            Map<String, Double> featureImportance = Map.of(
                    "price_vs_sma20", 0.3,
                    "momentum_5d", 0.2,
                    "rsi", 0.2,
                    "volume_ratio", 0.15,
                    "volatility", 0.15
            );
            
            return new MLPrediction(
                    current.getSymbol(),
                    MLPrediction.PredictionType.PRICE_DIRECTION,
                    confidence,
                    predictions,
                    "Simplified_ML_Model",
                    featureImportance
            );
            
        } catch (Exception e) {
            logger.error("Error making prediction: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Generate trading signal from ML prediction.
     */
    private TradingSignal generateTradingSignal(MarketData current, MLPrediction prediction, Map<String, Double> features) {
        try {
            MLPrediction.Direction direction = prediction.getPriceDirection();
            double confidence = prediction.getConfidence();
            
            if (direction == MLPrediction.Direction.SIDEWAYS || confidence < config.getMinConfidence()) {
                return null;
            }
            
            TradeType tradeType = direction == MLPrediction.Direction.UP ? TradeType.BUY : TradeType.SELL;
            BigDecimal currentPrice = current.getClose();
            
            // Calculate position size based on confidence
            BigDecimal baseSize = config.getMaxPositionSize().multiply(BigDecimal.valueOf(0.1)); // 10% of max
            BigDecimal positionSize = baseSize.multiply(BigDecimal.valueOf(confidence));
            
            // Calculate stop loss and take profit
            double volatility = features.getOrDefault("volatility", 0.02);
            double stopLossPercent = Math.max(0.01, volatility * 2); // 2x volatility or 1% minimum
            double takeProfitPercent = stopLossPercent * 2; // 2:1 reward/risk ratio
            
            BigDecimal stopLoss, takeProfit;
            if (tradeType == TradeType.BUY) {
                stopLoss = currentPrice.multiply(BigDecimal.valueOf(1.0 - stopLossPercent));
                takeProfit = currentPrice.multiply(BigDecimal.valueOf(1.0 + takeProfitPercent));
            } else {
                stopLoss = currentPrice.multiply(BigDecimal.valueOf(1.0 + stopLossPercent));
                takeProfit = currentPrice.multiply(BigDecimal.valueOf(1.0 - takeProfitPercent));
            }
            
            String reason = String.format("ML Analysis: %s direction (confidence: %.2f), " +
                                        "Bullish score: %.2f, Bearish score: %.2f, RSI: %.1f",
                                        direction, confidence,
                                        (Double) prediction.getPredictions().get("bullish_score"),
                                        (Double) prediction.getPredictions().get("bearish_score"),
                                        features.getOrDefault("rsi", 50.0));
            
            return TradingSignal.builder()
                    .symbol(current.getSymbol())
                    .tradeType(tradeType)
                    .quantity(positionSize.setScale(2, RoundingMode.HALF_UP))
                    .targetPrice(currentPrice)
                    .stopLoss(stopLoss.setScale(2, RoundingMode.HALF_UP))
                    .takeProfit(takeProfit.setScale(2, RoundingMode.HALF_UP))
                    .confidence(confidence)
                    .strategyName(getName())
                    .reason(reason)
                    .build();
                    
        } catch (Exception e) {
            logger.error("Error generating trading signal: {}", e.getMessage(), e);
            return null;
        }
    }
    
    // Helper calculation methods
    
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
    
    private double calculateSimpleRSI(List<MarketData> data, int period) {
        if (data.size() < period + 1) return 50.0;
        
        double gainSum = 0.0;
        double lossSum = 0.0;
        
        int start = Math.max(0, data.size() - period - 1);
        
        for (int i = start + 1; i < data.size(); i++) {
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
    
    /**
     * Get recent accuracy for performance tracking.
     */
    public double getRecentAccuracy() {
        return totalPredictions > 0 ? (double) correctPredictions / totalPredictions : 0.5;
    }
    
    /**
     * Check if strategy needs retraining (simplified).
     */
    public boolean needsRetraining() {
        return getRecentAccuracy() < 0.4 && totalPredictions > 20;
    }
    
    private static StrategyConfig createDefaultConfig() {
        return StrategyConfig.builder("Simplified_ML_Strategy")
                .maxPositionSize(BigDecimal.valueOf(10000))
                .riskPerTrade(BigDecimal.valueOf(0.02))
                .minConfidence(0.6)
                .enabled(true)
                .parameter("ml_enabled", true)
                .parameter("min_data_points", 50)
                .parameter("max_history", 200)
                .build();
    }
}