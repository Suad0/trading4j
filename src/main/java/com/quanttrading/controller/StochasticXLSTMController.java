package com.quanttrading.controller;

import com.quanttrading.ml.MLPrediction;
import com.quanttrading.ml.stoxlstm.StochasticXLSTMService;
import com.quanttrading.model.MarketData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for Stochastic XLSTM model operations.
 * Provides endpoints for training, prediction, and model management.
 */
@RestController
@RequestMapping("/api/ml/stochastic-xlstm")
@CrossOrigin(origins = "*")
public class StochasticXLSTMController {
    
    private static final Logger logger = LoggerFactory.getLogger(StochasticXLSTMController.class);
    
    private final StochasticXLSTMService stochasticXLSTMService;
    
    @Autowired
    public StochasticXLSTMController(StochasticXLSTMService stochasticXLSTMService) {
        this.stochasticXLSTMService = stochasticXLSTMService;
    }
    
    /**
     * Get model status and configuration.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getModelStatus() {
        try {
            Map<String, Object> status = Map.of(
                    "model_ready", stochasticXLSTMService.isModelReady(),
                    "configuration", stochasticXLSTMService.getModelConfiguration(),
                    "metrics", stochasticXLSTMService.getModelMetrics()
            );
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            logger.error("Error getting model status", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get model status: " + e.getMessage()));
        }
    }
    
    /**
     * Train the model with historical data.
     */
    @PostMapping("/train")
    public ResponseEntity<Map<String, Object>> trainModel(@RequestBody List<MarketData> historicalData) {
        try {
            if (historicalData == null || historicalData.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Historical data is required for training"));
            }
            
            logger.info("Received training request with {} data points", historicalData.size());
            
            // Start training asynchronously
            CompletableFuture<Void> trainingFuture = stochasticXLSTMService.trainModel(historicalData);
            
            return ResponseEntity.accepted()
                    .body(Map.of(
                            "message", "Training started",
                            "data_points", historicalData.size(),
                            "status", "training_in_progress"
                    ));
            
        } catch (Exception e) {
            logger.error("Error starting model training", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to start training: " + e.getMessage()));
        }
    }
    
    /**
     * Generate prediction for single market data point.
     */
    @PostMapping("/predict")
    public ResponseEntity<MLPrediction> predict(@RequestBody MarketData marketData) {
        try {
            if (marketData == null) {
                return ResponseEntity.badRequest().build();
            }
            
            if (!stochasticXLSTMService.isModelReady()) {
                logger.warn("Model is not ready for predictions");
                return ResponseEntity.status(503).build();
            }
            
            MLPrediction prediction = stochasticXLSTMService.predict(marketData);
            
            logger.debug("Generated prediction for {}: {} (confidence: {:.3f})", 
                    marketData.getSymbol(), prediction.getPriceDirection(), prediction.getConfidence());
            
            return ResponseEntity.ok(prediction);
            
        } catch (Exception e) {
            logger.error("Error generating prediction", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Generate predictions for multiple market data points.
     */
    @PostMapping("/predict/batch")
    public ResponseEntity<CompletableFuture<Map<String, MLPrediction>>> predictBatch(
            @RequestBody List<MarketData> marketDataList) {
        try {
            if (marketDataList == null || marketDataList.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            if (!stochasticXLSTMService.isModelReady()) {
                logger.warn("Model is not ready for predictions");
                return ResponseEntity.status(503).build();
            }
            
            CompletableFuture<Map<String, MLPrediction>> predictions = 
                    stochasticXLSTMService.predictBatch(marketDataList);
            
            logger.info("Generated batch predictions for {} symbols", marketDataList.size());
            
            return ResponseEntity.ok(predictions);
            
        } catch (Exception e) {
            logger.error("Error generating batch predictions", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Update model with new data and actual outcome.
     */
    @PostMapping("/update")
    public ResponseEntity<Map<String, Object>> updateModel(
            @RequestBody UpdateModelRequest request) {
        try {
            if (request == null || request.getMarketData() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Market data is required"));
            }
            
            stochasticXLSTMService.updateModel(request.getMarketData(), request.getActualOutcome());
            
            return ResponseEntity.ok(Map.of(
                    "message", "Model updated successfully",
                    "symbol", request.getMarketData().getSymbol(),
                    "actual_outcome", request.getActualOutcome()
            ));
            
        } catch (Exception e) {
            logger.error("Error updating model", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to update model: " + e.getMessage()));
        }
    }
    
    /**
     * Get detailed model metrics and performance statistics.
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getModelMetrics() {
        try {
            Map<String, Object> metrics = stochasticXLSTMService.getModelMetrics();
            return ResponseEntity.ok(metrics);
            
        } catch (Exception e) {
            logger.error("Error getting model metrics", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get metrics: " + e.getMessage()));
        }
    }
    
    /**
     * Get model configuration details.
     */
    @GetMapping("/configuration")
    public ResponseEntity<Map<String, Object>> getModelConfiguration() {
        try {
            Map<String, Object> config = stochasticXLSTMService.getModelConfiguration();
            return ResponseEntity.ok(config);
            
        } catch (Exception e) {
            logger.error("Error getting model configuration", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get configuration: " + e.getMessage()));
        }
    }
    
    /**
     * Retrain model with all available historical data.
     */
    @PostMapping("/retrain")
    public ResponseEntity<Map<String, Object>> retrainModel(@RequestBody List<MarketData> allHistoricalData) {
        try {
            if (allHistoricalData == null || allHistoricalData.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Historical data is required for retraining"));
            }
            
            logger.info("Received retraining request with {} data points", allHistoricalData.size());
            
            // Start retraining asynchronously
            CompletableFuture<Void> retrainingFuture = stochasticXLSTMService.retrainModel(allHistoricalData);
            
            return ResponseEntity.accepted()
                    .body(Map.of(
                            "message", "Retraining started",
                            "data_points", allHistoricalData.size(),
                            "status", "retraining_in_progress"
                    ));
            
        } catch (Exception e) {
            logger.error("Error starting model retraining", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to start retraining: " + e.getMessage()));
        }
    }
    
    /**
     * Health check endpoint for the StochasticXLSTM service.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            boolean isHealthy = stochasticXLSTMService != null;
            boolean isModelReady = isHealthy && stochasticXLSTMService.isModelReady();
            
            Map<String, Object> health = Map.of(
                    "status", isHealthy ? "UP" : "DOWN",
                    "model_ready", isModelReady,
                    "service_available", isHealthy,
                    "timestamp", java.time.LocalDateTime.now()
            );
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            logger.error("Error during health check", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "status", "DOWN",
                            "error", e.getMessage(),
                            "timestamp", java.time.LocalDateTime.now()
                    ));
        }
    }
    
    /**
     * Request DTO for model updates.
     */
    public static class UpdateModelRequest {
        private MarketData marketData;
        private double actualOutcome;
        
        public UpdateModelRequest() {}
        
        public UpdateModelRequest(MarketData marketData, double actualOutcome) {
            this.marketData = marketData;
            this.actualOutcome = actualOutcome;
        }
        
        public MarketData getMarketData() { return marketData; }
        public void setMarketData(MarketData marketData) { this.marketData = marketData; }
        
        public double getActualOutcome() { return actualOutcome; }
        public void setActualOutcome(double actualOutcome) { this.actualOutcome = actualOutcome; }
    }
}