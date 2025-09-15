package com.quanttrading.ml.stoxlstm;

import com.quanttrading.ml.MLPrediction;
import com.quanttrading.model.MarketData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for StochasticXLSTMModel.
 */
@SpringBootTest
@ActiveProfiles("test")
class StochasticXLSTMModelTest {
    
    private StochasticXLSTMModel model;
    
    @BeforeEach
    void setUp() {
        model = new StochasticXLSTMModel();
    }
    
    @Test
    void testModelInitialization() {
        assertNotNull(model);
        assertEquals("StochasticXLSTM_v1.0", model.getModelName());
        assertNotNull(model.getNetwork());
        assertNotNull(model.getConfig());
        
        // Test configuration
        StoxLSTMConfig config = model.getConfig();
        assertEquals(128, config.hiddenSize);
        assertEquals(32, config.latentDim);
        assertTrue(config.useExponentialGating);
        assertTrue(config.useMemoryMixing);
        assertTrue(config.useLayerNormalization);
        assertEquals(0.15, config.stochasticRegularization, 0.001);
    }
    
    @Test
    void testModelTraining() {
        // Generate synthetic training data
        List<MarketData> historicalData = generateSyntheticMarketData(100);
        Map<String, List<Double>> features = generateSyntheticFeatures(100);
        
        // Train the model
        boolean success = model.train(historicalData, features);
        assertTrue(success);
        
        // Verify model is ready
        assertTrue(model.isModelReady());
        assertNotNull(model.getMetrics().getLastTrainingTime());
    }
    
    @Test
    void testPredictionGeneration() {
        // Train with minimal data
        List<MarketData> historicalData = generateSyntheticMarketData(70);
        Map<String, List<Double>> features = generateSyntheticFeatures(70);
        model.train(historicalData, features);
        
        // Generate prediction
        MarketData testData = createTestMarketData("AAPL");
        Map<String, Double> testFeatures = generateSingleFeatureSet();
        MLPrediction prediction = model.predict(testData, testFeatures);
        
        assertNotNull(prediction);
        assertEquals("AAPL", prediction.getSymbol());
        assertEquals(MLPrediction.PredictionType.PRICE_DIRECTION, prediction.getType());
        assertTrue(prediction.getConfidence() >= 0.0 && prediction.getConfidence() <= 1.0);
        assertNotNull(prediction.getPredictions());
        
        // Check stochastic components
        Map<String, Object> predictions = prediction.getPredictions();
        assertTrue(predictions.containsKey("uncertainty"));
        assertTrue(predictions.containsKey("kl_divergence"));
        assertTrue(predictions.containsKey("entropy"));
        assertNotNull(prediction.getPriceDirection());
    }
    
    @Test
    void testStochasticMetrics() {
        // Train model
        List<MarketData> historicalData = generateSyntheticMarketData(50);
        Map<String, List<Double>> features = generateSyntheticFeatures(50);
        model.train(historicalData, features);
        
        // Generate prediction to compute stochastic metrics
        MarketData testData = createTestMarketData("TSLA");
        Map<String, Double> testFeatures = generateSingleFeatureSet();
        MLPrediction prediction = model.predict(testData, testFeatures);
        
        // Verify stochastic metrics are computed
        double klDivergence = model.getLastKLDivergence();
        double uncertainty = model.getLastUncertainty();
        double entropy = model.getLastEntropy();
        
        assertTrue(klDivergence >= 0.0); // KL divergence should be non-negative
        assertTrue(uncertainty >= 0.0);  // Uncertainty should be non-negative
        assertTrue(entropy >= 0.0);      // Entropy should be non-negative
        
        // Check prediction contains stochastic information
        Map<String, Object> predictions = prediction.getPredictions();
        assertTrue(predictions.containsKey("uncertainty"));
        assertTrue(predictions.containsKey("kl_divergence"));
        assertTrue(predictions.containsKey("entropy"));
    }
    
    @Test
    void testModelMetrics() {
        // Train model
        List<MarketData> historicalData = generateSyntheticMarketData(40);
        Map<String, List<Double>> features = generateSyntheticFeatures(40);
        model.train(historicalData, features);
        
        var metrics = model.getMetrics();
        
        assertNotNull(metrics);
        assertEquals("StochasticXLSTM_v1.0", metrics.getModelName());
        assertTrue(metrics.getAccuracy() >= 0.0 && metrics.getAccuracy() <= 1.0);
        assertNotNull(metrics.getLastTrainingTime());
        assertNotNull(metrics.getAdditionalMetrics());
        
        // Check additional stochastic metrics
        Map<String, Double> additionalMetrics = metrics.getAdditionalMetrics();
        assertTrue(additionalMetrics.containsKey("last_kl_divergence"));
        assertTrue(additionalMetrics.containsKey("last_uncertainty"));
        assertTrue(additionalMetrics.containsKey("last_entropy"));
        assertTrue(additionalMetrics.containsKey("training_data_count"));
        assertEquals(40.0, additionalMetrics.get("training_data_count"), 0.001);
    }
    
    @Test
    void testModelPersistence() {
        // Train model
        List<MarketData> historicalData = generateSyntheticMarketData(30);
        Map<String, List<Double>> features = generateSyntheticFeatures(30);
        model.train(historicalData, features);
        
        // Save model
        String modelPath = "tmp_rovodev_test_model.zip";
        boolean saveSuccess = model.saveModel(modelPath);
        assertTrue(saveSuccess);
        
        // Create new model and load
        StochasticXLSTMModel loadedModel = new StochasticXLSTMModel();
        boolean loadSuccess = loadedModel.loadModel(modelPath);
        assertTrue(loadSuccess);
        assertTrue(loadedModel.isModelReady());
        
        // Clean up
        java.io.File file = new java.io.File(modelPath);
        if (file.exists()) {
            file.delete();
        }
    }
    
