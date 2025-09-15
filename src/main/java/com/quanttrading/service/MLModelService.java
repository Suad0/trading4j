package com.quanttrading.service;

import com.quanttrading.config.MLConfig;
import com.quanttrading.ml.MLModelMetrics;
import com.quanttrading.ml.MLPrediction;
import com.quanttrading.model.MarketData;
import com.quanttrading.repository.TradeRepository;
import com.quanttrading.strategy.MLEnhancedTradingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing ML models, training, and predictions.
 */
@Service
public class MLModelService {
    
    private static final Logger logger = LoggerFactory.getLogger(MLModelService.class);
    
    private final MLConfig mlConfig;
    private final MarketDataService marketDataService;
    private final TradeRepository tradeRepository;
    
    // Strategy instances per symbol
    private final Map<String, MLEnhancedTradingStrategy> strategies = new ConcurrentHashMap<>();
    
    // Model status tracking
    private final Map<String, ModelStatus> modelStatuses = new ConcurrentHashMap<>();
    
    @Autowired
    public MLModelService(MLConfig mlConfig, 
                         MarketDataService marketDataService,
                         TradeRepository tradeRepository) {
        this.mlConfig = mlConfig;
        this.marketDataService = marketDataService;
        this.tradeRepository = tradeRepository;
        
        logger.info("ML Model Service initialized with ML enabled: {}", mlConfig.isEnabled());
    }
    
