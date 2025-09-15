package com.quanttrading.ml;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents ML model predictions for trading decisions.
 */
public class MLPrediction {
    
    public enum PredictionType {
        PRICE_DIRECTION,    // Up/Down/Sideways
        PRICE_TARGET,       // Specific price prediction
        VOLATILITY,         // Expected volatility
        MARKET_REGIME,      // Bull/Bear/Sideways market
        RISK_LEVEL          // Low/Medium/High risk
    }
    
    public enum Direction {
        UP, DOWN, SIDEWAYS
    }
    
    public enum MarketRegime {
        BULL, BEAR, SIDEWAYS, VOLATILE
    }
    
    public enum RiskLevel {
        LOW, MEDIUM, HIGH, EXTREME
    }
    
    private final String symbol;
    private final LocalDateTime timestamp;
    private final PredictionType type;
    private final double confidence;
    private final Map<String, Object> predictions;
    private final String modelName;
    private final Map<String, Double> featureImportance;
    
    public MLPrediction(String symbol, PredictionType type, double confidence, 
                       Map<String, Object> predictions, String modelName,
                       Map<String, Double> featureImportance) {
        this.symbol = symbol;
        this.timestamp = LocalDateTime.now();
        this.type = type;
        this.confidence = confidence;
        this.predictions = predictions;
        this.modelName = modelName;
        this.featureImportance = featureImportance;
    }
    
    // Getters
    public String getSymbol() { return symbol; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public PredictionType getType() { return type; }
    public double getConfidence() { return confidence; }
    public Map<String, Object> getPredictions() { return predictions; }
    public String getModelName() { return modelName; }
    public Map<String, Double> getFeatureImportance() { return featureImportance; }
    
    // Convenience methods for specific prediction types
    public Direction getPriceDirection() {
        return (Direction) predictions.get("direction");
    }
    
    public BigDecimal getPriceTarget() {
        return (BigDecimal) predictions.get("price_target");
    }
    
    public Double getVolatilityPrediction() {
        return (Double) predictions.get("volatility");
    }
    
    public MarketRegime getMarketRegime() {
        return (MarketRegime) predictions.get("market_regime");
    }
    
    public RiskLevel getRiskLevel() {
        return (RiskLevel) predictions.get("risk_level");
    }
    
    public Double getProbabilityUp() {
        return (Double) predictions.get("prob_up");
    }
    
    public Double getProbabilityDown() {
        return (Double) predictions.get("prob_down");
    }
    
    @Override
    public String toString() {
        return "MLPrediction{" +
                "symbol='" + symbol + '\'' +
                ", timestamp=" + timestamp +
                ", type=" + type +
                ", confidence=" + confidence +
                ", modelName='" + modelName + '\'' +
                ", predictions=" + predictions +
                '}';
    }
}