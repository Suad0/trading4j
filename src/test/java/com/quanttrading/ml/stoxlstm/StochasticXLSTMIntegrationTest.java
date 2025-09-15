package com.quanttrading.ml.stoxlstm;

import com.quanttrading.ml.MLPrediction;
import com.quanttrading.ml.core.FeatureEngineer;
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
 * Integration test for the complete Stochastic XLSTM implementation.
 */
@SpringBootTest
@ActiveProfiles("test")
class StochasticXLSTMIntegrationTest {
    
    private StochasticXLSTMModel model;
    private StochasticXLSTMService service;
    private FeatureEngineer featureEngineer;
    
    @BeforeEach
    void setUp() {
        featureEngineer = new FeatureEngineer();
        service = new StochasticXLSTMService(featureEngineer);
        model = new StochasticXLSTMModel(12, 64, 50);
    }
    
    @Test
    void testModelInitialization() {
        assertNotNull(model);
        assertEquals("StochasticXLSTM_Model", model.getModelName());
        assertNotNull(model.getNetwork());
        assertNotNull(model.getConfig());
        
        // Test configuration
        StoxLSTMConfig config = model.getConfig();
        assertEquals(64, config.hiddenSize);
        assertEquals(16, config.latentDim);
        assertTrue(config.useExponentialGating);
        assertTrue(config.useMemoryMixing);
        assertTrue(config.useLayerNormalization);
        assertEquals(0.01, config.stochasticRegularization, 0.001);
    }
    
    @Test
    void testTrainingWithSyntheticData() {
        // Generate synthetic training data
        List<Map<String, Double>> features = generateSyntheticFeatures(100);
        List<Double> targets = generateSyntheticTargets(100);
        
        // Train the model
        assertDoesNotThrow(() -> model.train(features, targets));
        
        // Verify model is ready
        assertTrue(model.isReady());
        assertNotNull(model.getMetrics().getLastTrainingTime());
    }
    
    @Test
    void testPredictionGeneration() {
        // Train with minimal data
        List<Map<String, Double>> features = generateSyntheticFeatures(50);
        List<Double> targets = generateSyntheticTargets(50);
        model.train(features, targets);
        
        // Generate prediction
        Map<String, Double> testFeatures = generateSingleFeatureSet();
        MLPrediction prediction = model.predict(testFeatures);
        
        assertNotNull(prediction);
        assertNotNull(prediction.getSignal());
        assertTrue(prediction.getConfidence() >= 0.0 && prediction.getConfidence() <= 1.0);
        assertNotNull(prediction.getPredictions());
        
        // Check stochastic components
        assertTrue(prediction.getPredictions().containsKey("uncertainty"));
        assertTrue(prediction.getPredictions().containsKey("kl_divergence"));
        assertTrue(prediction.getPredictions().containsKey("stochastic_enhancement"));
    }
    
    @Test
    void testStochasticMetrics() {
        // Train model
        List<Map<String, Double>> features = generateSyntheticFeatures(50);
        List<Double> targets = generateSyntheticTargets(50);
        model.train(features, targets);
        
        // Generate prediction to compute stochastic metrics
        Map<String, Double> testFeatures = generateSingleFeatureSet();
        MLPrediction prediction = model.predict(testFeatures);
        
        // Verify stochastic metrics are computed
        double klDivergence = model.getLastKLDivergence();
        double uncertainty = model.getLastUncertainty();
        
        assertTrue(klDivergence >= 0.0); // KL divergence should be non-negative
        assertTrue(uncertainty >= 0.0);  // Uncertainty should be non-negative
        
        // Check prediction contains stochastic information
        Map<String, Double> predictions = prediction.getPredictions();
        assertTrue(predictions.containsKey("uncertainty"));
        assertTrue(predictions.containsKey("kl_divergence"));
        assertEquals(uncertainty, predictions.get("uncertainty"), 0.001);
    }
    
    @Test
    void testModelMetrics() {
        // Train model
        List<Map<String, Double>> features = generateSyntheticFeatures(30);
        List<Double> targets = generateSyntheticTargets(30);
        model.train(features, targets);
        
        var metrics = model.getMetrics();
        
        assertNotNull(metrics);
        assertEquals("StochasticXLSTM_Model", metrics.getModelName());
        assertTrue(metrics.getAccuracy() >= 0.0 && metrics.getAccuracy() <= 1.0);
        assertNotNull(metrics.getLastTrainingTime());
        assertNotNull(metrics.getAdditionalMetrics());
        
        // Check additional stochastic metrics
        Map<String, Double> additionalMetrics = metrics.getAdditionalMetrics();
        assertTrue(additionalMetrics.containsKey("last_kl_divergence"));
        assertTrue(additionalMetrics.containsKey("last_uncertainty"));
        assertTrue(additionalMetrics.containsKey("training_samples"));
        assertEquals(30.0, additionalMetrics.get("training_samples"), 0.001);
    }
    
    @Test
    void testModelUpdate() {
        // Train initial model
        List<Map<String, Double>> features = generateSyntheticFeatures(20);
        List<Double> targets = generateSyntheticTargets(20);
        model.train(features, targets);
        
        int initialPredictions = model.getMetrics().getAdditionalMetrics().get("total_predictions").intValue();
        
        // Update model with new data
        Map<String, Double> newFeatures = generateSingleFeatureSet();
        double actualOutcome = 0.5;
        
        assertDoesNotThrow(() -> model.updateModel(newFeatures, actualOutcome));
        
        // Verify update
        var updatedMetrics = model.getMetrics();
        assertTrue(updatedMetrics.getAdditionalMetrics().get("training_samples") >= 20.0);
    }
    
