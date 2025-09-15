package com.quanttrading.ml.stoxlstm;

import com.quanttrading.ml.MLPrediction;
import com.quanttrading.model.MarketData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Demo class to showcase the Stochastic XLSTM implementation.
 * This demonstrates the complete workflow of training and prediction.
 */
public class StochasticXLSTMDemo {
    
    private static final Logger logger = LoggerFactory.getLogger(StochasticXLSTMDemo.class);
    
    public static void main(String[] args) {
        logger.info("Starting Stochastic XLSTM Demo");
        
        try {
            // 1. Initialize the model
            StochasticXLSTMModel model = new StochasticXLSTMModel();
            logger.info("Model initialized: {}", model.getModelName());
            
            // 2. Generate synthetic training data
            List<MarketData> trainingData = generateSyntheticMarketData(200);
            Map<String, List<Double>> features = generateSyntheticFeatures(200);
            logger.info("Generated {} training samples", trainingData.size());
            
            // 3. Train the model
            logger.info("Starting model training...");
            boolean trainingSuccess = model.train(trainingData, features);
            logger.info("Training completed: {}", trainingSuccess ? "SUCCESS" : "FAILED");
            
            if (!trainingSuccess) {
                logger.error("Training failed, cannot proceed with demo");
                return;
            }
            
            // 4. Display model metrics
            displayModelMetrics(model);
            
            // 5. Generate predictions
            logger.info("Generating predictions...");
            List<MarketData> testData = generateSyntheticMarketData(10);
            
            for (MarketData data : testData) {
                Map<String, Double> testFeatures = generateSingleFeatureSet(data);
                MLPrediction prediction = model.predict(data, testFeatures);
                
                displayPrediction(data, prediction);
            }
            
            // 6. Demonstrate model persistence
            demonstrateModelPersistence(model);
            
            logger.info("Stochastic XLSTM Demo completed successfully");
            
        } catch (Exception e) {
            logger.error("Demo failed with error", e);
        }
    }
    
    private static void displayModelMetrics(StochasticXLSTMModel model) {
        logger.info("=== Model Metrics ===");
        var metrics = model.getMetrics();
        logger.info("Model Name: {}", metrics.getModelName());
        logger.info("Accuracy: {:.3f}", metrics.getAccuracy());
        logger.info("Training Time: {}", metrics.getLastTrainingTime());
        
        Map<String, Double> additionalMetrics = metrics.getAdditionalMetrics();
        logger.info("KL Divergence: {:.6f}", additionalMetrics.get("last_kl_divergence"));
        logger.info("Uncertainty: {:.6f}", additionalMetrics.get("last_uncertainty"));
        logger.info("Entropy: {:.6f}", additionalMetrics.get("last_entropy"));
        logger.info("Network Score: {:.6f}", additionalMetrics.get("network_score"));
    }
    
    private static void displayPrediction(MarketData data, MLPrediction prediction) {
        logger.info("=== Prediction for {} ===", data.getSymbol());
        logger.info("Current Price: ${:.2f}", data.getPrice().doubleValue());
        logger.info("Direction: {}", prediction.getPriceDirection());
        logger.info("Confidence: {:.3f}", prediction.getConfidence());
        
        Map<String, Object> predictions = prediction.getPredictions();
        logger.info("Prob UP: {:.3f}", predictions.get("prob_up"));
        logger.info("Prob DOWN: {:.3f}", predictions.get("prob_down"));
        logger.info("Prob SIDEWAYS: {:.3f}", predictions.get("prob_sideways"));
        logger.info("Uncertainty: {:.6f}", predictions.get("uncertainty"));
        logger.info("Target Price: ${:.2f}", ((BigDecimal)predictions.get("price_target")).doubleValue());
        logger.info("---");
    }
    
    private static void demonstrateModelPersistence(StochasticXLSTMModel model) {
        logger.info("=== Demonstrating Model Persistence ===");
        
        String modelPath = "tmp_rovodev_demo_model.zip";
        
        // Save model
        boolean saveSuccess = model.saveModel(modelPath);
        logger.info("Model save: {}", saveSuccess ? "SUCCESS" : "FAILED");
        
        if (saveSuccess) {
            // Load model
            StochasticXLSTMModel loadedModel = new StochasticXLSTMModel();
            boolean loadSuccess = loadedModel.loadModel(modelPath);
            logger.info("Model load: {}", loadSuccess ? "SUCCESS" : "FAILED");
            
            if (loadSuccess) {
                logger.info("Loaded model is ready: {}", loadedModel.isModelReady());
                
                // Test prediction with loaded model
                MarketData testData = createTestMarketData("DEMO", 100.0);
                Map<String, Double> testFeatures = generateSingleFeatureSet(testData);
                MLPrediction prediction = loadedModel.predict(testData, testFeatures);
                logger.info("Loaded model prediction: {} (confidence: {:.3f})", 
                           prediction.getPriceDirection(), prediction.getConfidence());
            }
            
            // Clean up
            java.io.File file = new java.io.File(modelPath);
            if (file.exists()) {
                boolean deleted = file.delete();
                logger.info("Cleanup: {}", deleted ? "SUCCESS" : "FAILED");
            }
        }
    }
    
