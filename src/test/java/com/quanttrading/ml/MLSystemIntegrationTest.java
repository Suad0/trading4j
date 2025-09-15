package com.quanttrading.ml;

import com.quanttrading.ml.core.FeatureEngineer;
import com.quanttrading.ml.models.EnsembleMLModel;
import com.quanttrading.model.MarketData;
import com.quanttrading.strategy.MLEnhancedTradingStrategy;
import com.quanttrading.strategy.TradingSignal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive integration tests for the ML trading system.
 * Tests the entire ML pipeline from feature extraction to signal generation.
 */
public class MLSystemIntegrationTest {
    
    private static final Logger logger = LoggerFactory.getLogger(MLSystemIntegrationTest.class);
    
    private FeatureEngineer featureEngineer;
    private EnsembleMLModel mlModel;
    private MLEnhancedTradingStrategy mlStrategy;
    
    @BeforeEach
    void setUp() {
        featureEngineer = new FeatureEngineer();
        mlModel = new EnsembleMLModel();
        mlStrategy = new MLEnhancedTradingStrategy();
        
        logger.info("Set up ML system integration test");
    }
    
    @Test
    void testFeatureEngineering() {
        logger.info("Testing feature engineering pipeline...");
        
        // Generate sample market data
        List<MarketData> sampleData = generateSampleMarketData("AAPL", 100);
        
        // Add data to feature engineer
        for (MarketData data : sampleData) {
            featureEngineer.addMarketData(data.getSymbol(), data);
        }
        
        // Extract features from the last data point
        MarketData lastData = sampleData.get(sampleData.size() - 1);
        Map<String, Double> features = featureEngineer.extractFeatures("AAPL", lastData);
        
        // Verify features are extracted
        assertNotNull(features);
        assertFalse(features.isEmpty());
        
        logger.info("Extracted {} features: {}", features.size(), features.keySet());
        
        // Verify specific feature categories
        assertTrue(features.keySet().stream().anyMatch(key -> key.contains("sma")), 
                  "Should contain moving average features");
        assertTrue(features.keySet().stream().anyMatch(key -> key.contains("rsi")), 
                  "Should contain RSI features");
        assertTrue(features.keySet().stream().anyMatch(key -> key.contains("volume")), 
                  "Should contain volume features");
        assertTrue(features.keySet().stream().anyMatch(key -> key.contains("volatility")), 
                  "Should contain volatility features");
        
        // Verify feature values are reasonable
        for (Map.Entry<String, Double> entry : features.entrySet()) {
            Double value = entry.getValue();
            assertNotNull(value, "Feature " + entry.getKey() + " should not be null");
            assertFalse(Double.isNaN(value), "Feature " + entry.getKey() + " should not be NaN");
            assertFalse(Double.isInfinite(value), "Feature " + entry.getKey() + " should not be infinite");
        }
        
        logger.info("✅ Feature engineering test passed");
    }
    
    @Test
    void testMLModelTraining() {
        logger.info("Testing ML model training...");
        
        // Generate training data
        List<MarketData> trainingData = generateSampleMarketData("AAPL", 150);
        
        // Prepare features for training
        Map<String, List<Double>> features = prepareTrainingFeatures(trainingData);
        
        // Train the model
        boolean trained = mlModel.train(trainingData, features);
        
        assertTrue(trained, "ML model should train successfully");
        assertTrue(mlModel.isModelReady(), "ML model should be ready after training");
        
        // Verify model metrics
        MLModelMetrics metrics = mlModel.getMetrics();
        assertNotNull(metrics, "Model metrics should be available after training");
        assertEquals("Ensemble_ML_Model", metrics.getModelName());
        
        logger.info("✅ ML model training test passed");
    }
    
