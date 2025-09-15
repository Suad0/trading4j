package com.quanttrading.controller;

import com.quanttrading.dto.ErrorResponse;
import com.quanttrading.ml.MLModelMetrics;
import com.quanttrading.ml.MLPrediction;
import com.quanttrading.service.MLModelService;
import com.quanttrading.service.MarketDataService;
import com.quanttrading.model.MarketData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for ML model management and predictions.
 * Provides comprehensive endpoints for ML operations in the trading system.
 */
@RestController
@RequestMapping("/api/ml")
@CrossOrigin(origins = "*")
public class MLController {
    
    private static final Logger logger = LoggerFactory.getLogger(MLController.class);
    
    private final MLModelService mlModelService;
    private final MarketDataService marketDataService;
    
    @Autowired
    public MLController(MLModelService mlModelService, MarketDataService marketDataService) {
        this.mlModelService = mlModelService;
        this.marketDataService = marketDataService;
    }
    
    /**
     * Initialize ML models for a symbol.
     */
    @PostMapping("/models/{symbol}/initialize")
    public ResponseEntity<?> initializeModel(@PathVariable String symbol) {
        try {
            logger.info("Initializing ML model for symbol: {}", symbol);
            
            boolean success = mlModelService.initializeStrategy(symbol);
            
            if (success) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "ML model initialized successfully for " + symbol);
                response.put("symbol", symbol);
                response.put("timestamp", java.time.LocalDateTime.now());
                
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("ML_INIT_FAILED", "Failed to initialize ML model for " + symbol));
            }
            
        } catch (Exception e) {
            logger.error("Error initializing ML model for {}: {}", symbol, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "Internal server error: " + e.getMessage()));
        }
    }
    
    /**
     * Train ML models for a symbol.
     */
    @PostMapping("/models/{symbol}/train")
    public ResponseEntity<?> trainModel(@PathVariable String symbol) {
        try {
            logger.info("Starting ML model training for symbol: {}", symbol);
            
            // Start training asynchronously
            CompletableFuture<Boolean> trainingFuture = mlModelService.trainModels(symbol);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "ML model training started for " + symbol);
            response.put("symbol", symbol);
            response.put("training_status", "in_progress");
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.accepted().body(response);
            
        } catch (Exception e) {
            logger.error("Error starting ML model training for {}: {}", symbol, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("TRAINING_ERROR", "Failed to start training: " + e.getMessage()));
        }
    }
    
    /**
     * Get ML prediction for a symbol.
     */
    @GetMapping("/predictions/{symbol}")
    public ResponseEntity<?> getPrediction(@PathVariable String symbol) {
        try {
            logger.debug("Getting ML prediction for symbol: {}", symbol);
            
            // Create mock market data for demonstration
            // In production, this would fetch current market data
            MarketData currentData = createMockMarketData(symbol);
            
            // Get ML prediction
            MLPrediction prediction = mlModelService.getPrediction(symbol, currentData);
            
            if (prediction != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("symbol", symbol);
                response.put("prediction", formatPrediction(prediction));
                response.put("timestamp", prediction.getTimestamp());
                response.put("interpretation", interpretPrediction(prediction));
                
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("NO_PREDICTION", "No ML prediction available for " + symbol + 
                                          ". Model may not be trained yet."));
            }
            
        } catch (Exception e) {
            logger.error("Error getting ML prediction for {}: {}", symbol, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("PREDICTION_ERROR", "Failed to get prediction: " + e.getMessage()));
        }
    }
    
    /**
     * Get model metrics for a symbol.
     */
    @GetMapping("/models/{symbol}/metrics")
    public ResponseEntity<?> getModelMetrics(@PathVariable String symbol) {
        try {
            logger.debug("Getting ML model metrics for symbol: {}", symbol);
            
            MLModelMetrics metrics = mlModelService.getModelMetrics(symbol);
            
            if (metrics != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("symbol", symbol);
                response.put("metrics", formatMetrics(metrics));
                response.put("timestamp", java.time.LocalDateTime.now());
                
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("NO_METRICS", "No model metrics available for " + symbol));
            }
            
        } catch (Exception e) {
            logger.error("Error getting model metrics for {}: {}", symbol, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("METRICS_ERROR", "Failed to get metrics: " + e.getMessage()));
        }
    }
    
    /**
     * Get model status for a symbol.
     */
    @GetMapping("/models/{symbol}/status")
    public ResponseEntity<?> getModelStatus(@PathVariable String symbol) {
        try {
            logger.debug("Getting ML model status for symbol: {}", symbol);
            
            MLModelService.ModelStatus status = mlModelService.getModelStatus(symbol);
            
            if (status != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("symbol", symbol);
                response.put("status", formatModelStatus(status));
                response.put("timestamp", java.time.LocalDateTime.now());
                
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("NO_MODEL", "No model found for " + symbol));
            }
            
        } catch (Exception e) {
            logger.error("Error getting model status for {}: {}", symbol, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("STATUS_ERROR", "Failed to get status: " + e.getMessage()));
        }
    }
    
    /**
     * Get status for all ML models.
     */
    @GetMapping("/models/status")
    public ResponseEntity<?> getAllModelStatuses() {
        try {
            logger.debug("Getting all ML model statuses");
            
            Map<String, MLModelService.ModelStatus> statuses = mlModelService.getAllModelStatuses();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("models", formatAllModelStatuses(statuses));
            response.put("total_models", statuses.size());
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting all model statuses: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("STATUS_ERROR", "Failed to get statuses: " + e.getMessage()));
        }
    }
    
    /**
     * Get comprehensive ML analysis for a symbol.
     */
    @GetMapping("/analysis/{symbol}")
    public ResponseEntity<?> getMLAnalysis(@PathVariable String symbol) {
        try {
            logger.debug("Getting comprehensive ML analysis for symbol: {}", symbol);
            
            // Create mock market data for demonstration
            MarketData currentData = createMockMarketData(symbol);
            
            // Get various ML insights
            MLPrediction prediction = mlModelService.getPrediction(symbol, currentData);
            MLModelMetrics metrics = mlModelService.getModelMetrics(symbol);
            MLModelService.ModelStatus status = mlModelService.getModelStatus(symbol);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("symbol", symbol);
            response.put("current_price", currentData.getClose());
            response.put("prediction", prediction != null ? formatPrediction(prediction) : null);
            response.put("model_metrics", metrics != null ? formatMetrics(metrics) : null);
            response.put("model_status", status != null ? formatModelStatus(status) : null);
            response.put("analysis_timestamp", java.time.LocalDateTime.now());
            
            // Add interpretation
            if (prediction != null) {
                response.put("interpretation", interpretPrediction(prediction));
                response.put("trading_recommendation", generateTradingRecommendation(prediction));
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting ML analysis for {}: {}", symbol, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("ANALYSIS_ERROR", "Failed to get analysis: " + e.getMessage()));
        }
    }
    
    /**
     * Health check endpoint for ML system.
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        try {
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("ml_service", "operational");
            health.put("timestamp", java.time.LocalDateTime.now());
            
            Map<String, MLModelService.ModelStatus> statuses = mlModelService.getAllModelStatuses();
            health.put("active_models", statuses.size());
            
            long trainedModels = statuses.values().stream()
                    .mapToLong(status -> status.isModelTrained() ? 1 : 0)
                    .sum();
            health.put("trained_models", trainedModels);
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            logger.error("Error in ML health check: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("HEALTH_ERROR", "ML service health check failed"));
        }
    }
    
    // Helper methods for formatting responses
    
    private Map<String, Object> formatPrediction(MLPrediction prediction) {
        Map<String, Object> formatted = new HashMap<>();
        formatted.put("direction", prediction.getPriceDirection());
        formatted.put("confidence", Math.round(prediction.getConfidence() * 10000.0) / 100.0); // Percentage
        formatted.put("model_name", prediction.getModelName());
        formatted.put("prediction_type", prediction.getType());
        formatted.put("timestamp", prediction.getTimestamp());
        
        // Add specific prediction details
        if (prediction.getPredictions() != null) {
            formatted.put("details", prediction.getPredictions());
        }
        
        // Add feature importance
        if (prediction.getFeatureImportance() != null) {
            formatted.put("feature_importance", prediction.getFeatureImportance());
        }
        
        return formatted;
    }
    
    private Map<String, Object> formatMetrics(MLModelMetrics metrics) {
        Map<String, Object> formatted = new HashMap<>();
        formatted.put("model_name", metrics.getModelName());
        formatted.put("accuracy", Math.round(metrics.getAccuracy() * 10000.0) / 100.0); // Percentage
        formatted.put("total_predictions", metrics.getTotalPredictions());
        formatted.put("correct_predictions", metrics.getCorrectPredictions());
        formatted.put("last_training_time", metrics.getLastTrainingTime());
        formatted.put("training_data_size", metrics.getTrainingDataSize());
        
        return formatted;
    }
    
    private Map<String, Object> formatModelStatus(MLModelService.ModelStatus status) {
        Map<String, Object> formatted = new HashMap<>();
        formatted.put("symbol", status.getSymbol());
        formatted.put("model_trained", status.isModelTrained());
        formatted.put("training_in_progress", status.isTrainingInProgress());
        formatted.put("last_training_attempt", status.getLastTrainingAttempt());
        formatted.put("last_successful_training", status.getLastSuccessfulTraining());
        formatted.put("training_data_size", status.getTrainingDataSize());
        formatted.put("model_confidence", Math.round(status.getModelConfidence() * 10000.0) / 100.0);
        formatted.put("last_error", status.getLastError());
        
        return formatted;
    }
    
    private Map<String, Object> formatAllModelStatuses(Map<String, MLModelService.ModelStatus> statuses) {
        Map<String, Object> formatted = new HashMap<>();
        
        for (Map.Entry<String, MLModelService.ModelStatus> entry : statuses.entrySet()) {
            formatted.put(entry.getKey(), formatModelStatus(entry.getValue()));
        }
        
        return formatted;
    }
    
    private Map<String, Object> interpretPrediction(MLPrediction prediction) {
        Map<String, Object> interpretation = new HashMap<>();
        
        // Direction interpretation
        MLPrediction.Direction direction = prediction.getPriceDirection();
        if (direction != null) {
            String directionText = switch (direction) {
                case UP -> "The ML model predicts upward price movement";
                case DOWN -> "The ML model predicts downward price movement";
                case SIDEWAYS -> "The ML model predicts sideways price movement";
            };
            interpretation.put("direction", directionText);
        }
        
        // Confidence interpretation
        double confidence = prediction.getConfidence();
        String confidenceText;
        if (confidence >= 0.8) {
            confidenceText = "Very High Confidence";
        } else if (confidence >= 0.7) {
            confidenceText = "High Confidence";
        } else if (confidence >= 0.6) {
            confidenceText = "Medium Confidence";
        } else if (confidence >= 0.5) {
            confidenceText = "Low Confidence";
        } else {
            confidenceText = "Very Low Confidence";
        }
        interpretation.put("confidence_level", confidenceText);
        interpretation.put("confidence_score", Math.round(confidence * 10000.0) / 100.0);
        
        // Market regime interpretation
        MLPrediction.MarketRegime regime = prediction.getMarketRegime();
        if (regime != null) {
            String regimeText = switch (regime) {
                case BULL -> "Bull market conditions detected - favorable for long positions";
                case BEAR -> "Bear market conditions detected - consider defensive strategies";
                case SIDEWAYS -> "Sideways market - range-bound trading likely";
                case VOLATILE -> "High volatility detected - exercise caution";
            };
            interpretation.put("market_regime", regimeText);
        }
        
        return interpretation;
    }
    
    private String generateTradingRecommendation(MLPrediction prediction) {
        double confidence = prediction.getConfidence();
        MLPrediction.Direction direction = prediction.getPriceDirection();
        MLPrediction.MarketRegime regime = prediction.getMarketRegime();
        
        if (confidence < 0.6) {
            return "Low confidence - consider waiting for clearer signals or reducing position size";
        }
        
        if (regime == MLPrediction.MarketRegime.VOLATILE && confidence < 0.8) {
            return "High volatility detected - reduce position size and use wider stop losses";
        }
        
        if (direction == MLPrediction.Direction.UP && confidence >= 0.7) {
            return "Strong bullish signal - consider long position with appropriate risk management";
        } else if (direction == MLPrediction.Direction.DOWN && confidence >= 0.7) {
            return "Strong bearish signal - consider short position or exit long positions";
        } else if (direction == MLPrediction.Direction.SIDEWAYS) {
            return "Sideways movement expected - consider range trading strategies or wait for breakout";
        } else {
            return "Mixed signals - exercise caution and consider smaller position sizes";
        }
    }
    
    /**
     * Create mock market data for demonstration purposes.
     * In production, this would fetch real market data.
     */
    private MarketData createMockMarketData(String symbol) {
        MarketData data = new MarketData();
        data.setSymbol(symbol);
        data.setOpen(java.math.BigDecimal.valueOf(150.00));
        data.setHigh(java.math.BigDecimal.valueOf(152.50));
        data.setLow(java.math.BigDecimal.valueOf(149.75));
        data.setPrice(java.math.BigDecimal.valueOf(151.25)); // Close price
        data.setVolume(java.math.BigDecimal.valueOf(1000000));
        data.setTimestamp(java.time.LocalDateTime.now());
        return data;
    }
}