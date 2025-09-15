package com.quanttrading.strategy;

import com.quanttrading.model.MarketData;
import com.quanttrading.model.TradeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class StrategyRegistryTest {

    private StrategyRegistry registry;
    private TradingStrategy strategy1;
    private TradingStrategy strategy2;

    @BeforeEach
    void setUp() {
        registry = new StrategyRegistry();
        
        StrategyConfig config1 = StrategyConfig.builder("Strategy1").build();
        StrategyConfig config2 = StrategyConfig.builder("Strategy2").enabled(false).build();
        
        strategy1 = new TestTradingStrategy("Strategy1", config1);
        strategy2 = new TestTradingStrategy("Strategy2", config2);
    }

    @Test
    void testRegisterStrategy() {
        registry.registerStrategy(strategy1);
        
        assertTrue(registry.isStrategyRegistered("Strategy1"));
        assertEquals(1, registry.getStrategyCount());
        assertEquals(strategy1, registry.getStrategy("Strategy1"));
    }

    @Test
    void testRegisterStrategyWithNullStrategy() {
        assertThrows(NullPointerException.class, () -> registry.registerStrategy(null));
    }

    @Test
    void testRegisterDuplicateStrategy() {
        registry.registerStrategy(strategy1);
        
        TradingStrategy duplicateStrategy = new TestTradingStrategy("Strategy1", 
                StrategyConfig.builder("Strategy1").build());
        
        assertThrows(IllegalArgumentException.class, () -> 
                registry.registerStrategy(duplicateStrategy));
    }

    @Test
    void testUnregisterStrategy() {
        registry.registerStrategy(strategy1);
        
        TradingStrategy removed = registry.unregisterStrategy("Strategy1");
        
        assertEquals(strategy1, removed);
        assertFalse(registry.isStrategyRegistered("Strategy1"));
        assertEquals(0, registry.getStrategyCount());
    }

    @Test
    void testUnregisterNonExistentStrategy() {
        TradingStrategy removed = registry.unregisterStrategy("NonExistent");
        assertNull(removed);
    }

    @Test
    void testGetStrategy() {
        registry.registerStrategy(strategy1);
        
        assertEquals(strategy1, registry.getStrategy("Strategy1"));
        assertNull(registry.getStrategy("NonExistent"));
    }

    @Test
    void testGetAllStrategies() {
        registry.registerStrategy(strategy1);
        registry.registerStrategy(strategy2);
        
        Collection<TradingStrategy> strategies = registry.getAllStrategies();
        
        assertEquals(2, strategies.size());
        assertTrue(strategies.contains(strategy1));
        assertTrue(strategies.contains(strategy2));
        
        // Ensure returned collection is unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> 
                strategies.add(strategy1));
    }

    @Test
    void testGetEnabledStrategies() {
        registry.registerStrategy(strategy1); // enabled
        registry.registerStrategy(strategy2); // disabled
        
        List<TradingStrategy> enabledStrategies = registry.getEnabledStrategies();
        
        assertEquals(1, enabledStrategies.size());
        assertTrue(enabledStrategies.contains(strategy1));
        assertFalse(enabledStrategies.contains(strategy2));
    }

    @Test
    void testGetStrategyNames() {
        registry.registerStrategy(strategy1);
        registry.registerStrategy(strategy2);
        
        Set<String> names = registry.getStrategyNames();
        
        assertEquals(2, names.size());
        assertTrue(names.contains("Strategy1"));
        assertTrue(names.contains("Strategy2"));
        
        // Ensure returned set is unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> 
                names.add("NewStrategy"));
    }

    @Test
    void testIsStrategyRegistered() {
        assertFalse(registry.isStrategyRegistered("Strategy1"));
        
        registry.registerStrategy(strategy1);
        
        assertTrue(registry.isStrategyRegistered("Strategy1"));
        assertFalse(registry.isStrategyRegistered("Strategy2"));
    }

    @Test
    void testGetStrategyCount() {
        assertEquals(0, registry.getStrategyCount());
        
        registry.registerStrategy(strategy1);
        assertEquals(1, registry.getStrategyCount());
        
        registry.registerStrategy(strategy2);
        assertEquals(2, registry.getStrategyCount());
        
        registry.unregisterStrategy("Strategy1");
        assertEquals(1, registry.getStrategyCount());
    }

    @Test
    void testGetEnabledStrategyCount() {
        assertEquals(0, registry.getEnabledStrategyCount());
        
        registry.registerStrategy(strategy1); // enabled
        assertEquals(1, registry.getEnabledStrategyCount());
        
        registry.registerStrategy(strategy2); // disabled
        assertEquals(1, registry.getEnabledStrategyCount());
        
        strategy1.setEnabled(false);
        assertEquals(0, registry.getEnabledStrategyCount());
    }

    @Test
    void testSetStrategyEnabled() {
        registry.registerStrategy(strategy1);
        
        assertTrue(strategy1.isEnabled());
        
        boolean result = registry.setStrategyEnabled("Strategy1", false);
        assertTrue(result);
        assertFalse(strategy1.isEnabled());
        
        result = registry.setStrategyEnabled("Strategy1", true);
        assertTrue(result);
        assertTrue(strategy1.isEnabled());
    }

    @Test
    void testSetStrategyEnabledForNonExistentStrategy() {
        boolean result = registry.setStrategyEnabled("NonExistent", true);
        assertFalse(result);
    }

    @Test
    void testUpdateStrategyConfig() {
        registry.registerStrategy(strategy1);
        
        StrategyConfig newConfig = StrategyConfig.builder("Strategy1")
                .minConfidence(0.9)
                .build();
        
        boolean result = registry.updateStrategyConfig("Strategy1", newConfig);
        
        assertTrue(result);
        assertEquals(newConfig, strategy1.getConfig());
    }

    @Test
    void testUpdateStrategyConfigForNonExistentStrategy() {
        StrategyConfig config = StrategyConfig.builder("NonExistent").build();
        boolean result = registry.updateStrategyConfig("NonExistent", config);
        assertFalse(result);
    }

    @Test
    void testClear() {
        registry.registerStrategy(strategy1);
        registry.registerStrategy(strategy2);
        
        assertEquals(2, registry.getStrategyCount());
        
        registry.clear();
        
        assertEquals(0, registry.getStrategyCount());
        assertFalse(registry.isStrategyRegistered("Strategy1"));
        assertFalse(registry.isStrategyRegistered("Strategy2"));
    }

    // Test implementation of TradingStrategy
    private static class TestTradingStrategy implements TradingStrategy {
        private final String name;
        private StrategyConfig config;
        private boolean enabled;

        public TestTradingStrategy(String name, StrategyConfig config) {
            this.name = name;
            this.config = config;
            this.enabled = config.isEnabled();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public List<TradingSignal> analyze(MarketData marketData) {
            return List.of();
        }

        @Override
        public boolean shouldExecute(TradingSignal signal) {
            return enabled;
        }

        @Override
        public StrategyConfig getConfig() {
            return config;
        }

        @Override
        public void updateConfig(StrategyConfig config) {
            this.config = config;
            this.enabled = config.isEnabled();
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}