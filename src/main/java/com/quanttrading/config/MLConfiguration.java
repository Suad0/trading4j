package com.quanttrading.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for ML components.
 * Provides comprehensive configuration for all ML-related settings.
 */
@Configuration
@ConfigurationProperties(prefix = "ml")
public class MLConfiguration {
    
    private boolean enabled = true;
    private String modelStoragePath = "./models/";
    private int minTrainingData = 100;
    private int retrainingThreshold = 500;
    private double defaultConfidenceThreshold = 0.65;
    private boolean autoRetraining = true;
    private int maxModelAge = 30; // days
    private int maxHistorySize = 200;
    
    // Feature Engineering Configuration
    private FeatureConfig features = new FeatureConfig();
    
    // Model Performance Configuration
    private PerformanceConfig performance = new PerformanceConfig();
    
    // Risk Management Configuration
    private RiskConfig risk = new RiskConfig();
    
    // Getters and Setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public String getModelStoragePath() { return modelStoragePath; }
    public void setModelStoragePath(String modelStoragePath) { this.modelStoragePath = modelStoragePath; }
    
    public int getMinTrainingData() { return minTrainingData; }
    public void setMinTrainingData(int minTrainingData) { this.minTrainingData = minTrainingData; }
    
    public int getRetrainingThreshold() { return retrainingThreshold; }
    public void setRetrainingThreshold(int retrainingThreshold) { this.retrainingThreshold = retrainingThreshold; }
    
    public double getDefaultConfidenceThreshold() { return defaultConfidenceThreshold; }
    public void setDefaultConfidenceThreshold(double defaultConfidenceThreshold) { 
        this.defaultConfidenceThreshold = defaultConfidenceThreshold; 
    }
    
    public boolean isAutoRetraining() { return autoRetraining; }
    public void setAutoRetraining(boolean autoRetraining) { this.autoRetraining = autoRetraining; }
    
    public int getMaxModelAge() { return maxModelAge; }
    public void setMaxModelAge(int maxModelAge) { this.maxModelAge = maxModelAge; }
    
    public int getMaxHistorySize() { return maxHistorySize; }
    public void setMaxHistorySize(int maxHistorySize) { this.maxHistorySize = maxHistorySize; }
    
    public FeatureConfig getFeatures() { return features; }
    public void setFeatures(FeatureConfig features) { this.features = features; }
    
    public PerformanceConfig getPerformance() { return performance; }
    public void setPerformance(PerformanceConfig performance) { this.performance = performance; }
    
    public RiskConfig getRisk() { return risk; }
    public void setRisk(RiskConfig risk) { this.risk = risk; }
    
    public static class FeatureConfig {
        private int lookbackPeriod = 50;
        private boolean enableTechnicalIndicators = true;
        private boolean enableStatisticalFeatures = true;
        private boolean enableVolumeFeatures = true;
        private boolean enableVolatilityFeatures = true;
        private boolean enableMomentumFeatures = true;
        private boolean enablePatternRecognition = true;
        
        public int getLookbackPeriod() { return lookbackPeriod; }
        public void setLookbackPeriod(int lookbackPeriod) { this.lookbackPeriod = lookbackPeriod; }
        
        public boolean isEnableTechnicalIndicators() { return enableTechnicalIndicators; }
        public void setEnableTechnicalIndicators(boolean enableTechnicalIndicators) { 
            this.enableTechnicalIndicators = enableTechnicalIndicators; 
        }
        
        public boolean isEnableStatisticalFeatures() { return enableStatisticalFeatures; }
        public void setEnableStatisticalFeatures(boolean enableStatisticalFeatures) { 
            this.enableStatisticalFeatures = enableStatisticalFeatures; 
        }
        
        public boolean isEnableVolumeFeatures() { return enableVolumeFeatures; }
        public void setEnableVolumeFeatures(boolean enableVolumeFeatures) { 
            this.enableVolumeFeatures = enableVolumeFeatures; 
        }
        
