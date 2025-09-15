package com.quanttrading.strategy;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration class for trading strategies.
 */
public class StrategyConfig {
    
    private final String strategyName;
    private final Map<String, Object> parameters;
    private final BigDecimal maxPositionSize;
    private final BigDecimal riskPerTrade;
    private final double minConfidence;
    private final boolean enabled;
    
    private StrategyConfig(Builder builder) {
        this.strategyName = builder.strategyName;
        this.parameters = new HashMap<>(builder.parameters);
        this.maxPositionSize = builder.maxPositionSize;
        this.riskPerTrade = builder.riskPerTrade;
        this.minConfidence = builder.minConfidence;
        this.enabled = builder.enabled;
    }
    
    public String getStrategyName() {
        return strategyName;
    }
    
    public Map<String, Object> getParameters() {
        return new HashMap<>(parameters);
    }
    
    public <T> T getParameter(String key, Class<T> type) {
        Object value = parameters.get(key);
        if (value == null) {
            return null;
        }
        
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        
        throw new IllegalArgumentException("Parameter " + key + " is not of type " + type.getSimpleName());
    }
    
    public <T> T getParameter(String key, Class<T> type, T defaultValue) {
        T value = getParameter(key, type);
        return value != null ? value : defaultValue;
    }
    
    public BigDecimal getMaxPositionSize() {
        return maxPositionSize;
    }
    
    public BigDecimal getRiskPerTrade() {
        return riskPerTrade;
    }
    
    public double getMinConfidence() {
        return minConfidence;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StrategyConfig that = (StrategyConfig) o;
        return Double.compare(that.minConfidence, minConfidence) == 0 &&
                enabled == that.enabled &&
                Objects.equals(strategyName, that.strategyName) &&
                Objects.equals(parameters, that.parameters) &&
                Objects.equals(maxPositionSize, that.maxPositionSize) &&
                Objects.equals(riskPerTrade, that.riskPerTrade);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(strategyName, parameters, maxPositionSize, riskPerTrade, minConfidence, enabled);
    }
    
    @Override
    public String toString() {
        return "StrategyConfig{" +
                "strategyName='" + strategyName + '\'' +
                ", parameters=" + parameters +
                ", maxPositionSize=" + maxPositionSize +
                ", riskPerTrade=" + riskPerTrade +
                ", minConfidence=" + minConfidence +
                ", enabled=" + enabled +
                '}';
    }
    
    public static Builder builder(String strategyName) {
        return new Builder(strategyName);
    }
    
    public static class Builder {
        private final String strategyName;
        private final Map<String, Object> parameters = new HashMap<>();
        private BigDecimal maxPositionSize = new BigDecimal("1000");
        private BigDecimal riskPerTrade = new BigDecimal("0.02");
        private double minConfidence = 0.6;
        private boolean enabled = true;
        
        public Builder(String strategyName) {
            this.strategyName = Objects.requireNonNull(strategyName, "Strategy name is required");
        }
        
        public Builder parameter(String key, Object value) {
            this.parameters.put(key, value);
            return this;
        }
        
        public Builder parameters(Map<String, Object> parameters) {
            this.parameters.putAll(parameters);
            return this;
        }
        
        public Builder maxPositionSize(BigDecimal maxPositionSize) {
            this.maxPositionSize = maxPositionSize;
            return this;
        }
        
        public Builder riskPerTrade(BigDecimal riskPerTrade) {
            this.riskPerTrade = riskPerTrade;
            return this;
        }
        
        public Builder minConfidence(double minConfidence) {
            this.minConfidence = minConfidence;
            return this;
        }
        
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }
        
        public StrategyConfig build() {
            if (maxPositionSize != null && maxPositionSize.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Max position size must be positive");
            }
            
            if (riskPerTrade != null && (riskPerTrade.compareTo(BigDecimal.ZERO) <= 0 || 
                riskPerTrade.compareTo(BigDecimal.ONE) > 0)) {
                throw new IllegalArgumentException("Risk per trade must be between 0 and 1");
            }
            
            if (minConfidence < 0.0 || minConfidence > 1.0) {
                throw new IllegalArgumentException("Min confidence must be between 0.0 and 1.0");
            }
            
            return new StrategyConfig(this);
        }
    }
}