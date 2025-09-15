package com.quanttrading.ml.stoxlstm;

import com.quanttrading.ml.MLPrediction;
import com.quanttrading.ml.core.FeatureEngineer;
import com.quanttrading.model.MarketData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * Service layer for Stochastic XLSTM model integration.
 * Provides high-level interface for the trading system.
 */
@Service
public class StochasticXLSTMService {
    
    private static final Logger logger = LoggerFactory.getLogger(StochasticXLSTMService.class);
    
    private final FeatureEngineer featureEngineer;
    private StochasticXLSTMModel model;
    private final TimeSeriesPreprocessor preprocessor;
    
    // Model configuration
    private static final int INPUT_SIZE = 12; // Number of features
    private static final int HIDDEN_SIZE = 64;
    private static final int SEQUENCE_LENGTH = 50;
    
    @Autowired
    public StochasticXLSTMService(FeatureEngineer featureEngineer) {
        this.featureEngineer = featureEngineer;
        this.preprocessor = new TimeSeriesPreprocessor();
        initializeModel();
    }
    
    private void initializeModel() {
        try {
            this.model = new StochasticXLSTMModel();
            logger.info("StochasticXLSTM service initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize StochasticXLSTM service", e);
            throw new RuntimeException("Failed to initialize StochasticXLSTM service", e);
        }
    }
    