    @Test
    void testMLPredictions() {
        logger.info("Testing ML predictions...");
        
        // Generate training data and train model
        List<MarketData> trainingData = generateSampleMarketData("AAPL", 150);
        Map<String, List<Double>> features = prepareTrainingFeatures(trainingData);
        
        boolean trained = mlModel.train(trainingData, features);
        assertTrue(trained, "Model should be trained");
        
        // Test prediction
        MarketData currentData = generateSampleMarketData("AAPL", 1).get(0);
        
        // Add data to feature engineer
        for (MarketData data : trainingData) {
            featureEngineer.addMarketData(data.getSymbol(), data);
        }
        featureEngineer.addMarketData(currentData.getSymbol(), currentData);
        
        // Extract features and make prediction
        Map<String, Double> currentFeatures = featureEngineer.extractFeatures("AAPL", currentData);
        MLPrediction prediction = mlModel.predict(currentData, currentFeatures);
        
        assertNotNull(prediction, "Prediction should not be null");
        assertNotNull(prediction.getSymbol());
        assertEquals("AAPL", prediction.getSymbol());
        assertTrue(prediction.getConfidence() >= 0.0 && prediction.getConfidence() <= 1.0,
                  "Confidence should be between 0 and 1");
        assertNotNull(prediction.getPriceDirection());
        
        logger.info("Generated prediction: {} with confidence: {:.2f}", 
                   prediction.getPriceDirection(), prediction.getConfidence());
        
        logger.info("✅ ML predictions test passed");
    }
    
    @Test
    void testMLTradingStrategy() {
        logger.info("Testing ML trading strategy...");
        
        // Generate market data
        List<MarketData> marketData = generateSampleMarketData("AAPL", 120);
        
        // Train the strategy
        boolean trained = mlStrategy.trainModel(marketData);
        assertTrue(trained, "ML strategy should train successfully");
        
        // Test signal generation
        MarketData currentData = generateSampleMarketData("AAPL", 1).get(0);
        
        // Analyze market data
        List<TradingSignal> signals = mlStrategy.analyze(currentData);
        
        assertNotNull(signals, "Signals should not be null");
        
        // Verify signal quality if any signals generated
        for (TradingSignal signal : signals) {
            assertNotNull(signal.getSymbol());
            assertNotNull(signal.getTradeType());
            assertTrue(signal.getConfidence() >= 0.0 && signal.getConfidence() <= 1.0);
            assertNotNull(signal.getReason());
            assertTrue(signal.getQuantity().compareTo(BigDecimal.ZERO) > 0);
            
            logger.info("Generated signal: {} {} {} at {} (confidence: {:.2f})", 
                       signal.getTradeType(), signal.getQuantity(), signal.getSymbol(),
                       signal.getTargetPrice(), signal.getConfidence());
        }
        
        logger.info("✅ ML trading strategy test passed");
    }
    
    @Test
    void testEndToEndMLWorkflow() {
        logger.info("Testing end-to-end ML workflow...");
        
        try {
            String symbol = "AAPL";
            
            // 1. Generate comprehensive market data
            List<MarketData> historicalData = generateSampleMarketData(symbol, 150);
            
            // 2. Initialize and train ML strategy
            boolean trained = mlStrategy.trainModel(historicalData);
            assertTrue(trained, "ML strategy training should succeed");
            
            // 3. Process new market data
            for (int i = 0; i < 10; i++) {
                MarketData newData = generateSampleMarketData(symbol, 1).get(0);
                
                // Update model with new data
                mlStrategy.updateModel(newData);
                
                // Generate trading signals
                List<TradingSignal> signals = mlStrategy.analyze(newData);
                assertNotNull(signals, "Signals should not be null");
                
                // Log results
                if (!signals.isEmpty()) {
                    logger.info("Day {}: Generated {} signals", i + 1, signals.size());
                } else {
                    logger.info("Day {}: No signals generated", i + 1);
                }
            }
            
            // 4. Verify model performance tracking
            double modelConfidence = mlStrategy.getModelConfidence();
            assertTrue(modelConfidence >= 0.0 && modelConfidence <= 1.0,
                      "Model confidence should be valid");
            
            // 5. Test model persistence
            String testModelPath = "./test_models/test_ensemble_model";
            boolean saved = mlStrategy.saveModel(testModelPath);
            
            if (saved) {
                logger.info("Model saved successfully");
                
                // Create new strategy and load model
                MLEnhancedTradingStrategy newStrategy = new MLEnhancedTradingStrategy();
                boolean loaded = newStrategy.loadModel(testModelPath);
                
                if (loaded) {
                    logger.info("Model loaded successfully");
                    assertTrue(newStrategy.getModelConfidence() >= 0.0);
                }
            }
            
            logger.info("✅ End-to-end ML workflow test passed");
            
        } catch (Exception e) {
            logger.error("Error in end-to-end ML workflow test", e);
            fail("End-to-end workflow should not throw exceptions: " + e.getMessage());
        }
    }
    
