package com.quanttrading.ml.core;

import com.quanttrading.ml.MLModelMetrics;
import com.quanttrading.ml.MLPrediction;
import com.quanttrading.model.MarketData;

import java.util.List;
import java.util.Map;

/**
 * Core interface for all ML models in the trading system.
 * Provides a clean abstraction for different ML algorithms.
 */
public interface MLModel {
    
    /**
     * Get the unique name/identifier of this model.
     */
    String getModelName();
    
    /**
     * Train the model with historical market data.
     * @param historicalData training data
     * @param features extracted features for training
     * @return true if training was successful
     */
    boolean train(List<MarketData> historicalData, Map<String, List<Double>> features);
    
    /**
     * Make a prediction based on current market data and features.
     * @param currentData current market data
     * @param features current extracted features
     * @return ML prediction with confidence score
     */
    MLPrediction predict(MarketData currentData, Map<String, Double> features);
    
    /**
     * Update the model with new data (online learning).
     * @param newData new market data point
     * @param features features for the new data
     */
    void updateModel(MarketData newData, Map<String, Double> features);
    
    /**
     * Check if the model is trained and ready for predictions.
     */
    boolean isModelReady();
    
    /**
     * Get current model performance metrics.
     */
    MLModelMetrics getMetrics();
    
    /**
     * Check if the model needs retraining based on performance degradation.
     */
    boolean needsRetraining();
    
    /**
     * Get the minimum amount of data required for training.
     */
    int getMinimumTrainingData();
    
    /**
     * Save the model to the specified path.
     */
    boolean saveModel(String path);
    
    /**
     * Load the model from the specified path.
     */
    boolean loadModel(String path);
}