    /**
     * Initialize ML strategy for a symbol.
     */
    public boolean initializeStrategy(String symbol) {
        try {
            if (!mlConfig.isEnabled()) {
                logger.warn("ML is disabled in configuration");
                return false;
            }
            
            if (strategies.containsKey(symbol)) {
                logger.debug("ML strategy already exists for symbol: {}", symbol);
                return true;
            }
            
            MLEnhancedTradingStrategy strategy = new MLEnhancedTradingStrategy();
            strategies.put(symbol, strategy);
            
            modelStatuses.put(symbol, new ModelStatus(symbol));
            
            logger.info("Initialized ML strategy for symbol: {}", symbol);
            return true;
            
        } catch (Exception e) {
            logger.error("Error initializing ML strategy for {}: {}", symbol, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Train ML models for a specific symbol.
     */
    public CompletableFuture<Boolean> trainModels(String symbol) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Starting ML model training for symbol: {}", symbol);
                
                MLEnhancedTradingStrategy strategy = strategies.get(symbol);
                if (strategy == null) {
                    logger.error("No ML strategy found for symbol: {}", symbol);
                    return false;
                }
                
                // Get historical market data
                List<MarketData> historicalData = getHistoricalData(symbol);
                
                if (historicalData.size() < mlConfig.getMinTrainingData()) {
                    logger.warn("Insufficient historical data for training {}: {} < {}", 
                               symbol, historicalData.size(), mlConfig.getMinTrainingData());
                    return false;
                }
                
                // Update model status
                ModelStatus status = modelStatuses.get(symbol);
                status.setTrainingInProgress(true);
                status.setLastTrainingAttempt(LocalDateTime.now());
                
                // Train the models
                boolean success = strategy.trainModel(historicalData);
                
                // Update status
                status.setTrainingInProgress(false);
                status.setModelTrained(success);
                if (success) {
                    status.setLastSuccessfulTraining(LocalDateTime.now());
                    status.setTrainingDataSize(historicalData.size());
                }
                
                logger.info("ML model training for {} completed: {}", symbol, success ? "SUCCESS" : "FAILED");
                return success;
                
            } catch (Exception e) {
                logger.error("Error during ML model training for {}: {}", symbol, e.getMessage(), e);
                
                ModelStatus status = modelStatuses.get(symbol);
                if (status != null) {
                    status.setTrainingInProgress(false);
                    status.setLastError(e.getMessage());
                }
                
                return false;
            }
        });
    }
    
    /**
     * Get ML prediction for a symbol.
     */
    public MLPrediction getPrediction(String symbol, MarketData currentData) {
        try {
            if (!mlConfig.isEnabled()) {
                return null;
            }
            
            MLEnhancedTradingStrategy strategy = strategies.get(symbol);
            if (strategy == null) {
                logger.debug("No ML strategy found for symbol: {}", symbol);
                return null;
            }
            
            ModelStatus status = modelStatuses.get(symbol);
            if (status == null || !status.isModelTrained()) {
                logger.debug("ML model not trained for symbol: {}", symbol);
                return null;
            }
            
            return strategy.predict(currentData);
            
        } catch (Exception e) {
            logger.error("Error getting ML prediction for {}: {}", symbol, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Get model metrics for a symbol.
     */
    public MLModelMetrics getModelMetrics(String symbol) {
        try {
            MLEnhancedTradingStrategy strategy = strategies.get(symbol);
            if (strategy == null) {
                return null;
            }
            
            return null; // Will implement metrics later
            
        } catch (Exception e) {
            logger.error("Error getting model metrics for {}: {}", symbol, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Get status for all ML models.
     */
    public Map<String, ModelStatus> getAllModelStatuses() {
        return new HashMap<>(modelStatuses);
    }
    
    /**
     * Get status for a specific model.
     */
    public ModelStatus getModelStatus(String symbol) {
        return modelStatuses.get(symbol);
    }
    
    /**
     * Save ML models to disk.
     */
    public boolean saveModels(String symbol) {
        try {
            MLEnhancedTradingStrategy strategy = strategies.get(symbol);
            if (strategy == null) {
                return false;
            }
            
            String modelPath = mlConfig.getModelStoragePath() + symbol + "_" + 
                              LocalDateTime.now().toLocalDate().toString();
            
            boolean saved = strategy.saveModel(modelPath);
            
            if (saved) {
                ModelStatus status = modelStatuses.get(symbol);
                if (status != null) {
                    status.setLastModelSave(LocalDateTime.now());
                    status.setModelPath(modelPath);
                }
            }
            
            return saved;
            
        } catch (Exception e) {
            logger.error("Error saving models for {}: {}", symbol, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Load ML models from disk.
     */
    public boolean loadModels(String symbol, String modelPath) {
        try {
            MLEnhancedTradingStrategy strategy = strategies.get(symbol);
            if (strategy == null) {
                initializeStrategy(symbol);
                strategy = strategies.get(symbol);
            }
            
            boolean loaded = strategy.loadModel(modelPath);
            
            if (loaded) {
                ModelStatus status = modelStatuses.get(symbol);
                if (status != null) {
                    status.setModelTrained(true);
                    status.setModelPath(modelPath);
                    status.setLastModelLoad(LocalDateTime.now());
                }
            }
            
            return loaded;
            
        } catch (Exception e) {
            logger.error("Error loading models for {}: {}", symbol, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Update models with new market data.
     */
    public void updateModels(String symbol, MarketData newData) {
        try {
            MLEnhancedTradingStrategy strategy = strategies.get(symbol);
            if (strategy != null) {
                strategy.updateModel(newData);
                
                // Check if retraining is needed
                if (mlConfig.isAutoRetraining() && strategy.needsRetraining()) {
                    logger.info("Scheduling automatic retraining for symbol: {}", symbol);
                    trainModels(symbol);
                }
            }
        } catch (Exception e) {
            logger.error("Error updating models for {}: {}", symbol, e.getMessage(), e);
        }
    }
    
    /**
     * Scheduled task to check for model maintenance needs.
     */
    @Scheduled(fixedRate = 3600000) // Run every hour
    public void performModelMaintenance() {
        if (!mlConfig.isEnabled()) {
            return;
        }
        
        logger.debug("Performing ML model maintenance check");
        
        LocalDateTime now = LocalDateTime.now();
        
        for (Map.Entry<String, ModelStatus> entry : modelStatuses.entrySet()) {
            String symbol = entry.getKey();
            ModelStatus status = entry.getValue();
            
            // Check if model is too old
            if (status.getLastSuccessfulTraining() != null) {
                long daysSinceTraining = java.time.Duration.between(
                    status.getLastSuccessfulTraining(), now).toDays();
                
                if (daysSinceTraining > mlConfig.getMaxModelAge()) {
                    logger.info("Model for {} is {} days old, scheduling retraining", 
                               symbol, daysSinceTraining);
                    trainModels(symbol);
                }
            }
            
            // Auto-save models periodically
            if (status.isModelTrained() && status.getLastModelSave() != null) {
                long hoursSinceSave = java.time.Duration.between(
                    status.getLastModelSave(), now).toHours();
                
                if (hoursSinceSave > 24) { // Save daily
                    logger.debug("Auto-saving models for symbol: {}", symbol);
                    saveModels(symbol);
                }
            }
        }
    }
    
    /**
     * Get historical market data for training.
     */
    private List<MarketData> getHistoricalData(String symbol) {
        try {
            // In a real implementation, this would fetch from a data provider
            // For now, return empty list - this should be implemented based on your data source
            return new ArrayList<>();
            
        } catch (Exception e) {
            logger.error("Error fetching historical data for {}: {}", symbol, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Model status tracking class.
     */
    public static class ModelStatus {
        private final String symbol;
        private boolean modelTrained = false;
        private boolean trainingInProgress = false;
        private LocalDateTime lastTrainingAttempt;
        private LocalDateTime lastSuccessfulTraining;
        private LocalDateTime lastModelSave;
        private LocalDateTime lastModelLoad;
        private int trainingDataSize = 0;
        private String modelPath;
        private String lastError;
        private double modelConfidence = 0.0;
        
        public ModelStatus(String symbol) {
            this.symbol = symbol;
        }
        
        // Getters and Setters
        public String getSymbol() { return symbol; }
        
        public boolean isModelTrained() { return modelTrained; }
        public void setModelTrained(boolean modelTrained) { this.modelTrained = modelTrained; }
        
        public boolean isTrainingInProgress() { return trainingInProgress; }
        public void setTrainingInProgress(boolean trainingInProgress) { this.trainingInProgress = trainingInProgress; }
        
        public LocalDateTime getLastTrainingAttempt() { return lastTrainingAttempt; }
        public void setLastTrainingAttempt(LocalDateTime lastTrainingAttempt) { this.lastTrainingAttempt = lastTrainingAttempt; }
        
        public LocalDateTime getLastSuccessfulTraining() { return lastSuccessfulTraining; }
        public void setLastSuccessfulTraining(LocalDateTime lastSuccessfulTraining) { this.lastSuccessfulTraining = lastSuccessfulTraining; }
        
        public LocalDateTime getLastModelSave() { return lastModelSave; }
        public void setLastModelSave(LocalDateTime lastModelSave) { this.lastModelSave = lastModelSave; }
        
        public LocalDateTime getLastModelLoad() { return lastModelLoad; }
        public void setLastModelLoad(LocalDateTime lastModelLoad) { this.lastModelLoad = lastModelLoad; }
        
        public int getTrainingDataSize() { return trainingDataSize; }
        public void setTrainingDataSize(int trainingDataSize) { this.trainingDataSize = trainingDataSize; }
        
        public String getModelPath() { return modelPath; }
        public void setModelPath(String modelPath) { this.modelPath = modelPath; }
        
        public String getLastError() { return lastError; }
        public void setLastError(String lastError) { this.lastError = lastError; }
        
        public double getModelConfidence() { return modelConfidence; }
        public void setModelConfidence(double modelConfidence) { this.modelConfidence = modelConfidence; }
    }
}