    /**
     * Train the model with historical market data.
     */
    public CompletableFuture<Void> trainModel(List<MarketData> historicalData) {
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("Starting StochasticXLSTM model training with {} data points", historicalData.size());
                
                // Extract features using the feature engineer
                Map<String, List<Double>> features = extractFeaturesForTraining(historicalData);
                
                if (features.isEmpty()) {
                    logger.warn("No training features available");
                    return;
                }
                
                // Train the model with the correct interface
                boolean success = model.train(historicalData, features);
                
                if (success) {
                    logger.info("StochasticXLSTM model training completed successfully");
                } else {
                    logger.warn("Model training was not successful");
                }
                
            } catch (Exception e) {
                logger.error("Error during model training", e);
                throw new RuntimeException("Model training failed", e);
            }
        });
    }
    
    /**
     * Generate prediction for given market data.
     */
    public MLPrediction predict(MarketData marketData) {
        try {
            if (!model.isModelReady()) {
                logger.warn("Model is not ready for predictions");
                return createDefaultPrediction(marketData.getSymbol());
            }
            
            // Extract features
            Map<String, Double> features = featureEngineer.extractFeatures(marketData.getSymbol(), marketData);
            
            // Generate prediction using the correct interface
            MLPrediction prediction = model.predict(marketData, features);
            
            logger.debug("Generated prediction for {}: {} (confidence: {:.3f})", 
                    marketData.getSymbol(), prediction.getPriceDirection(), prediction.getConfidence());
            
            return prediction;
            
        } catch (Exception e) {
            logger.error("Error during prediction", e);
            return createDefaultPrediction(marketData.getSymbol());
        }
    }
    
    /**
     * Generate predictions for multiple market data points.
     */
    public CompletableFuture<Map<String, MLPrediction>> predictBatch(List<MarketData> marketDataList) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, MLPrediction> predictions = new HashMap<>();
            
            if (!model.isModelReady()) {
                logger.warn("Model is not ready for batch predictions");
                for (MarketData marketData : marketDataList) {
                    predictions.put(marketData.getSymbol(), createDefaultPrediction(marketData.getSymbol()));
                }
                return predictions;
            }
            
            for (MarketData marketData : marketDataList) {
                try {
                    MLPrediction prediction = predict(marketData);
                    predictions.put(marketData.getSymbol(), prediction);
                } catch (Exception e) {
                    logger.error("Error predicting for symbol {}", marketData.getSymbol(), e);
                    predictions.put(marketData.getSymbol(), createDefaultPrediction(marketData.getSymbol()));
                }
            }
            
            return predictions;
        });
    }
    
    /**
     * Update model with new market data and actual outcome.
     */
    public void updateModel(MarketData marketData, double actualOutcome) {
        try {
            Map<String, Double> features = featureEngineer.extractFeatures(marketData.getSymbol(), marketData);
            model.updateModel(marketData, features);
            
            logger.debug("Updated model with new data for {}", marketData.getSymbol());
            
        } catch (Exception e) {
            logger.error("Error updating model", e);
        }
    }
    
    /**
     * Get model performance metrics.
     */
    public Map<String, Object> getModelMetrics() {
        try {
            var metrics = model.getMetrics();
            
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("model_name", metrics.getModelName());
            result.put("accuracy", metrics.getAccuracy());
            result.put("precision", metrics.getPrecision());
            result.put("recall", metrics.getRecall());
            result.put("last_training_time", metrics.getLastTrainingTime());
            result.put("additional_metrics", metrics.getAdditionalMetrics());
            
            // Add stochastic-specific metrics
            result.put("last_kl_divergence", model.getLastKLDivergence());
            result.put("last_uncertainty", model.getLastUncertainty());
            result.put("is_ready", model.isModelReady());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error getting model metrics", e);
            return Map.of("error", "Failed to get metrics");
        }
    }
    
    /**
     * Get model configuration.
     */
    public Map<String, Object> getModelConfiguration() {
        StoxLSTMConfig config = model.getConfig();
        
        Map<String, Object> configMap = new java.util.HashMap<>();
        configMap.put("input_size", INPUT_SIZE);
        configMap.put("hidden_size", HIDDEN_SIZE);
        configMap.put("sequence_length", SEQUENCE_LENGTH);
        configMap.put("latent_dim", config.latentDim);
        configMap.put("use_exponential_gating", config.useExponentialGating);
        configMap.put("use_memory_mixing", config.useMemoryMixing);
        configMap.put("use_layer_normalization", config.useLayerNormalization);
        configMap.put("stochastic_regularization", config.stochasticRegularization);
        configMap.put("learning_rate", config.learningRate);
        configMap.put("kl_divergence_weight", config.beta);
        
        return configMap;
    }
    
    /**
     * Check if model is ready for predictions.
     */
    public boolean isModelReady() {
        return model != null && model.isModelReady();
    }
    
    /**
     * Retrain model with all available data.
     */
    public CompletableFuture<Void> retrainModel(List<MarketData> allHistoricalData) {
        return trainModel(allHistoricalData);
    }
    
    private MLPrediction createDefaultPrediction(String symbol) {
        Map<String, Object> predictions = new HashMap<>();
        predictions.put("direction", MLPrediction.Direction.SIDEWAYS);
        predictions.put("prob_up", 0.33);
        predictions.put("prob_down", 0.33);
        predictions.put("prob_sideways", 0.34);
        predictions.put("uncertainty", 1.0);
        
        return new MLPrediction(
                symbol,
                MLPrediction.PredictionType.PRICE_DIRECTION,
                0.1,
                predictions,
                "StochasticXLSTM_Default",
                Map.of()
        );
    }
    
    /**
     * Extract features for training from historical data.
     */
    private Map<String, List<Double>> extractFeaturesForTraining(List<MarketData> historicalData) {
        Map<String, List<Double>> features = new HashMap<>();
        
        // Initialize feature lists
        String[] featureNames = {
            "price", "volume", "high", "low", "open", "close", 
            "price_change", "volume_change", "volatility", "momentum"
        };
        
        for (String featureName : featureNames) {
            features.put(featureName, new ArrayList<>());
        }
        
        // Extract features for each data point
        for (int i = 0; i < historicalData.size(); i++) {
            MarketData data = historicalData.get(i);
            Map<String, Double> currentFeatures = featureEngineer.extractFeatures(data.getSymbol(), data);
            
            // Add price-based features
            features.get("price").add(data.getPrice().doubleValue());
            features.get("volume").add(data.getVolume().doubleValue());
            features.get("high").add(data.getHigh().doubleValue());
            features.get("low").add(data.getLow().doubleValue());
            features.get("open").add(data.getOpen().doubleValue());
            features.get("close").add(data.getClose().doubleValue());
            
            // Calculate derived features
            if (i > 0) {
                MarketData prevData = historicalData.get(i - 1);
                double priceChange = (data.getPrice().doubleValue() - prevData.getPrice().doubleValue()) / prevData.getPrice().doubleValue();
                double volumeChange = (data.getVolume().doubleValue() - prevData.getVolume().doubleValue()) / prevData.getVolume().doubleValue();
                
                features.get("price_change").add(priceChange);
                features.get("volume_change").add(volumeChange);
            } else {
                features.get("price_change").add(0.0);
                features.get("volume_change").add(0.0);
            }
            
            // Add technical indicator features from feature engineer
            features.get("volatility").add(currentFeatures.getOrDefault("volatility", 0.0));
            features.get("momentum").add(currentFeatures.getOrDefault("momentum", 0.0));
        }
        
        return features;
    }
    
    // Getters for testing and monitoring
    public StochasticXLSTMModel getModel() {
        return model;
    }
    
    public FeatureEngineer getFeatureEngineer() {
        return featureEngineer;
    }
}