    @Test
    void testServiceIntegration() {
        assertNotNull(service);
        assertTrue(service.isModelReady());
        
        // Test configuration retrieval
        Map<String, Object> config = service.getModelConfiguration();
        assertNotNull(config);
        assertEquals(12, config.get("input_size"));
        assertEquals(64, config.get("hidden_size"));
        assertEquals(50, config.get("sequence_length"));
    }
    
    @Test
    void testServicePrediction() {
        // Create test market data
        MarketData marketData = createTestMarketData();
        
        // Generate prediction through service
        MLPrediction prediction = service.predict(marketData);
        
        assertNotNull(prediction);
        assertNotNull(prediction.getSignal());
        assertTrue(Arrays.asList("BUY", "SELL", "HOLD").contains(prediction.getSignal()));
    }
    
    @Test
    void testBatchPrediction() throws Exception {
        // Create multiple market data points
        List<MarketData> marketDataList = Arrays.asList(
                createTestMarketData("AAPL"),
                createTestMarketData("GOOGL"),
                createTestMarketData("MSFT")
        );
        
        // Generate batch predictions
        Map<String, MLPrediction> predictions = service.predictBatch(marketDataList).get();
        
        assertNotNull(predictions);
        assertEquals(3, predictions.size());
        assertTrue(predictions.containsKey("AAPL"));
        assertTrue(predictions.containsKey("GOOGL"));
        assertTrue(predictions.containsKey("MSFT"));
    }
    
    @Test
    void testStochasticRegularization() {
        // Test with different regularization settings
        StochasticXLSTMModel modelWithHighReg = new StochasticXLSTMModel(12, 32, 20);
        StochasticXLSTMModel modelWithLowReg = new StochasticXLSTMModel(12, 32, 20);
        
        // Modify configurations
        modelWithHighReg.getConfig().stochasticRegularization = 0.1;
        modelWithLowReg.getConfig().stochasticRegularization = 0.001;
        
        // Train both models
        List<Map<String, Double>> features = generateSyntheticFeatures(30);
        List<Double> targets = generateSyntheticTargets(30);
        
        modelWithHighReg.train(features, targets);
        modelWithLowReg.train(features, targets);
        
        // Generate predictions
        Map<String, Double> testFeatures = generateSingleFeatureSet();
        MLPrediction predHigh = modelWithHighReg.predict(testFeatures);
        MLPrediction predLow = modelWithLowReg.predict(testFeatures);
        
        // High regularization should generally lead to higher uncertainty
        double uncertaintyHigh = predHigh.getPredictions().get("uncertainty");
        double uncertaintyLow = predLow.getPredictions().get("uncertainty");
        
        // Both should be valid
        assertTrue(uncertaintyHigh >= 0.0);
        assertTrue(uncertaintyLow >= 0.0);
    }
    
    // Helper methods
    
    private List<Map<String, Double>> generateSyntheticFeatures(int count) {
        List<Map<String, Double>> features = new ArrayList<>();
        Random random = new Random(42); // Fixed seed for reproducibility
        
        for (int i = 0; i < count; i++) {
            Map<String, Double> featureMap = new HashMap<>();
            featureMap.put("price", 100.0 + random.nextGaussian() * 10);
            featureMap.put("volume", 1000000.0 + random.nextGaussian() * 100000);
            featureMap.put("sma_20", 100.0 + random.nextGaussian() * 5);
            featureMap.put("ema_12", 100.0 + random.nextGaussian() * 5);
            featureMap.put("rsi", 50.0 + random.nextGaussian() * 20);
            featureMap.put("macd", random.nextGaussian() * 2);
            featureMap.put("bb_upper", 105.0 + random.nextGaussian() * 3);
            featureMap.put("bb_lower", 95.0 + random.nextGaussian() * 3);
            featureMap.put("stochastic", 50.0 + random.nextGaussian() * 25);
            featureMap.put("atr", 2.0 + random.nextGaussian() * 0.5);
            featureMap.put("momentum", random.nextGaussian() * 5);
            featureMap.put("volatility", 0.2 + random.nextGaussian() * 0.1);
            
            features.add(featureMap);
        }
        
        return features;
    }
    
    private List<Double> generateSyntheticTargets(int count) {
        List<Double> targets = new ArrayList<>();
        Random random = new Random(42);
        
        for (int i = 0; i < count; i++) {
            targets.add(random.nextGaussian() * 0.02); // Small price changes
        }
        
        return targets;
    }
    
    private Map<String, Double> generateSingleFeatureSet() {
        Map<String, Double> features = new HashMap<>();
        features.put("price", 105.0);
        features.put("volume", 1200000.0);
        features.put("sma_20", 103.0);
        features.put("ema_12", 104.0);
        features.put("rsi", 65.0);
        features.put("macd", 1.5);
        features.put("bb_upper", 108.0);
        features.put("bb_lower", 98.0);
        features.put("stochastic", 70.0);
        features.put("atr", 2.5);
        features.put("momentum", 3.0);
        features.put("volatility", 0.25);
        
        return features;
    }
    
    private MarketData createTestMarketData() {
        return createTestMarketData("AAPL");
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