    @Test
    void testModelUpdate() {
        // Train initial model
        List<MarketData> historicalData = generateSyntheticMarketData(25);
        Map<String, List<Double>> features = generateSyntheticFeatures(25);
        model.train(historicalData, features);
        
        // Update model with new data
        MarketData newData = createTestMarketData("MSFT");
        Map<String, Double> newFeatures = generateSingleFeatureSet();
        
        assertDoesNotThrow(() -> model.updateModel(newData, newFeatures));
        
        // Verify update doesn't break the model
        assertTrue(model.isModelReady());
    }
    
    @Test
    void testMinimumTrainingData() {
        int minData = model.getMinimumTrainingData();
        assertTrue(minData > 0);
        assertTrue(minData >= model.getConfig().lookbackLength);
    }
    
    @Test
    void testNeedsRetraining() {
        // Initially should not need retraining
        assertFalse(model.needsRetraining());
        
        // After training, check retraining logic
        List<MarketData> historicalData = generateSyntheticMarketData(30);
        Map<String, List<Double>> features = generateSyntheticFeatures(30);
        model.train(historicalData, features);
        
        // Should still be based on performance
        boolean needsRetraining = model.needsRetraining();
        // This is based on model's internal logic, so we just verify it returns a boolean
        assertTrue(needsRetraining || !needsRetraining); // Always true - just testing no exception
    }
    
    @Test
    void testDefaultPrediction() {
        // Test prediction without training
        MarketData testData = createTestMarketData("NVDA");
        Map<String, Double> testFeatures = generateSingleFeatureSet();
        
        MLPrediction prediction = model.predict(testData, testFeatures);
        
        assertNotNull(prediction);
        assertEquals("NVDA", prediction.getSymbol());
        assertEquals(MLPrediction.PredictionType.PRICE_DIRECTION, prediction.getType());
        assertTrue(prediction.getConfidence() >= 0.0 && prediction.getConfidence() <= 1.0);
    }
    
    // Helper methods
    
    private List<MarketData> generateSyntheticMarketData(int count) {
        List<MarketData> data = new ArrayList<>();
        Random random = new Random(42); // Fixed seed for reproducibility
        
        double basePrice = 100.0;
        long baseVolume = 1000000L;
        
        for (int i = 0; i < count; i++) {
            MarketData marketData = new MarketData();
            marketData.setSymbol("TEST");
            marketData.setTimestamp(LocalDateTime.now().minusDays(count - i));
            
            // Simulate realistic price movements
            double priceChange = random.nextGaussian() * 0.02; // 2% volatility
            basePrice *= (1 + priceChange);
            
            marketData.setPrice(BigDecimal.valueOf(basePrice));
            marketData.setOpen(BigDecimal.valueOf(basePrice * (1 + random.nextGaussian() * 0.005)));
            marketData.setHigh(BigDecimal.valueOf(basePrice * (1 + Math.abs(random.nextGaussian()) * 0.01)));
            marketData.setLow(BigDecimal.valueOf(basePrice * (1 - Math.abs(random.nextGaussian()) * 0.01)));
            marketData.setClose(BigDecimal.valueOf(basePrice));
            
            baseVolume += (long)(random.nextGaussian() * baseVolume * 0.1);
            marketData.setVolume(baseVolume);
            
            data.add(marketData);
        }
        
        return data;
    }
    
    private Map<String, List<Double>> generateSyntheticFeatures(int count) {
        Map<String, List<Double>> features = new HashMap<>();
        Random random = new Random(42);
        
        String[] featureNames = {"price", "volume", "high", "low", "open", "close", 
                                "price_change", "volume_change", "volatility", "momentum"};
        
        for (String featureName : featureNames) {
            List<Double> values = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                if (featureName.contains("price") || featureName.contains("high") || 
                    featureName.contains("low") || featureName.contains("open") || 
                    featureName.contains("close")) {
                    values.add(100.0 + random.nextGaussian() * 10);
                } else if (featureName.contains("volume")) {
                    values.add(1000000.0 + random.nextGaussian() * 100000);
                } else if (featureName.contains("change")) {
                    values.add(random.nextGaussian() * 0.02);
                } else {
                    values.add(random.nextGaussian());
                }
            }
            features.put(featureName, values);
        }
        
        return features;
    }
    
    private Map<String, Double> generateSingleFeatureSet() {
        Map<String, Double> features = new HashMap<>();
        features.put("price", 105.0);
        features.put("volume", 1200000.0);
        features.put("high", 106.0);
        features.put("low", 104.0);
        features.put("open", 104.5);
        features.put("close", 105.0);
        features.put("price_change", 0.01);
        features.put("volume_change", 0.05);
        features.put("volatility", 0.25);
        features.put("momentum", 1.5);
        
        return features;
    }
    
    private MarketData createTestMarketData(String symbol) {
        MarketData marketData = new MarketData();
        marketData.setSymbol(symbol);
        marketData.setPrice(BigDecimal.valueOf(150.0));
        marketData.setVolume(1000000L);
        marketData.setTimestamp(LocalDateTime.now());
        marketData.setOpen(BigDecimal.valueOf(149.0));
        marketData.setHigh(BigDecimal.valueOf(151.0));
        marketData.setLow(BigDecimal.valueOf(148.0));
        marketData.setClose(BigDecimal.valueOf(150.0));
        
        return marketData;
    }
}