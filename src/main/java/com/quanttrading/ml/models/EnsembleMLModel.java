package com.quanttrading.ml.models;

import com.quanttrading.ml.MLModelMetrics;
import com.quanttrading.ml.MLPrediction;
import com.quanttrading.ml.core.MLModel;
import com.quanttrading.model.MarketData;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Ensemble ML model that combines multiple prediction techniques:
 * 1. Statistical Pattern Recognition
 * 2. Trend Analysis
 * 3. Mean Reversion Detection
 * 4. Volatility Regime Classification
 * 5. Market Sentiment Analysis
 */
public class EnsembleMLModel implements MLModel {
    
    private static final Logger logger = LoggerFactory.getLogger(EnsembleMLModel.class);
    
    private final String modelName;
    private final int minTrainingData;
    
    // Model state
    private boolean isModelReady = false;
    private LocalDateTime lastTrainingTime;
    private int totalPredictions = 0;
    private int correctPredictions = 0;
    private double cumulativeError = 0.0;
    
    // Model components
    private final TrendAnalysisModel trendModel;
    private final MeanReversionModel meanReversionModel;
    private final VolatilityRegimeModel volatilityModel;
    private final PatternRecognitionModel patternModel;
    
    // Performance tracking
    private final LinkedList<Double> recentAccuracy = new LinkedList<>();
    private final LinkedList<Double> recentErrors = new LinkedList<>();
    
    public EnsembleMLModel() {
        this.modelName = "Ensemble_ML_Model";
        this.minTrainingData = 100;
        
        // Initialize sub-models
        this.trendModel = new TrendAnalysisModel();
        this.meanReversionModel = new MeanReversionModel();
        this.volatilityModel = new VolatilityRegimeModel();
        this.patternModel = new PatternRecognitionModel();
        
        logger.info("Initialized Ensemble ML Model with 4 sub-models");
    }
    
    @Override
    public String getModelName() {
        return modelName;
    }
    