    @Test
    void testMLSystemPerformance() {
        logger.info("Testing ML system performance...");
        
        long startTime = System.currentTimeMillis();
        
        // Generate large dataset
        List<MarketData> largeDataset = generateSampleMarketData("AAPL", 500);
        
        // Test feature extraction performance
        long featureStartTime = System.currentTimeMillis();
        for (MarketData data : largeDataset) {
            featureEngineer.addMarketData(data.getSymbol(), data);
        }
        
        MarketData lastData = largeDataset.get(largeDataset.size() - 1);
        Map<String, Double> features = featureEngineer.extractFeatures("AAPL", lastData);
        long featureEndTime = System.currentTimeMillis();
        
        assertFalse(features.isEmpty(), "Features should be extracted");
        
        // Test model training performance
        long trainingStartTime = System.currentTimeMillis();
        Map<String, List<Double>> trainingFeatures = prepareTrainingFeatures(largeDataset);
        boolean trained = mlModel.train(largeDataset, trainingFeatures);
        long trainingEndTime = System.currentTimeMillis();
        
        assertTrue(trained, "Model should train successfully");
        
        // Test prediction performance
        long predictionStartTime = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            MLPrediction prediction = mlModel.predict(lastData, features);
            assertNotNull(prediction, "Prediction should not be null");
        }
        long predictionEndTime = System.currentTimeMillis();
        
        long totalTime = System.currentTimeMillis() - startTime;
        
        logger.info("Performance Results:");
        logger.info("  Feature extraction: {} ms", featureEndTime - featureStartTime);
        logger.info("  Model training: {} ms", trainingEndTime - trainingStartTime);
        logger.info("  100 predictions: {} ms", predictionEndTime - predictionStartTime);
        logger.info("  Total time: {} ms", totalTime);
        
        // Performance assertions
        assertTrue(featureEndTime - featureStartTime < 5000, "Feature extraction should be fast");
        assertTrue(predictionEndTime - predictionStartTime < 1000, "Predictions should be fast");
        
        logger.info("✅ ML system performance test passed");
    }
    
    /**
     * Generate sample market data for testing.
     */
    private List<MarketData> generateSampleMarketData(String symbol, int count) {
        List<MarketData> data = new ArrayList<>();
        Random random = new Random(42); // Fixed seed for reproducible tests
        
        double basePrice = 150.0;
        double price = basePrice;
        LocalDateTime timestamp = LocalDateTime.now().minusDays(count);
        
        for (int i = 0; i < count; i++) {
            // Simulate realistic price movement
            double trend = 0.0005; // Slight upward trend
            double noise = random.nextGaussian() * 0.02; // 2% volatility
            double priceChange = trend + noise;
            
            price = price * (1 + priceChange);
            
            // Create market data
            MarketData marketData = new MarketData();
            marketData.setSymbol(symbol);
            marketData.setTimestamp(timestamp.plusDays(i));
            
            // OHLC data with realistic relationships
            double open = price * (1 + random.nextGaussian() * 0.005);
            double high = Math.max(open, price) * (1 + Math.abs(random.nextGaussian()) * 0.01);
            double low = Math.min(open, price) * (1 - Math.abs(random.nextGaussian()) * 0.01);
            
            marketData.setOpen(BigDecimal.valueOf(Math.round(open * 100.0) / 100.0));
            marketData.setHigh(BigDecimal.valueOf(Math.round(high * 100.0) / 100.0));
            marketData.setLow(BigDecimal.valueOf(Math.round(low * 100.0) / 100.0));
            marketData.setPrice(BigDecimal.valueOf(Math.round(price * 100.0) / 100.0));
            
            // Volume with some correlation to price movement
            long baseVolume = 1000000;
            long volumeVariation = (long) (baseVolume * Math.abs(priceChange) * 5);
            long volume = baseVolume + volumeVariation + (long) (random.nextGaussian() * 200000);
            marketData.setVolume(BigDecimal.valueOf(Math.max(100000, volume)));
            
            data.add(marketData);
        }
        
        return data;
    }
    
    /**
     * Prepare training features from historical data.
     */
    private Map<String, List<Double>> prepareTrainingFeatures(List<MarketData> data) {
        Map<String, List<Double>> features = new HashMap<>();
        
        for (MarketData md : data) {
            featureEngineer.addMarketData(md.getSymbol(), md);
            Map<String, Double> currentFeatures = featureEngineer.extractFeatures(md.getSymbol(), md);
            
            for (Map.Entry<String, Double> entry : currentFeatures.entrySet()) {
                features.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(entry.getValue());
            }
        }
        
        return features;
    }
}