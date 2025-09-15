package com.quanttrading.strategy;

import com.quanttrading.model.MarketData;
import com.quanttrading.model.TradeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AbstractTradingStrategyTest {

    private TestTradingStrategy strategy;
    private StrategyConfig config;
    private MarketData marketData;

    @BeforeEach
    void setUp() {
        config = StrategyConfig.builder("TestStrategy")
                .minConfidence(0.7)
                .parameter("testParam", "testValue")
                .build();
        
        strategy = new TestTradingStrategy(config);
        
        marketData = new MarketData();
        marketData.setSymbol("AAPL");
        marketData.setPrice(new BigDecimal("150.00"));
        marketData.setTimestamp(LocalDateTime.now());
    }

    @Test
    void testConstructorWithValidConfig() {
        assertNotNull(strategy);
        assertEquals("TestStrategy", strategy.getName());
        assertEquals(config, strategy.getConfig());
        assertTrue(strategy.isEnabled());
    }

    @Test
    void testConstructorWithNullConfig() {
        assertThrows(NullPointerException.class, () -> new TestTradingStrategy(null));
    }

    @Test
    void testAnalyzeWhenEnabled() {
        List<TradingSignal> signals = strategy.analyze(marketData);
        
        assertEquals(1, signals.size());
        TradingSignal signal = signals.get(0);
        assertEquals("AAPL", signal.getSymbol());
        assertEquals(TradeType.BUY, signal.getTradeType());
        assertEquals("TestStrategy", signal.getStrategyName());
    }

    @Test
    void testAnalyzeWhenDisabled() {
        strategy.setEnabled(false);
        List<TradingSignal> signals = strategy.analyze(marketData);
        
        assertTrue(signals.isEmpty());
    }

    @Test
    void testAnalyzeWithNullMarketData() {
        List<TradingSignal> signals = strategy.analyze(null);
        assertTrue(signals.isEmpty());
    }

    @Test
    void testAnalyzeWithException() {
        strategy.setShouldThrowException(true);
        List<TradingSignal> signals = strategy.analyze(marketData);
        
        // Should return empty list when exception occurs
        assertTrue(signals.isEmpty());
    }

    @Test
    void testShouldExecuteWithValidSignal() {
        TradingSignal signal = TradingSignal.builder()
                .symbol("AAPL")
                .tradeType(TradeType.BUY)
                .quantity(new BigDecimal("100"))
                .strategyName("TestStrategy")
                .confidence(0.8) // Above minimum confidence
                .build();

        assertTrue(strategy.shouldExecute(signal));
    }

    @Test
    void testShouldExecuteWithLowConfidence() {
        TradingSignal signal = TradingSignal.builder()
                .symbol("AAPL")
                .tradeType(TradeType.BUY)
                .quantity(new BigDecimal("100"))
                .strategyName("TestStrategy")
                .confidence(0.5) // Below minimum confidence (0.7)
                .build();

        assertFalse(strategy.shouldExecute(signal));
    }

    @Test
    void testShouldExecuteWhenDisabled() {
        strategy.setEnabled(false);
        
        TradingSignal signal = TradingSignal.builder()
                .symbol("AAPL")
                .tradeType(TradeType.BUY)
                .quantity(new BigDecimal("100"))
                .strategyName("TestStrategy")
                .confidence(0.8)
                .build();

        assertFalse(strategy.shouldExecute(signal));
    }

    @Test
    void testShouldExecuteWithNullSignal() {
        assertFalse(strategy.shouldExecute(null));
    }

    @Test
    void testUpdateConfig() {
        StrategyConfig newConfig = StrategyConfig.builder("TestStrategy")
                .minConfidence(0.9)
                .enabled(false)
                .build();

        strategy.updateConfig(newConfig);

        assertEquals(newConfig, strategy.getConfig());
        assertFalse(strategy.isEnabled());
    }

    @Test
    void testUpdateConfigWithNull() {
        assertThrows(NullPointerException.class, () -> strategy.updateConfig(null));
    }

    @Test
    void testSetEnabled() {
        assertTrue(strategy.isEnabled());
        
        strategy.setEnabled(false);
        assertFalse(strategy.isEnabled());
        
        strategy.setEnabled(true);
        assertTrue(strategy.isEnabled());
    }

    @Test
    void testValidateRequiredParameters() {
        // Should not throw exception for existing parameter
        assertDoesNotThrow(() -> strategy.testValidateRequiredParameters("testParam"));
        
        // Should throw exception for missing parameter
        assertThrows(IllegalArgumentException.class, () -> 
                strategy.testValidateRequiredParameters("missingParam"));
    }

    @Test
    void testGetConfigParameter() {
        // Test existing parameter
        String value = strategy.testGetConfigParameter("testParam", String.class, "default");
        assertEquals("testValue", value);
        
        // Test missing parameter with default
        String defaultValue = strategy.testGetConfigParameter("missingParam", String.class, "default");
        assertEquals("default", defaultValue);
    }

    // Test implementation of AbstractTradingStrategy
    private static class TestTradingStrategy extends AbstractTradingStrategy {
        private boolean shouldThrowException = false;

        public TestTradingStrategy(StrategyConfig config) {
            super(config);
        }

        @Override
        public String getName() {
            return "TestStrategy";
        }

        @Override
        protected List<TradingSignal> doAnalyze(MarketData marketData) {
            if (shouldThrowException) {
                throw new RuntimeException("Test exception");
            }
            
            TradingSignal signal = TradingSignal.builder()
                    .symbol(marketData.getSymbol())
                    .tradeType(TradeType.BUY)
                    .quantity(new BigDecimal("100"))
                    .strategyName(getName())
                    .confidence(0.8)
                    .build();
            
            return List.of(signal);
        }

        public void setShouldThrowException(boolean shouldThrowException) {
            this.shouldThrowException = shouldThrowException;
        }

        // Expose protected methods for testing
        public void testValidateRequiredParameters(String... params) {
            validateRequiredParameters(params);
        }

        public <T> T testGetConfigParameter(String key, Class<T> type, T defaultValue) {
            return getConfigParameter(key, type, defaultValue);
        }
    }
}