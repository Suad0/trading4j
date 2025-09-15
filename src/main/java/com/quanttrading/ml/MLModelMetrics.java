package com.quanttrading.ml;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents performance metrics for ML models.
 */
public class MLModelMetrics {
    
    private final String modelName;
    private final LocalDateTime lastTrainingTime;
    private final LocalDateTime lastUpdateTime;
    private final int trainingDataSize;
    private final int testDataSize;
    
    // Performance metrics
    private final double accuracy;
    private final double precision;
    private final double recall;
    private final double f1Score;
    private final double rocAuc;
    private final double sharpeRatio;
    private final double maxDrawdown;
    private final double winRate;
    private final double avgReturn;
    private final double volatility;
    
    // Model-specific metrics
    private final Map<String, Double> additionalMetrics;
    private final int totalPredictions;
    private final int correctPredictions;
    private final double modelDrift;
    
    private MLModelMetrics(Builder builder) {
        this.modelName = builder.modelName;
        this.lastTrainingTime = builder.lastTrainingTime;
        this.lastUpdateTime = builder.lastUpdateTime;
        this.trainingDataSize = builder.trainingDataSize;
        this.testDataSize = builder.testDataSize;
        this.accuracy = builder.accuracy;
        this.precision = builder.precision;
        this.recall = builder.recall;
        this.f1Score = builder.f1Score;
        this.rocAuc = builder.rocAuc;
        this.sharpeRatio = builder.sharpeRatio;
        this.maxDrawdown = builder.maxDrawdown;
        this.winRate = builder.winRate;
        this.avgReturn = builder.avgReturn;
        this.volatility = builder.volatility;
        this.additionalMetrics = builder.additionalMetrics;
        this.totalPredictions = builder.totalPredictions;
        this.correctPredictions = builder.correctPredictions;
        this.modelDrift = builder.modelDrift;
    }
    
    // Getters
    public String getModelName() { return modelName; }
    public LocalDateTime getLastTrainingTime() { return lastTrainingTime; }
    public LocalDateTime getLastUpdateTime() { return lastUpdateTime; }
    public int getTrainingDataSize() { return trainingDataSize; }
    public int getTestDataSize() { return testDataSize; }
    public double getAccuracy() { return accuracy; }
    public double getPrecision() { return precision; }
    public double getRecall() { return recall; }
    public double getF1Score() { return f1Score; }
    public double getRocAuc() { return rocAuc; }
    public double getSharpeRatio() { return sharpeRatio; }
    public double getMaxDrawdown() { return maxDrawdown; }
    public double getWinRate() { return winRate; }
    public double getAvgReturn() { return avgReturn; }
    public double getVolatility() { return volatility; }
    public Map<String, Double> getAdditionalMetrics() { return additionalMetrics; }
    public int getTotalPredictions() { return totalPredictions; }
    public int getCorrectPredictions() { return correctPredictions; }
    public double getModelDrift() { return modelDrift; }
    
    public static Builder builder(String modelName) {
        return new Builder(modelName);
    }
    
    public static class Builder {
        private final String modelName;
        private LocalDateTime lastTrainingTime;
        private LocalDateTime lastUpdateTime;
        private int trainingDataSize;
        private int testDataSize;
        private double accuracy;
        private double precision;
        private double recall;
        private double f1Score;
        private double rocAuc;
        private double sharpeRatio;
        private double maxDrawdown;
        private double winRate;
        private double avgReturn;
        private double volatility;
        private Map<String, Double> additionalMetrics;
        private int totalPredictions;
        private int correctPredictions;
        private double modelDrift;
        
        public Builder(String modelName) {
            this.modelName = modelName;
        }
        
        public Builder lastTrainingTime(LocalDateTime lastTrainingTime) {
            this.lastTrainingTime = lastTrainingTime;
            return this;
        }
        
        public Builder lastUpdateTime(LocalDateTime lastUpdateTime) {
            this.lastUpdateTime = lastUpdateTime;
            return this;
        }
        
        public Builder trainingDataSize(int trainingDataSize) {
            this.trainingDataSize = trainingDataSize;
            return this;
        }
        
        public Builder testDataSize(int testDataSize) {
            this.testDataSize = testDataSize;
            return this;
        }
        
        public Builder accuracy(double accuracy) {
            this.accuracy = accuracy;
            return this;
        }
        
        public Builder precision(double precision) {
            this.precision = precision;
            return this;
        }
        
        public Builder recall(double recall) {
            this.recall = recall;
            return this;
        }
        
        public Builder f1Score(double f1Score) {
            this.f1Score = f1Score;
            return this;
        }
        
        public Builder rocAuc(double rocAuc) {
            this.rocAuc = rocAuc;
            return this;
        }
        
        public Builder sharpeRatio(double sharpeRatio) {
            this.sharpeRatio = sharpeRatio;
            return this;
        }
        
        public Builder maxDrawdown(double maxDrawdown) {
            this.maxDrawdown = maxDrawdown;
            return this;
        }
        
        public Builder winRate(double winRate) {
            this.winRate = winRate;
            return this;
        }
        
        public Builder avgReturn(double avgReturn) {
            this.avgReturn = avgReturn;
            return this;
        }
        
        public Builder volatility(double volatility) {
            this.volatility = volatility;
            return this;
        }
        
        public Builder additionalMetrics(Map<String, Double> additionalMetrics) {
            this.additionalMetrics = additionalMetrics;
            return this;
        }
        
        public Builder totalPredictions(int totalPredictions) {
            this.totalPredictions = totalPredictions;
            return this;
        }
        
        public Builder correctPredictions(int correctPredictions) {
            this.correctPredictions = correctPredictions;
            return this;
        }
        
        public Builder modelDrift(double modelDrift) {
            this.modelDrift = modelDrift;
            return this;
        }
        
        public MLModelMetrics build() {
            return new MLModelMetrics(this);
        }
    }
    
    @Override
    public String toString() {
        return "MLModelMetrics{" +
                "modelName='" + modelName + '\'' +
                ", accuracy=" + accuracy +
                ", precision=" + precision +
                ", recall=" + recall +
                ", f1Score=" + f1Score +
                ", sharpeRatio=" + sharpeRatio +
                ", winRate=" + winRate +
                ", avgReturn=" + avgReturn +
                '}';
    }
}