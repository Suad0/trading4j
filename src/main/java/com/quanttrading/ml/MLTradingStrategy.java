package com.quanttrading.ml;

import com.quanttrading.model.MarketData;
import com.quanttrading.strategy.TradingSignal;

import java.util.List;

/**
 * Interface for ML-enhanced trading strategies.
 */
public interface MLTradingStrategy {
    
    /**
     * Train the ML model with historical data.
     * @param historicalData list of historical market data
     * @return true if training was successful
     */
    boolean trainModel(List<MarketData> historicalData);
    
    /**
     * Make predictions based on current market data.
     * @param marketData current market data
     * @return ML predictions
     */
    MLPrediction predict(MarketData marketData);
    
    /**
     * Generate trading signals based on ML predictions.
     * @param prediction ML prediction result
     * @param marketData current market data
     * @return list of trading signals
     */
    List<TradingSignal> generateSignals(MLPrediction prediction, MarketData marketData);
    
    /**
     * Get the current model performance metrics.
     * @return model performance metrics
     */
    MLModelMetrics getModelMetrics();
    
    /**
     * Update the model with new data (online learning).
     * @param newData new market data point
     */
    void updateModel(MarketData newData);
    
    /**
     * Check if the model needs retraining.
     * @return true if retraining is recommended
     */
    boolean needsRetraining();
    
    /**
     * Get the model's confidence in its predictions.
     * @return confidence score between 0 and 1
     */
    double getModelConfidence();
    
    /**
     * Save the trained model to disk.
     * @param modelPath path to save the model
     * @return true if save was successful
     */
    boolean saveModel(String modelPath);
    
    /**
     * Load a trained model from disk.
     * @param modelPath path to load the model from
     * @return true if load was successful
     */
    boolean loadModel(String modelPath);
}