        public boolean isEnableVolatilityFeatures() { return enableVolatilityFeatures; }
        public void setEnableVolatilityFeatures(boolean enableVolatilityFeatures) { 
            this.enableVolatilityFeatures = enableVolatilityFeatures; 
        }
        
        public boolean isEnableMomentumFeatures() { return enableMomentumFeatures; }
        public void setEnableMomentumFeatures(boolean enableMomentumFeatures) { 
            this.enableMomentumFeatures = enableMomentumFeatures; 
        }
        
        public boolean isEnablePatternRecognition() { return enablePatternRecognition; }
        public void setEnablePatternRecognition(boolean enablePatternRecognition) { 
            this.enablePatternRecognition = enablePatternRecognition; 
        }
    }
    
    public static class PerformanceConfig {
        private double minAccuracyThreshold = 0.45;
        private int performanceWindowSize = 50;
        private boolean enablePerformanceTracking = true;
        private double performanceDecayFactor = 0.95;
        
        public double getMinAccuracyThreshold() { return minAccuracyThreshold; }
        public void setMinAccuracyThreshold(double minAccuracyThreshold) { 
            this.minAccuracyThreshold = minAccuracyThreshold; 
        }
        
        public int getPerformanceWindowSize() { return performanceWindowSize; }
        public void setPerformanceWindowSize(int performanceWindowSize) { 
            this.performanceWindowSize = performanceWindowSize; 
        }
        
        public boolean isEnablePerformanceTracking() { return enablePerformanceTracking; }
        public void setEnablePerformanceTracking(boolean enablePerformanceTracking) { 
            this.enablePerformanceTracking = enablePerformanceTracking; 
        }
        
        public double getPerformanceDecayFactor() { return performanceDecayFactor; }
        public void setPerformanceDecayFactor(double performanceDecayFactor) { 
            this.performanceDecayFactor = performanceDecayFactor; 
        }
    }
    
    public static class RiskConfig {
        private double maxPositionSizePercent = 0.1; // 10% of portfolio
        private double volatilityAdjustmentFactor = 1.5;
        private double confidenceScalingFactor = 1.2;
        private double stopLossMultiplier = 2.0;
        private double takeProfitRatio = 2.0; // Risk:Reward ratio
        private boolean enableDynamicSizing = true;
        private boolean enableVolatilityAdjustment = true;
        
        public double getMaxPositionSizePercent() { return maxPositionSizePercent; }
        public void setMaxPositionSizePercent(double maxPositionSizePercent) { 
            this.maxPositionSizePercent = maxPositionSizePercent; 
        }
        
        public double getVolatilityAdjustmentFactor() { return volatilityAdjustmentFactor; }
        public void setVolatilityAdjustmentFactor(double volatilityAdjustmentFactor) { 
            this.volatilityAdjustmentFactor = volatilityAdjustmentFactor; 
        }
        
        public double getConfidenceScalingFactor() { return confidenceScalingFactor; }
        public void setConfidenceScalingFactor(double confidenceScalingFactor) { 
            this.confidenceScalingFactor = confidenceScalingFactor; 
        }
        
        public double getStopLossMultiplier() { return stopLossMultiplier; }
        public void setStopLossMultiplier(double stopLossMultiplier) { 
            this.stopLossMultiplier = stopLossMultiplier; 
        }
        
        public double getTakeProfitRatio() { return takeProfitRatio; }
        public void setTakeProfitRatio(double takeProfitRatio) { 
            this.takeProfitRatio = takeProfitRatio; 
        }
        
        public boolean isEnableDynamicSizing() { return enableDynamicSizing; }
        public void setEnableDynamicSizing(boolean enableDynamicSizing) { 
            this.enableDynamicSizing = enableDynamicSizing; 
        }
        
        public boolean isEnableVolatilityAdjustment() { return enableVolatilityAdjustment; }
        public void setEnableVolatilityAdjustment(boolean enableVolatilityAdjustment) { 
            this.enableVolatilityAdjustment = enableVolatilityAdjustment; 
        }
    }
}