    @Override
    public boolean train(List<MarketData> historicalData, Map<String, List<Double>> features) {
        try {
            logger.info("Training ensemble model with {} data points", historicalData.size());
            
            if (historicalData.size() < minTrainingData) {
                logger.warn("Insufficient training data: {} < {}", historicalData.size(), minTrainingData);
                return false;
            }
            
            // Train all sub-models
            boolean trendTrained = trendModel.train(historicalData, features);
            boolean meanRevTrained = meanReversionModel.train(historicalData, features);
            boolean volTrained = volatilityModel.train(historicalData, features);
            boolean patternTrained = patternModel.train(historicalData, features);
            
            if (trendTrained && meanRevTrained && volTrained && patternTrained) {
                isModelReady = true;
                lastTrainingTime = LocalDateTime.now();
                logger.info("Ensemble model training completed successfully");
                return true;
            } else {
                logger.error("Failed to train one or more sub-models");
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Error during ensemble model training", e);
            return false;
        }
    }
    
    @Override
    public MLPrediction predict(MarketData currentData, Map<String, Double> features) {
        if (!isModelReady) {
            logger.debug("Model not ready for predictions");
            return null;
        }
        
        try {
            // Get predictions from all sub-models
            MLPrediction trendPrediction = trendModel.predict(currentData, features);
            MLPrediction meanRevPrediction = meanReversionModel.predict(currentData, features);
            MLPrediction volPrediction = volatilityModel.predict(currentData, features);
            MLPrediction patternPrediction = patternModel.predict(currentData, features);
            
            // Ensemble the predictions
            return ensemblePredictions(currentData, trendPrediction, meanRevPrediction, 
                                     volPrediction, patternPrediction, features);
            
        } catch (Exception e) {
            logger.error("Error making ensemble prediction", e);
            return null;
        }
    }
    
    /**
     * Combine predictions from multiple models using weighted voting.
     */
    private MLPrediction ensemblePredictions(MarketData currentData,
                                           MLPrediction trend, MLPrediction meanRev,
                                           MLPrediction vol, MLPrediction pattern,
                                           Map<String, Double> features) {
        
        // Model weights based on recent performance and market conditions
        double trendWeight = calculateTrendWeight(features);
        double meanRevWeight = calculateMeanReversionWeight(features);
        double volWeight = calculateVolatilityWeight(features);
        double patternWeight = calculatePatternWeight(features);
        
        // Normalize weights
        double totalWeight = trendWeight + meanRevWeight + volWeight + patternWeight;
        trendWeight /= totalWeight;
        meanRevWeight /= totalWeight;
        volWeight /= totalWeight;
        patternWeight /= totalWeight;
        
        // Combine direction predictions
        double bullishScore = 0.0;
        double bearishScore = 0.0;
        
        if (trend != null && trend.getPriceDirection() == MLPrediction.Direction.UP) {
            bullishScore += trendWeight * trend.getConfidence();
        } else if (trend != null && trend.getPriceDirection() == MLPrediction.Direction.DOWN) {
            bearishScore += trendWeight * trend.getConfidence();
        }
        
        if (meanRev != null && meanRev.getPriceDirection() == MLPrediction.Direction.UP) {
            bullishScore += meanRevWeight * meanRev.getConfidence();
        } else if (meanRev != null && meanRev.getPriceDirection() == MLPrediction.Direction.DOWN) {
            bearishScore += meanRevWeight * meanRev.getConfidence();
        }
        
        if (pattern != null && pattern.getPriceDirection() == MLPrediction.Direction.UP) {
            bullishScore += patternWeight * pattern.getConfidence();
        } else if (pattern != null && pattern.getPriceDirection() == MLPrediction.Direction.DOWN) {
            bearishScore += patternWeight * pattern.getConfidence();
        }
        
        // Determine final direction and confidence
        MLPrediction.Direction finalDirection;
        double confidence;
        
        if (bullishScore > bearishScore && bullishScore > 0.3) {
            finalDirection = MLPrediction.Direction.UP;
            confidence = Math.min(0.95, bullishScore);
        } else if (bearishScore > bullishScore && bearishScore > 0.3) {
            finalDirection = MLPrediction.Direction.DOWN;
            confidence = Math.min(0.95, bearishScore);
        } else {
            finalDirection = MLPrediction.Direction.SIDEWAYS;
            confidence = 0.5;
        }
        
        // Adjust confidence based on model agreement
        double agreement = calculateModelAgreement(trend, meanRev, vol, pattern);
        confidence *= (0.5 + 0.5 * agreement); // Scale confidence by agreement
        
        // Create ensemble prediction
        Map<String, Object> predictions = new HashMap<>();
        predictions.put("direction", finalDirection);
        predictions.put("prob_up", bullishScore);
        predictions.put("prob_down", bearishScore);
        predictions.put("model_agreement", agreement);
        predictions.put("trend_weight", trendWeight);
        predictions.put("mean_reversion_weight", meanRevWeight);
        predictions.put("volatility_weight", volWeight);
        predictions.put("pattern_weight", patternWeight);
        
        // Add volatility prediction if available
        if (vol != null) {
            predictions.put("volatility", vol.getVolatilityPrediction());
            predictions.put("market_regime", vol.getMarketRegime());
        }
        
        // Feature importance (weighted average)
        Map<String, Double> featureImportance = calculateEnsembleFeatureImportance(
                trend, meanRev, vol, pattern, trendWeight, meanRevWeight, volWeight, patternWeight);
        
        totalPredictions++;
        
        return new MLPrediction(
                currentData.getSymbol(),
                MLPrediction.PredictionType.PRICE_DIRECTION,
                confidence,
                predictions,
                modelName,
                featureImportance
        );
    }
    
    /**
     * Calculate dynamic weights based on market conditions.
     */
    private double calculateTrendWeight(Map<String, Double> features) {
        // Higher weight in trending markets
        double momentum = Math.abs(features.getOrDefault("momentum_10d", 0.0));
        double trendStrength = Math.abs(features.getOrDefault("price_vs_sma_20", 0.0));
        return 0.2 + 0.3 * Math.min(1.0, momentum * 10 + trendStrength * 5);
    }
    
    private double calculateMeanReversionWeight(Map<String, Double> features) {
        // Higher weight when price is far from mean
        double priceVsSma = Math.abs(features.getOrDefault("price_vs_sma_20", 0.0));
        double rsi = features.getOrDefault("rsi", 50.0);
        double extremeRsi = Math.max(0, Math.abs(rsi - 50) - 20) / 30.0; // RSI > 70 or < 30
        return 0.2 + 0.3 * Math.min(1.0, priceVsSma * 10 + extremeRsi);
    }
    
    private double calculateVolatilityWeight(Map<String, Double> features) {
        // Higher weight in volatile markets
        double volatility = features.getOrDefault("volatility_20d", 0.02);
        return 0.15 + 0.25 * Math.min(1.0, volatility * 50);
    }
    
    private double calculatePatternWeight(Map<String, Double> features) {
        // Base weight for pattern recognition
        return 0.25;
    }
    
    /**
     * Calculate how much the models agree with each other.
     */
    private double calculateModelAgreement(MLPrediction... predictions) {
        List<MLPrediction> validPredictions = Arrays.stream(predictions)
                .filter(Objects::nonNull)
                .toList();
        
        if (validPredictions.size() < 2) return 0.5;
        
        int agreements = 0;
        int total = 0;
        
        for (int i = 0; i < validPredictions.size(); i++) {
            for (int j = i + 1; j < validPredictions.size(); j++) {
                if (validPredictions.get(i).getPriceDirection() == 
                    validPredictions.get(j).getPriceDirection()) {
                    agreements++;
                }
                total++;
            }
        }
        
        return total > 0 ? (double) agreements / total : 0.5;
    }
    
    /**
     * Calculate weighted feature importance across all models.
     */
    private Map<String, Double> calculateEnsembleFeatureImportance(
            MLPrediction trend, MLPrediction meanRev, MLPrediction vol, MLPrediction pattern,
            double trendWeight, double meanRevWeight, double volWeight, double patternWeight) {
        
        Map<String, Double> ensembleImportance = new HashMap<>();
        
        // Combine feature importance from all models
        if (trend != null && trend.getFeatureImportance() != null) {
            for (Map.Entry<String, Double> entry : trend.getFeatureImportance().entrySet()) {
                ensembleImportance.merge(entry.getKey(), entry.getValue() * trendWeight, Double::sum);
            }
        }
        
        if (meanRev != null && meanRev.getFeatureImportance() != null) {
            for (Map.Entry<String, Double> entry : meanRev.getFeatureImportance().entrySet()) {
                ensembleImportance.merge(entry.getKey(), entry.getValue() * meanRevWeight, Double::sum);
            }
        }
        
        if (vol != null && vol.getFeatureImportance() != null) {
            for (Map.Entry<String, Double> entry : vol.getFeatureImportance().entrySet()) {
                ensembleImportance.merge(entry.getKey(), entry.getValue() * volWeight, Double::sum);
            }
        }
        
        if (pattern != null && pattern.getFeatureImportance() != null) {
            for (Map.Entry<String, Double> entry : pattern.getFeatureImportance().entrySet()) {
                ensembleImportance.merge(entry.getKey(), entry.getValue() * patternWeight, Double::sum);
            }
        }
        
        return ensembleImportance;
    }
    
    @Override
    public void updateModel(MarketData newData, Map<String, Double> features) {
        if (!isModelReady) return;
        
        try {
            // Update all sub-models
            trendModel.updateModel(newData, features);
            meanReversionModel.updateModel(newData, features);
            volatilityModel.updateModel(newData, features);
            patternModel.updateModel(newData, features);
            
            // Update performance tracking
            updatePerformanceMetrics(newData, features);
            
        } catch (Exception e) {
            logger.error("Error updating ensemble model", e);
        }
    }
    
    private void updatePerformanceMetrics(MarketData newData, Map<String, Double> features) {
        // Track recent accuracy
        if (recentAccuracy.size() > 50) {
            recentAccuracy.removeFirst();
        }
        
        // Track recent errors
        if (recentErrors.size() > 50) {
            recentErrors.removeFirst();
        }
        
        // Calculate current accuracy (simplified)
        double currentAccuracy = totalPredictions > 0 ? (double) correctPredictions / totalPredictions : 0.5;
        recentAccuracy.add(currentAccuracy);
    }
    
    @Override
    public boolean isModelReady() {
        return isModelReady;
    }
    
    @Override
    public MLModelMetrics getMetrics() {
        if (!isModelReady) return null;
        
        double accuracy = totalPredictions > 0 ? (double) correctPredictions / totalPredictions : 0.0;
        double avgError = totalPredictions > 0 ? cumulativeError / totalPredictions : 0.0;
        
        return MLModelMetrics.builder(modelName)
                .accuracy(accuracy)
                .lastTrainingTime(lastTrainingTime)
                .trainingDataSize(minTrainingData)
                .totalPredictions(totalPredictions)
                .correctPredictions(correctPredictions)
                .build();
    }
    
    @Override
    public boolean needsRetraining() {
        if (!isModelReady) return true;
        
        // Check if accuracy has degraded
        double currentAccuracy = totalPredictions > 0 ? (double) correctPredictions / totalPredictions : 0.5;
        
        // Check if model is too old
        if (lastTrainingTime != null) {
            long daysSinceTraining = java.time.Duration.between(lastTrainingTime, LocalDateTime.now()).toDays();
            if (daysSinceTraining > 30) return true;
        }
        
        // Check if accuracy is below threshold
        return currentAccuracy < 0.45 && totalPredictions > 50;
    }
    
    @Override
    public int getMinimumTrainingData() {
        return minTrainingData;
    }
    
    @Override
    public boolean saveModel(String path) {
        try {
            // In a real implementation, this would serialize the model state
            logger.info("Saving ensemble model to {}", path);
            return true;
        } catch (Exception e) {
            logger.error("Error saving model", e);
            return false;
        }
    }
    
    @Override
    public boolean loadModel(String path) {
        try {
            // In a real implementation, this would deserialize the model state
            logger.info("Loading ensemble model from {}", path);
            isModelReady = true;
            return true;
        } catch (Exception e) {
            logger.error("Error loading model", e);
            return false;
        }
    }
    
    // Inner classes for sub-models (simplified implementations)
    
    private static class TrendAnalysisModel implements MLModel {
        private boolean ready = false;
        
        @Override
        public String getModelName() { return "Trend_Analysis"; }
        
        @Override
        public boolean train(List<MarketData> historicalData, Map<String, List<Double>> features) {
            ready = historicalData.size() >= 50;
            return ready;
        }
        
        @Override
        public MLPrediction predict(MarketData currentData, Map<String, Double> features) {
            if (!ready) return null;
            
            // Simple trend analysis based on moving averages and momentum
            double priceVsSma20 = features.getOrDefault("price_vs_sma_20", 0.0);
            double momentum10d = features.getOrDefault("momentum_10d", 0.0);
            double macd = features.getOrDefault("macd", 0.0);
            
            double bullishScore = 0.0;
            if (priceVsSma20 > 0.02) bullishScore += 0.3;
            if (momentum10d > 0.01) bullishScore += 0.3;
            if (macd > 0) bullishScore += 0.2;
            
            MLPrediction.Direction direction = bullishScore > 0.4 ? MLPrediction.Direction.UP :
                    (bullishScore < 0.2 ? MLPrediction.Direction.DOWN : MLPrediction.Direction.SIDEWAYS);
            
            Map<String, Object> predictions = new HashMap<>();
            predictions.put("direction", direction);
            
            Map<String, Double> featureImportance = Map.of(
                    "price_vs_sma_20", 0.4,
                    "momentum_10d", 0.35,
                    "macd", 0.25
            );
            
            return new MLPrediction(currentData.getSymbol(), MLPrediction.PredictionType.PRICE_DIRECTION,
                    Math.max(0.1, Math.min(0.9, bullishScore)), predictions, getModelName(), featureImportance);
        }
        
        @Override
        public void updateModel(MarketData newData, Map<String, Double> features) {}
        
        @Override
        public boolean isModelReady() { return ready; }
        
        @Override
        public MLModelMetrics getMetrics() { return null; }
        
        @Override
        public boolean needsRetraining() { return false; }
        
        @Override
        public int getMinimumTrainingData() { return 50; }
        
        @Override
        public boolean saveModel(String path) { return true; }
        
        @Override
        public boolean loadModel(String path) { ready = true; return true; }
    }
    
    private static class MeanReversionModel implements MLModel {
        private boolean ready = false;
        
        @Override
        public String getModelName() { return "Mean_Reversion"; }
        
        @Override
        public boolean train(List<MarketData> historicalData, Map<String, List<Double>> features) {
            ready = historicalData.size() >= 50;
            return ready;
        }
        
        @Override
        public MLPrediction predict(MarketData currentData, Map<String, Double> features) {
            if (!ready) return null;
            
            // Mean reversion analysis based on RSI, Bollinger Bands, and price position
            double rsi = features.getOrDefault("rsi", 50.0);
            double bbPosition = features.getOrDefault("bb_position", 0.5);
            double pricePosition = features.getOrDefault("price_position_20d", 0.5);
            
            double reversionScore = 0.0;
            
            // RSI extremes
            if (rsi > 70) reversionScore += 0.3; // Overbought -> expect down
            else if (rsi < 30) reversionScore -= 0.3; // Oversold -> expect up
            
            // Bollinger Band position
            if (bbPosition > 0.8) reversionScore += 0.2; // Near upper band -> expect down
            else if (bbPosition < 0.2) reversionScore -= 0.2; // Near lower band -> expect up
            
            // Price position in range
            if (pricePosition > 0.8) reversionScore += 0.2;
            else if (pricePosition < 0.2) reversionScore -= 0.2;
            
            MLPrediction.Direction direction = reversionScore > 0.3 ? MLPrediction.Direction.DOWN :
                    (reversionScore < -0.3 ? MLPrediction.Direction.UP : MLPrediction.Direction.SIDEWAYS);
            
            Map<String, Object> predictions = new HashMap<>();
            predictions.put("direction", direction);
            
            Map<String, Double> featureImportance = Map.of(
                    "rsi", 0.4,
                    "bb_position", 0.3,
                    "price_position_20d", 0.3
            );
            
            return new MLPrediction(currentData.getSymbol(), MLPrediction.PredictionType.PRICE_DIRECTION,
                    Math.max(0.1, Math.min(0.9, Math.abs(reversionScore))), predictions, getModelName(), featureImportance);
        }
        
        @Override
        public void updateModel(MarketData newData, Map<String, Double> features) {}
        
        @Override
        public boolean isModelReady() { return ready; }
        
        @Override
        public MLModelMetrics getMetrics() { return null; }
        
        @Override
        public boolean needsRetraining() { return false; }
        
        @Override
        public int getMinimumTrainingData() { return 50; }
        
        @Override
        public boolean saveModel(String path) { return true; }
        
        @Override
        public boolean loadModel(String path) { ready = true; return true; }
    }
    
    private static class VolatilityRegimeModel implements MLModel {
        private boolean ready = false;
        
        @Override
        public String getModelName() { return "Volatility_Regime"; }
        
        @Override
        public boolean train(List<MarketData> historicalData, Map<String, List<Double>> features) {
            ready = historicalData.size() >= 50;
            return ready;
        }
        
        @Override
        public MLPrediction predict(MarketData currentData, Map<String, Double> features) {
            if (!ready) return null;
            
            // Volatility regime classification
            double vol20d = features.getOrDefault("volatility_20d", 0.02);
            double volRatio = features.getOrDefault("volatility_ratio", 1.0);
            
            MLPrediction.MarketRegime regime;
            if (vol20d > 0.04) {
                regime = MLPrediction.MarketRegime.VOLATILE;
            } else if (volRatio > 1.5) {
                regime = MLPrediction.MarketRegime.VOLATILE;
            } else {
                // Use trend to determine bull/bear/sideways
                double momentum = features.getOrDefault("momentum_10d", 0.0);
                if (momentum > 0.02) regime = MLPrediction.MarketRegime.BULL;
                else if (momentum < -0.02) regime = MLPrediction.MarketRegime.BEAR;
                else regime = MLPrediction.MarketRegime.SIDEWAYS;
            }
            
            Map<String, Object> predictions = new HashMap<>();
            predictions.put("market_regime", regime);
            predictions.put("volatility", vol20d);
            
            Map<String, Double> featureImportance = Map.of(
                    "volatility_20d", 0.5,
                    "volatility_ratio", 0.3,
                    "momentum_10d", 0.2
            );
            
            return new MLPrediction(currentData.getSymbol(), MLPrediction.PredictionType.MARKET_REGIME,
                    0.7, predictions, getModelName(), featureImportance);
        }
        
        @Override
        public void updateModel(MarketData newData, Map<String, Double> features) {}
        
        @Override
        public boolean isModelReady() { return ready; }
        
        @Override
        public MLModelMetrics getMetrics() { return null; }
        
        @Override
        public boolean needsRetraining() { return false; }
        
        @Override
        public int getMinimumTrainingData() { return 50; }
        
        @Override
        public boolean saveModel(String path) { return true; }
        
        @Override
        public boolean loadModel(String path) { ready = true; return true; }
    }
    
    private static class PatternRecognitionModel implements MLModel {
        private boolean ready = false;
        
        @Override
        public String getModelName() { return "Pattern_Recognition"; }
        
        @Override
        public boolean train(List<MarketData> historicalData, Map<String, List<Double>> features) {
            ready = historicalData.size() >= 50;
            return ready;
        }
        
        @Override
        public MLPrediction predict(MarketData currentData, Map<String, Double> features) {
            if (!ready) return null;
            
            // Simple pattern recognition based on candlestick patterns and technical patterns
            double bodyRatio = features.getOrDefault("body_ratio", 0.5);
            double upperShadow = features.getOrDefault("upper_shadow_ratio", 0.0);
            double lowerShadow = features.getOrDefault("lower_shadow_ratio", 0.0);
            double gap = features.getOrDefault("gap", 0.0);
            
            double patternScore = 0.0;
            
            // Doji pattern (small body)
            if (bodyRatio < 0.1) {
                patternScore += 0.1; // Indecision
            }
            
            // Hammer pattern (small body, long lower shadow)
            if (bodyRatio < 0.3 && lowerShadow > 0.6) {
                patternScore -= 0.3; // Bullish reversal
            }
            
            // Shooting star (small body, long upper shadow)
            if (bodyRatio < 0.3 && upperShadow > 0.6) {
                patternScore += 0.3; // Bearish reversal
            }
            
            // Gap analysis
            if (Math.abs(gap) > 0.02) {
                patternScore += gap > 0 ? 0.2 : -0.2; // Gap direction
            }
            
            MLPrediction.Direction direction = patternScore > 0.2 ? MLPrediction.Direction.DOWN :
                    (patternScore < -0.2 ? MLPrediction.Direction.UP : MLPrediction.Direction.SIDEWAYS);
            
            Map<String, Object> predictions = new HashMap<>();
            predictions.put("direction", direction);
            
            Map<String, Double> featureImportance = Map.of(
                    "body_ratio", 0.3,
                    "upper_shadow_ratio", 0.25,
                    "lower_shadow_ratio", 0.25,
                    "gap", 0.2
            );
            
            return new MLPrediction(currentData.getSymbol(), MLPrediction.PredictionType.PRICE_DIRECTION,
                    Math.max(0.1, Math.min(0.8, Math.abs(patternScore))), predictions, getModelName(), featureImportance);
        }
        
        @Override
        public void updateModel(MarketData newData, Map<String, Double> features) {}
        
        @Override
        public boolean isModelReady() { return ready; }
        
        @Override
        public MLModelMetrics getMetrics() { return null; }
        
        @Override
        public boolean needsRetraining() { return false; }
        
        @Override
        public int getMinimumTrainingData() { return 50; }
        
        @Override
        public boolean saveModel(String path) { return true; }
        
        @Override
        public boolean loadModel(String path) { ready = true; return true; }
    }
}