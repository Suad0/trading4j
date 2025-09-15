package com.quanttrading.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for ML components.
 */
@Configuration
@ConfigurationProperties(prefix = "ml")
public class MLConfig {
    
    private boolean enabled = true;
    private String modelStoragePath = "./models/";
    private int minTrainingData = 100;
    private int retrainingThreshold = 500;
    private double defaultConfidenceThreshold = 0.6;
    private boolean autoRetraining = true;
    private int maxModelAge = 30; // days
    
    // LSTM Configuration
    private LSTMConfig lstm = new LSTMConfig();
    
    // Regime Detection Configuration  
    private RegimeConfig regime = new RegimeConfig();
    
    // Feature Engineering Configuration
    private FeatureConfig features = new FeatureConfig();
    
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
    
    public LSTMConfig getLstm() { return lstm; }
    public void setLstm(LSTMConfig lstm) { this.lstm = lstm; }
    
    public RegimeConfig getRegime() { return regime; }
    public void setRegime(RegimeConfig regime) { this.regime = regime; }
    
    public FeatureConfig getFeatures() { return features; }
    public void setFeatures(FeatureConfig features) { this.features = features; }
    
    public static class LSTMConfig {
        private int sequenceLength = 20;
        private int hiddenLayerSize = 64;
        private double learningRate = 0.001;
        private int epochs = 100;
        private int batchSize = 32;
        
        public int getSequenceLength() { return sequenceLength; }
        public void setSequenceLength(int sequenceLength) { this.sequenceLength = sequenceLength; }
        
        public int getHiddenLayerSize() { return hiddenLayerSize; }
        public void setHiddenLayerSize(int hiddenLayerSize) { this.hiddenLayerSize = hiddenLayerSize; }
        
        public double getLearningRate() { return learningRate; }
        public void setLearningRate(double learningRate) { this.learningRate = learningRate; }
        
        public int getEpochs() { return epochs; }
        public void setEpochs(int epochs) { this.epochs = epochs; }
        
        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    }
    
    public static class RegimeConfig {
        private int lookbackPeriod = 30;
        private int numClusters = 4;
        private double volatilityThreshold = 0.03;
        private double trendThreshold = 0.001;
        
        public int getLookbackPeriod() { return lookbackPeriod; }
        public void setLookbackPeriod(int lookbackPeriod) { this.lookbackPeriod = lookbackPeriod; }
        
        public int getNumClusters() { return numClusters; }
        public void setNumClusters(int numClusters) { this.numClusters = numClusters; }
        
        public double getVolatilityThreshold() { return volatilityThreshold; }
        public void setVolatilityThreshold(double volatilityThreshold) { 
            this.volatilityThreshold = volatilityThreshold; 
        }
        
        public double getTrendThreshold() { return trendThreshold; }
        public void setTrendThreshold(double trendThreshold) { this.trendThreshold = trendThreshold; }
    }
    
    public static class FeatureConfig {
        private int lookbackPeriod = 50;
        private boolean enableTechnicalIndicators = true;
        private boolean enableStatisticalFeatures = true;
        private boolean enableVolumeFeatures = true;
        private boolean enableVolatilityFeatures = true;
        
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
    }
}