    private static List<MarketData> generateSyntheticMarketData(int count) {
        List<MarketData> data = new ArrayList<>();
        Random random = new Random(42);
        
        double basePrice = 100.0;
        long baseVolume = 1000000L;
        
        for (int i = 0; i < count; i++) {
            // Simulate realistic price movements with trend and volatility
            double trend = 0.0001; // Slight upward trend
            double volatility = 0.02; // 2% daily volatility
            double priceChange = trend + random.nextGaussian() * volatility;
            basePrice *= (1 + priceChange);
            
            MarketData marketData = createTestMarketData("SYN" + (i % 5), basePrice);
            marketData.setTimestamp(LocalDateTime.now().minusDays(count - i));
            
            // Simulate volume changes
            baseVolume += (long)(random.nextGaussian() * baseVolume * 0.1);
            marketData.setVolume(BigDecimal.valueOf(Math.max(100000L, baseVolume)));
            
            data.add(marketData);
        }
        
        return data;
    }
    
    private static Map<String, List<Double>> generateSyntheticFeatures(int count) {
        Map<String, List<Double>> features = new HashMap<>();
        Random random = new Random(42);
        
        String[] featureNames = {"price", "volume", "high", "low", "open", "close", 
                                "price_change", "volume_change", "volatility", "momentum"};
        
        for (String featureName : featureNames) {
            List<Double> values = new ArrayList<>();
            
            double baseValue = getBaseValue(featureName);
            double volatility = getVolatility(featureName);
            
            for (int i = 0; i < count; i++) {
                if (featureName.contains("change")) {
                    values.add(random.nextGaussian() * volatility);
                } else {
                    double trend = random.nextGaussian() * 0.001; // Small trend
                    baseValue *= (1 + trend);
                    values.add(baseValue + random.nextGaussian() * volatility);
                }
            }
            features.put(featureName, values);
        }
        
        return features;
    }
    
    private static double getBaseValue(String featureName) {
        switch (featureName) {
            case "price":
            case "high":
            case "low":
            case "open":
            case "close":
                return 100.0;
            case "volume":
                return 1000000.0;
            case "volatility":
                return 0.2;
            case "momentum":
                return 1.0;
            default:
                return 0.0;
        }
    }
    
    private static double getVolatility(String featureName) {
        switch (featureName) {
            case "price":
            case "high":
            case "low":
            case "open":
            case "close":
                return 2.0;
            case "volume":
                return 50000.0;
            case "volatility":
                return 0.05;
            case "momentum":
                return 0.5;
            case "price_change":
            case "volume_change":
                return 0.02;
            default:
                return 0.1;
        }
    }
    
    private static Map<String, Double> generateSingleFeatureSet(MarketData data) {
        Map<String, Double> features = new HashMap<>();
        Random random = new Random();
        
        double price = data.getPrice().doubleValue();
        features.put("price", price);
        features.put("volume", data.getVolume().doubleValue());
        features.put("high", data.getHigh().doubleValue());
        features.put("low", data.getLow().doubleValue());
        features.put("open", data.getOpen().doubleValue());
        features.put("close", data.getClose().doubleValue());
        features.put("price_change", random.nextGaussian() * 0.01);
        features.put("volume_change", random.nextGaussian() * 0.05);
        features.put("volatility", 0.15 + random.nextGaussian() * 0.05);
        features.put("momentum", random.nextGaussian() * 2.0);
        
        return features;
    }
    
    private static MarketData createTestMarketData(String symbol, double price) {
        MarketData marketData = new MarketData();
        marketData.setSymbol(symbol);
        marketData.setPrice(BigDecimal.valueOf(price));
        marketData.setTimestamp(LocalDateTime.now());
        
        // Add some realistic spread
        double spread = price * 0.001; // 0.1% spread
        marketData.setOpen(BigDecimal.valueOf(price - spread / 2));
        marketData.setHigh(BigDecimal.valueOf(price + spread));
        marketData.setLow(BigDecimal.valueOf(price - spread));
        marketData.setPrice(BigDecimal.valueOf(price)); // Price serves as close
        marketData.setVolume(BigDecimal.valueOf(1000000L));
        
        return marketData;
    }
}