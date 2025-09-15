package com.quanttrading.strategy;

import com.quanttrading.ml.MLModelMetrics;
import com.quanttrading.ml.MLPrediction;
import com.quanttrading.ml.core.FeatureEngineer;
import com.quanttrading.ml.models.EnsembleMLModel;
import com.quanttrading.model.MarketData;
import com.quanttrading.model.TradeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Advanced ML-enhanced trading strategy that integrates seamlessly with the existing framework.
 * Uses ensemble ML models with comprehensive feature engineering for intelligent trading decisions.
 */
@Component
public class MLEnhancedTradingStrategy extends AbstractTradingStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(MLEnhancedTradingStrategy.class);
    
    // ML Components
    private final FeatureEngineer featureEngineer;
    private final EnsembleMLModel mlModel;
    
    // Strategy state
    private boolean isModelTrained = false;
    private int dataPointsProcessed = 0;
    private final int minDataForTraining = 100;
    
    // Performance tracking
    private int totalSignals = 0;
    private int successfulSignals = 0;
    private final LinkedList<Double> recentPerformance = new LinkedList<>();
    
    public MLEnhancedTradingStrategy() {
        super(createDefaultConfig());
        this.featureEngineer = new FeatureEngineer();
        this.mlModel = new EnsembleMLModel();
        
        logger.info("Initialized ML-Enhanced Trading Strategy");
    }
    
    @Override
    public String getName() {
        return "ML_Enhanced_Trading_Strategy";
    }
    
    @Override
    protected List<TradingSignal> doAnalyze(MarketData marketData) {
        List<TradingSignal> signals = new ArrayList<>();
        
        try {
            String symbol = marketData.getSymbol();
            
            // Add market data to feature engineer
            featureEngineer.addMarketData(symbol, marketData);
            dataPointsProcessed++;
            
            // Check if we have enough data for ML analysis
            if (featureEngineer.getHistorySize(symbol) < 20) {
                logger.debug("Insufficient data for ML analysis: {} data points", 
                           featureEngineer.getHistorySize(symbol));
                return signals;
            }
            
            // Extract features
            Map<String, Double> features = featureEngineer.extractFeatures(symbol, marketData);
            if (features.isEmpty()) {
                logger.debug("No features extracted for {}", symbol);
                return signals;
            }
            
            // Train model if not trained and we have enough data
            if (!isModelTrained && dataPointsProcessed >= minDataForTraining) {
                trainModel(symbol);
            }
            
            // Make ML prediction if model is ready
            if (mlModel.isModelReady()) {
                MLPrediction prediction = mlModel.predict(marketData, features);
                
                if (prediction != null && prediction.getConfidence() >= config.getMinConfidence()) {
                    TradingSignal signal = generateTradingSignal(marketData, prediction, features);
                    if (signal != null) {
                        signals.add(signal);
                        totalSignals++;
                        
                        logger.info("Generated ML trading signal: {} {} {} at {} (confidence: {:.2f})",
                                   signal.getTradeType(), signal.getQuantity(), symbol, 
                                   signal.getTargetPrice(), signal.getConfidence());
                    }
                }
                
                // Update model with new data
                mlModel.updateModel(marketData, features);
            }
            
        } catch (Exception e) {
            logger.error("Error in ML analysis for {}: {}", marketData.getSymbol(), e.getMessage(), e);
        }
        
        return signals;
    }
    
    /**
     * Train the ML model with available historical data.
     */
    public boolean trainModel(String symbol) {
        try {
            logger.info("Training ML model for symbol: {}", symbol);
            
            // For now, we'll use a simplified training approach
            // In a real implementation, you would fetch historical data from your repository
            List<MarketData> historicalData = new ArrayList<>(); // Would be populated from database
            Map<String, List<Double>> features = new HashMap<>(); // Would be extracted from historical data
            
            boolean success = mlModel.train(historicalData, features);
            if (success) {
                isModelTrained = true;
                logger.info("ML model training completed successfully for {}", symbol);
            } else {
                logger.warn("ML model training failed for {}", symbol);
            }
            
            return success;
            
        } catch (Exception e) {
            logger.error("Error training ML model for {}: {}", symbol, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Generate trading signal from ML prediction.
     */
    private TradingSignal generateTradingSignal(MarketData marketData, MLPrediction prediction, 
                                              Map<String, Double> features) {
        try {
            String symbol = marketData.getSymbol();
            MLPrediction.Direction direction = prediction.getPriceDirection();
            double confidence = prediction.getConfidence();
            
            // Skip sideways predictions or low confidence
            if (direction == MLPrediction.Direction.SIDEWAYS || confidence < config.getMinConfidence()) {
                return null;
            }
            
            // Determine trade type
            TradeType tradeType = direction == MLPrediction.Direction.UP ? TradeType.BUY : TradeType.SELL;
            
            // Calculate position size based on confidence and risk management
            BigDecimal positionSize = calculatePositionSize(confidence, features);
            
            // Calculate stop loss and take profit based on ML insights
            BigDecimal currentPrice = marketData.getClose();
            BigDecimal stopLoss = calculateStopLoss(currentPrice, tradeType, features, prediction);
            BigDecimal takeProfit = calculateTakeProfit(currentPrice, tradeType, features, prediction);
            
            // Build comprehensive reason
            String reason = buildSignalReason(prediction, features);
            
            return TradingSignal.builder()
                    .symbol(symbol)
                    .tradeType(tradeType)
                    .quantity(positionSize)
                    .targetPrice(currentPrice)
                    .stopLoss(stopLoss)
                    .takeProfit(takeProfit)
                    .confidence(confidence)
                    .strategyName(getName())
                    .reason(reason)
                    .build();
                    
        } catch (Exception e) {
            logger.error("Error generating trading signal: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Calculate position size based on ML confidence and risk factors.
     */
    private BigDecimal calculatePositionSize(double confidence, Map<String, Double> features) {
        // Base position size
        BigDecimal baseSize = config.getMaxPositionSize().multiply(BigDecimal.valueOf(0.1)); // 10% of max
        
        // Adjust based on confidence
        double confidenceMultiplier = Math.min(1.5, confidence * 1.5);
        
        // Adjust based on volatility
        double volatility = features.getOrDefault("volatility_20d", 0.02);
        double volatilityMultiplier = Math.max(0.3, 1.0 - (volatility * 10)); // Reduce size in high volatility
        
        // Adjust based on recent performance
        double performanceMultiplier = calculatePerformanceMultiplier();
        
        double finalMultiplier = confidenceMultiplier * volatilityMultiplier * performanceMultiplier;
        
        return baseSize.multiply(BigDecimal.valueOf(finalMultiplier))
                      .setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate stop loss based on ML insights and market conditions.
     */
    private BigDecimal calculateStopLoss(BigDecimal currentPrice, TradeType tradeType, 
                                       Map<String, Double> features, MLPrediction prediction) {
        
        // Base stop loss percentage
        double baseStopLoss = 0.02; // 2%
        
        // Adjust based on volatility
        double volatility = features.getOrDefault("volatility_20d", 0.02);
        double volatilityAdjustment = Math.max(1.0, volatility * 25); // Scale with volatility
        
        // Adjust based on market regime
        MLPrediction.MarketRegime regime = prediction.getMarketRegime();
        double regimeAdjustment = 1.0;
        if (regime == MLPrediction.MarketRegime.VOLATILE) {
            regimeAdjustment = 1.5; // Wider stops in volatile markets
        } else if (regime == MLPrediction.MarketRegime.SIDEWAYS) {
            regimeAdjustment = 0.7; // Tighter stops in sideways markets
        }
        
        double finalStopLoss = baseStopLoss * volatilityAdjustment * regimeAdjustment;
        finalStopLoss = Math.min(0.05, Math.max(0.005, finalStopLoss)); // Cap between 0.5% and 5%
        
        if (tradeType == TradeType.BUY) {
            return currentPrice.multiply(BigDecimal.valueOf(1.0 - finalStopLoss));
        } else {
            return currentPrice.multiply(BigDecimal.valueOf(1.0 + finalStopLoss));
        }
    }
    
    /**
     * Calculate take profit based on ML insights and risk-reward ratio.
     */
    private BigDecimal calculateTakeProfit(BigDecimal currentPrice, TradeType tradeType, 
                                         Map<String, Double> features, MLPrediction prediction) {
        
        // Use 2:1 risk-reward ratio as base
        BigDecimal stopLoss = calculateStopLoss(currentPrice, tradeType, features, prediction);
        double stopLossDistance = Math.abs(currentPrice.subtract(stopLoss).doubleValue());
        double takeProfitDistance = stopLossDistance * 2.0; // 2:1 ratio
        
        // Adjust based on ML confidence
        double confidence = prediction.getConfidence();
        double confidenceAdjustment = Math.max(1.0, confidence * 1.5);
        takeProfitDistance *= confidenceAdjustment;
        
        // Adjust based on market regime
        MLPrediction.MarketRegime regime = prediction.getMarketRegime();
        if (regime == MLPrediction.MarketRegime.BULL && tradeType == TradeType.BUY) {
            takeProfitDistance *= 1.3; // Larger targets in bull markets for long positions
        } else if (regime == MLPrediction.MarketRegime.BEAR && tradeType == TradeType.SELL) {
            takeProfitDistance *= 1.3; // Larger targets in bear markets for short positions
        }
        
        if (tradeType == TradeType.BUY) {
            return currentPrice.add(BigDecimal.valueOf(takeProfitDistance));
        } else {
            return currentPrice.subtract(BigDecimal.valueOf(takeProfitDistance));
        }
    }
    
    /**
     * Build detailed reason for the trading signal.
     */
    private String buildSignalReason(MLPrediction prediction, Map<String, Double> features) {
        StringBuilder reason = new StringBuilder();
        
        reason.append(String.format("ML Prediction: %s direction (confidence: %.2f%%), ",
                prediction.getPriceDirection(), prediction.getConfidence() * 100));
        
        reason.append(String.format("Model: %s, ", prediction.getModelName()));
        
        // Add market regime if available
        MLPrediction.MarketRegime regime = prediction.getMarketRegime();
        if (regime != null) {
            reason.append(String.format("Market regime: %s, ", regime));
        }
        
        // Add key technical indicators
        Double rsi = features.get("rsi");
        if (rsi != null) {
            reason.append(String.format("RSI: %.1f, ", rsi));
        }
        
        Double momentum = features.get("momentum_10d");
        if (momentum != null) {
            reason.append(String.format("10d momentum: %.2f%%, ", momentum * 100));
        }
        
        Double volatility = features.get("volatility_20d");
        if (volatility != null) {
            reason.append(String.format("Volatility: %.2f%%", volatility * 100));
        }
        
        return reason.toString();
    }
    
    /**
     * Calculate performance multiplier based on recent trading success.
     */
    private double calculatePerformanceMultiplier() {
        if (totalSignals == 0) return 1.0;
        
        double recentSuccessRate = (double) successfulSignals / totalSignals;
        
        // Scale position size based on recent performance
        if (recentSuccessRate > 0.6) {
            return 1.2; // Increase size if performing well
        } else if (recentSuccessRate < 0.4) {
            return 0.7; // Reduce size if performing poorly
        } else {
            return 1.0; // Normal size
        }
    }
    
    /**
     * Update performance tracking (would be called by the trading system).
     */
    public void updatePerformance(boolean signalWasSuccessful) {
        if (signalWasSuccessful) {
            successfulSignals++;
        }
        
        // Track recent performance
        recentPerformance.add(signalWasSuccessful ? 1.0 : 0.0);
        if (recentPerformance.size() > 50) {
            recentPerformance.removeFirst();
        }
    }
    
    /**
     * Get ML model metrics.
     */
    public MLModelMetrics getMLModelMetrics() {
        return mlModel.getMetrics();
    }
    
    /**
     * Check if the model needs retraining.
     */
    public boolean needsRetraining() {
        return mlModel.needsRetraining();
    }
    
    /**
     * Get current model confidence.
     */
    public double getModelConfidence() {
        if (!isModelTrained) return 0.0;
        
        double successRate = totalSignals > 0 ? (double) successfulSignals / totalSignals : 0.5;
        return Math.max(0.0, Math.min(1.0, successRate));
    }
    
    /**
     * Save the ML model.
     */
    public boolean saveModel(String modelPath) {
        return mlModel.saveModel(modelPath);
    }
    
    /**
     * Load the ML model.
     */
    public boolean loadModel(String modelPath) {
        boolean loaded = mlModel.loadModel(modelPath);
        if (loaded) {
            isModelTrained = true;
        }
        return loaded;
    }
    
    /**
     * Update the model with new market data.
     */
    public void updateModel(MarketData newData) {
        String symbol = newData.getSymbol();
        featureEngineer.addMarketData(symbol, newData);
        
        if (mlModel.isModelReady()) {
            Map<String, Double> features = featureEngineer.extractFeatures(symbol, newData);
            mlModel.updateModel(newData, features);
        }
    }
    
    /**
     * Make a prediction for given market data.
     */
    public MLPrediction predict(MarketData marketData) {
        if (!mlModel.isModelReady()) {
            return null;
        }
        
        String symbol = marketData.getSymbol();
        Map<String, Double> features = featureEngineer.extractFeatures(symbol, marketData);
        
        return mlModel.predict(marketData, features);
    }
    
    /**
     * Train the model with historical data.
     */
    public boolean trainModel(List<MarketData> historicalData) {
        try {
            // Extract features from historical data
            Map<String, List<Double>> features = new HashMap<>();
            
            for (MarketData data : historicalData) {
                featureEngineer.addMarketData(data.getSymbol(), data);
                Map<String, Double> currentFeatures = featureEngineer.extractFeatures(data.getSymbol(), data);
                
                for (Map.Entry<String, Double> entry : currentFeatures.entrySet()) {
                    features.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(entry.getValue());
                }
            }
            
            boolean success = mlModel.train(historicalData, features);
            if (success) {
                isModelTrained = true;
                dataPointsProcessed = historicalData.size();
            }
            
            return success;
            
        } catch (Exception e) {
            logger.error("Error training model with historical data", e);
            return false;
        }
    }
    
    private static StrategyConfig createDefaultConfig() {
        return StrategyConfig.builder("ML_Enhanced_Trading_Strategy")
                .maxPositionSize(BigDecimal.valueOf(10000))
                .riskPerTrade(BigDecimal.valueOf(0.02))
                .minConfidence(0.65) // Higher threshold for ML strategy
                .enabled(true)
                .parameter("ml_enabled", true)
                .parameter("min_training_data", 100)
                .parameter("confidence_threshold", 0.65)
                .parameter("max_position_percent", 0.1)
                .build();
    }
}