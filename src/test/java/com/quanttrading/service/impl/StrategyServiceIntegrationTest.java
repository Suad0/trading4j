package com.quanttrading.service.impl;

import com.quanttrading.model.MarketData;
import com.quanttrading.model.TradeType;
import com.quanttrading.service.StrategyPerformance;
import com.quanttrading.service.TradingService;
import com.quanttrading.strategy.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for StrategyServiceImpl without mocking.
 */
class StrategyServiceIntegrationTest {

    private StrategyServiceImpl strategyService;
    private StrategyRegistry strategyRegistry;
    private TestTradingService tradingService;
    private TestTradingStrategy testStrategy;
    private MarketData marketData;

    @BeforeEach
    void setUp() {
        strategyRegistry = new StrategyRegistry();
        tradingService = new TestTradingService();
        strategyService = new StrategyServiceImpl(strategyRegistry, tradingService);
        
        StrategyConfig config = StrategyConfig.builder("TestStrategy")
                .parameter("testParam", "testValue")
                .build();
        testStrategy = new TestTradingStrategy(config);
        
        marketData = new MarketData();
        marketData.setSymbol("AAPL");
        marketData.setPrice(new BigDecimal("150.00"));
        marketData.setTimestamp(LocalDateTime.now());
    }

    @Test
    void testRegisterAndGetStrategy() {
        strategyService.registerStrategy(testStrategy);
        
        assertEquals(testStrategy, strategyService.getStrategy("TestStrategy"));
        assertEquals(1, strategyService.getStrategyCount());
        
        // Verify performance tracking is set up
        StrategyPerformance performance = strategyService.getStrategyPerformance("TestStrategy");
        assertNotNull(performance);
        assertEquals("TestStrategy", performance.getStrategyName());
    }

    @Test
    void testUnregisterStrategy() {
        strategyService.registerStrategy(testStrategy);
        
        boolean result = strategyService.unregisterStrategy("TestStrategy");
        
        assertTrue(result);
        assertNull(strategyService.getStrategy("TestStrategy"));
        assertEquals(0, strategyService.getStrategyCount());
        assertNull(strategyService.getStrategyPerformance("TestStrategy"));
    }

    @Test
    void testExecuteStrategy() {
        strategyService.registerStrategy(testStrategy);
        testStrategy.setSignalToGenerate(createTestSignal());
        
        List<TradingSignal> signals = strategyService.executeStrategy("TestStrategy", marketData);
        
        assertEquals(1, signals.size());
        assertEquals("AAPL", signals.get(0).getSymbol());
        assertEquals(TradeType.BUY, signals.get(0).getTradeType());
        
        // Verify performance tracking
        StrategyPerformance performance = strategyService.getStrategyPerformance("TestStrategy");
        assertEquals(1, performance.getTotalSignalsGenerated());
        assertEquals(1, performance.getTotalSignalsExecuted());
        
        // Verify trading service was called
        assertEquals(1, tradingService.getBuyOrderCount());
    }

    @Test
    void testExecuteStrategies() {
        strategyService.registerStrategy(testStrategy);
        testStrategy.setSignalToGenerate(createTestSignal());
        
        Map<String, List<TradingSignal>> results = strategyService.executeStrategies(marketData);
        
        assertEquals(1, results.size());
        assertTrue(results.containsKey("TestStrategy"));
        assertEquals(1, results.get("TestStrategy").size());
        
        // Verify performance tracking
        StrategyPerformance performance = strategyService.getStrategyPerformance("TestStrategy");
        assertEquals(1, performance.getTotalSignalsGenerated());
        assertEquals(1, performance.getTotalSignalsExecuted());
    }

    @Test
    void testExecuteStrategyWithDisabledStrategy() {
        strategyService.registerStrategy(testStrategy);
        testStrategy.setEnabled(false);
        
        List<TradingSignal> signals = strategyService.executeStrategy("TestStrategy", marketData);
        
        assertTrue(signals.isEmpty());
        
        // Performance should not be updated for disabled strategy
        StrategyPerformance performance = strategyService.getStrategyPerformance("TestStrategy");
        assertEquals(0, performance.getTotalSignalsGenerated());
    }

    @Test
    void testSetStrategyEnabled() {
        strategyService.registerStrategy(testStrategy);
        
        assertTrue(testStrategy.isEnabled());
        
        strategyService.setStrategyEnabled("TestStrategy", false);
        assertFalse(testStrategy.isEnabled());
        
        strategyService.setStrategyEnabled("TestStrategy", true);
        assertTrue(testStrategy.isEnabled());
    }

    @Test
    void testGetAllStrategies() {
        strategyService.registerStrategy(testStrategy);
        
        List<TradingStrategy> strategies = strategyService.getAllStrategies();
        
        assertEquals(1, strategies.size());
        assertEquals(testStrategy, strategies.get(0));
    }

    @Test
    void testGetEnabledStrategies() {
        strategyService.registerStrategy(testStrategy);
        
        List<TradingStrategy> enabledStrategies = strategyService.getEnabledStrategies();
        assertEquals(1, enabledStrategies.size());
        
        testStrategy.setEnabled(false);
        enabledStrategies = strategyService.getEnabledStrategies();
        assertEquals(0, enabledStrategies.size());
    }

    @Test
    void testResetPerformanceTracking() {
        strategyService.registerStrategy(testStrategy);
        testStrategy.setSignalToGenerate(createTestSignal());
        
        // Generate some performance data
        strategyService.executeStrategy("TestStrategy", marketData);
        
        StrategyPerformance performance = strategyService.getStrategyPerformance("TestStrategy");
        assertEquals(1, performance.getTotalSignalsGenerated());
        
        // Reset performance
        strategyService.resetPerformanceTracking();
        
        assertEquals(0, performance.getTotalSignalsGenerated());
    }

    private TradingSignal createTestSignal() {
        return TradingSignal.builder()
                .symbol("AAPL")
                .tradeType(TradeType.BUY)
                .quantity(new BigDecimal("100"))
                .targetPrice(new BigDecimal("150.00"))
                .strategyName("TestStrategy")
                .confidence(0.8)
                .build();
    }

    // Test implementation of TradingStrategy
    private static class TestTradingStrategy extends AbstractTradingStrategy {
        private TradingSignal signalToGenerate;

        public TestTradingStrategy(StrategyConfig config) {
            super(config);
        }

        @Override
        public String getName() {
            return "TestStrategy";
        }

        @Override
        protected List<TradingSignal> doAnalyze(MarketData marketData) {
            return signalToGenerate != null ? List.of(signalToGenerate) : List.of();
        }

        public void setSignalToGenerate(TradingSignal signal) {
            this.signalToGenerate = signal;
        }
    }

    // Test implementation of TradingService
    private static class TestTradingService implements TradingService {
        private int buyOrderCount = 0;
        private int sellOrderCount = 0;

        @Override
        public com.quanttrading.dto.OrderResponse executeBuyOrder(String symbol, BigDecimal quantity, String orderType) {
            buyOrderCount++;
            return null; // Not needed for this test
        }

        @Override
        public com.quanttrading.dto.OrderResponse executeBuyOrder(String symbol, BigDecimal quantity, String orderType, BigDecimal limitPrice) {
            buyOrderCount++;
            return null;
        }

        @Override
        public com.quanttrading.dto.OrderResponse executeSellOrder(String symbol, BigDecimal quantity, String orderType) {
            sellOrderCount++;
            return null;
        }

        @Override
        public com.quanttrading.dto.OrderResponse executeSellOrder(String symbol, BigDecimal quantity, String orderType, BigDecimal limitPrice) {
            sellOrderCount++;
            return null;
        }

        public int getBuyOrderCount() {
            return buyOrderCount;
        }

        public int getSellOrderCount() {
            return sellOrderCount;
        }

        // Other methods not needed for this test
        @Override
        public com.quanttrading.model.OrderStatus getOrderStatus(String orderId) { return null; }
        @Override
        public List<com.quanttrading.model.Trade> getTradeHistory() { return List.of(); }
        @Override
        public List<com.quanttrading.model.Trade> getTradeHistory(String accountId) { return List.of(); }
        @Override
        public List<com.quanttrading.model.Trade> getRecentTrades(int limit) { return List.of(); }
        @Override
        public List<com.quanttrading.model.Trade> getTradesBySymbol(String symbol) { return List.of(); }
        @Override
        public boolean cancelOrder(String orderId) { return false; }
        @Override
        public boolean validateOrder(String symbol, BigDecimal quantity, TradeType tradeType, BigDecimal price) { return true; }
        @Override
        public boolean hasSufficientFunds(String symbol, BigDecimal quantity, BigDecimal price) { return true; }
        @Override
        public boolean hasSufficientShares(String symbol, BigDecimal quantity) { return true; }
    }
}