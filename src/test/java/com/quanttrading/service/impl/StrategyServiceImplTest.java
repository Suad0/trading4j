package com.quanttrading.service.impl;

import com.quanttrading.model.MarketData;
import com.quanttrading.model.TradeType;
import com.quanttrading.service.StrategyPerformance;
import com.quanttrading.service.TradingService;
import com.quanttrading.strategy.StrategyRegistry;
import com.quanttrading.strategy.TradingSignal;
import com.quanttrading.strategy.TradingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StrategyServiceImplTest {

    @Mock
    private StrategyRegistry strategyRegistry;
    
    @Mock
    private TradingService tradingService;
    
    @Mock
    private TradingStrategy mockStrategy1;
    
    @Mock
    private TradingStrategy mockStrategy2;
    
    private StrategyServiceImpl strategyService;
    private MarketData marketData;

    @BeforeEach
    void setUp() {
        strategyService = new StrategyServiceImpl(strategyRegistry, tradingService);
        
        marketData = new MarketData();
        marketData.setSymbol("AAPL");
        marketData.setPrice(new BigDecimal("150.00"));
        marketData.setTimestamp(LocalDateTime.now());
        
        // Setup mock strategies
        when(mockStrategy1.getName()).thenReturn("Strategy1");
        when(mockStrategy1.isEnabled()).thenReturn(true);
        
        when(mockStrategy2.getName()).thenReturn("Strategy2");
        when(mockStrategy2.isEnabled()).thenReturn(true);
    }

    @Test
    void testRegisterStrategy() {
        strategyService.registerStrategy(mockStrategy1);
        
        verify(strategyRegistry).registerStrategy(mockStrategy1);
        
        // Verify performance tracking is set up
        StrategyPerformance performance = strategyService.getStrategyPerformance("Strategy1");
        assertNotNull(performance);
        assertEquals("Strategy1", performance.getStrategyName());
    }

    @Test
    void testUnregisterStrategy() {
        // Setup
        strategyService.registerStrategy(mockStrategy1);
        when(strategyRegistry.unregisterStrategy("Strategy1")).thenReturn(mockStrategy1);
        
        boolean result = strategyService.unregisterStrategy("Strategy1");
        
        assertTrue(result);
        verify(strategyRegistry).unregisterStrategy("Strategy1");
        
        // Verify performance tracking is removed
        assertNull(strategyService.getStrategyPerformance("Strategy1"));
    }

    @Test
    void testUnregisterNonExistentStrategy() {
        when(strategyRegistry.unregisterStrategy("NonExistent")).thenReturn(null);
        
        boolean result = strategyService.unregisterStrategy("NonExistent");
        
        assertFalse(result);
    }

    @Test
    void testGetStrategy() {
        when(strategyRegistry.getStrategy("Strategy1")).thenReturn(mockStrategy1);
        
        TradingStrategy result = strategyService.getStrategy("Strategy1");
        
        assertEquals(mockStrategy1, result);
        verify(strategyRegistry).getStrategy("Strategy1");
    }

    @Test
    void testGetAllStrategies() {
        List<TradingStrategy> strategies = List.of(mockStrategy1, mockStrategy2);
        when(strategyRegistry.getAllStrategies()).thenReturn(strategies);
        
        List<TradingStrategy> result = strategyService.getAllStrategies();
        
        assertEquals(2, result.size());
        assertTrue(result.contains(mockStrategy1));
        assertTrue(result.contains(mockStrategy2));
    }

    @Test
    void testGetEnabledStrategies() {
        List<TradingStrategy> enabledStrategies = List.of(mockStrategy1);
        when(strategyRegistry.getEnabledStrategies()).thenReturn(enabledStrategies);
        
        List<TradingStrategy> result = strategyService.getEnabledStrategies();
        
        assertEquals(1, result.size());
        assertEquals(mockStrategy1, result.get(0));
    }

    @Test
    void testExecuteStrategies() {
        // Setup
        TradingSignal signal1 = createTradingSignal("AAPL", TradeType.BUY, "Strategy1");
        TradingSignal signal2 = createTradingSignal("AAPL", TradeType.SELL, "Strategy2");
        
        when(strategyRegistry.getEnabledStrategies()).thenReturn(List.of(mockStrategy1, mockStrategy2));
        when(mockStrategy1.analyze(marketData)).thenReturn(List.of(signal1));
        when(mockStrategy2.analyze(marketData)).thenReturn(List.of(signal2));
        when(mockStrategy1.shouldExecute(signal1)).thenReturn(true);
        when(mockStrategy2.shouldExecute(signal2)).thenReturn(false);
        
        // Register strategies to set up performance tracking
        strategyService.registerStrategy(mockStrategy1);
        strategyService.registerStrategy(mockStrategy2);
        
        Map<String, List<TradingSignal>> result = strategyService.executeStrategies(marketData);
        
        assertEquals(2, result.size());
        assertEquals(1, result.get("Strategy1").size());
        assertEquals(1, result.get("Strategy2").size());
        assertEquals(signal1, result.get("Strategy1").get(0));
        assertEquals(signal2, result.get("Strategy2").get(0));
        
        // Verify trading service was called for executed signal
        verify(tradingService).executeBuyOrder(eq("AAPL"), eq(new BigDecimal("100")), isNull());
        verify(tradingService, never()).executeSellOrder(any(), any(), any());
        
        // Verify performance tracking
        StrategyPerformance perf1 = strategyService.getStrategyPerformance("Strategy1");
        StrategyPerformance perf2 = strategyService.getStrategyPerformance("Strategy2");
        
        assertEquals(1, perf1.getTotalSignalsGenerated());
        assertEquals(1, perf1.getTotalSignalsExecuted());
        assertEquals(1, perf2.getTotalSignalsGenerated());
        assertEquals(0, perf2.getTotalSignalsExecuted());
    }

    @Test
    void testExecuteStrategiesWithNullMarketData() {
        Map<String, List<TradingSignal>> result = strategyService.executeStrategies(null);
        
        assertTrue(result.isEmpty());
        verify(strategyRegistry, never()).getEnabledStrategies();
    }

    @Test
    void testExecuteStrategiesWithException() {
        // Setup strategy to throw exception
        when(strategyRegistry.getEnabledStrategies()).thenReturn(List.of(mockStrategy1));
        when(mockStrategy1.analyze(marketData)).thenThrow(new RuntimeException("Strategy error"));
        
        strategyService.registerStrategy(mockStrategy1);
        
        Map<String, List<TradingSignal>> result = strategyService.executeStrategies(marketData);
        
        assertEquals(1, result.size());
        assertTrue(result.get("Strategy1").isEmpty());
    }

    @Test
    void testExecuteStrategy() {
        TradingSignal signal = createTradingSignal("AAPL", TradeType.BUY, "Strategy1");
        
        when(strategyRegistry.getStrategy("Strategy1")).thenReturn(mockStrategy1);
        when(mockStrategy1.analyze(marketData)).thenReturn(List.of(signal));
        when(mockStrategy1.shouldExecute(signal)).thenReturn(true);
        
        strategyService.registerStrategy(mockStrategy1);
        
        List<TradingSignal> result = strategyService.executeStrategy("Strategy1", marketData);
        
        assertEquals(1, result.size());
        assertEquals(signal, result.get(0));
        
        verify(tradingService).executeBuyOrder(eq("AAPL"), eq(new BigDecimal("100")), isNull());
        
        // Verify performance tracking
        StrategyPerformance performance = strategyService.getStrategyPerformance("Strategy1");
        assertEquals(1, performance.getTotalSignalsGenerated());
        assertEquals(1, performance.getTotalSignalsExecuted());
    }

    @Test
    void testExecuteStrategyNotFound() {
        when(strategyRegistry.getStrategy("NonExistent")).thenReturn(null);
        
        List<TradingSignal> result = strategyService.executeStrategy("NonExistent", marketData);
        
        assertTrue(result.isEmpty());
    }

    @Test
    void testExecuteStrategyDisabled() {
        when(strategyRegistry.getStrategy("Strategy1")).thenReturn(mockStrategy1);
        when(mockStrategy1.isEnabled()).thenReturn(false);
        
        List<TradingSignal> result = strategyService.executeStrategy("Strategy1", marketData);
        
        assertTrue(result.isEmpty());
        verify(mockStrategy1, never()).analyze(any());
    }

    @Test
    void testExecuteStrategyWithNullMarketData() {
        when(strategyRegistry.getStrategy("Strategy1")).thenReturn(mockStrategy1);
        
        List<TradingSignal> result = strategyService.executeStrategy("Strategy1", null);
        
        assertTrue(result.isEmpty());
        verify(mockStrategy1, never()).analyze(any());
    }

    @Test
    void testSetStrategyEnabled() {
        when(strategyRegistry.setStrategyEnabled("Strategy1", true)).thenReturn(true);
        
        boolean result = strategyService.setStrategyEnabled("Strategy1", true);
        
        assertTrue(result);
        verify(strategyRegistry).setStrategyEnabled("Strategy1", true);
    }

    @Test
    void testGetStrategyPerformance() {
        strategyService.registerStrategy(mockStrategy1);
        strategyService.registerStrategy(mockStrategy2);
        
        Map<String, StrategyPerformance> result = strategyService.getStrategyPerformance();
        
        assertEquals(2, result.size());
        assertTrue(result.containsKey("Strategy1"));
        assertTrue(result.containsKey("Strategy2"));
    }

    @Test
    void testGetStrategyPerformanceByName() {
        strategyService.registerStrategy(mockStrategy1);
        
        StrategyPerformance result = strategyService.getStrategyPerformance("Strategy1");
        
        assertNotNull(result);
        assertEquals("Strategy1", result.getStrategyName());
        
        assertNull(strategyService.getStrategyPerformance("NonExistent"));
    }

    @Test
    void testResetPerformanceTracking() {
        strategyService.registerStrategy(mockStrategy1);
        strategyService.registerStrategy(mockStrategy2);
        
        // Add some performance data
        StrategyPerformance perf1 = strategyService.getStrategyPerformance("Strategy1");
        perf1.recordSignalGenerated();
        
        strategyService.resetPerformanceTracking();
        
        assertEquals(0, perf1.getTotalSignalsGenerated());
    }

    @Test
    void testResetStrategyPerformance() {
        strategyService.registerStrategy(mockStrategy1);
        
        StrategyPerformance performance = strategyService.getStrategyPerformance("Strategy1");
        performance.recordSignalGenerated();
        
        boolean result = strategyService.resetStrategyPerformance("Strategy1");
        
        assertTrue(result);
        assertEquals(0, performance.getTotalSignalsGenerated());
        
        assertFalse(strategyService.resetStrategyPerformance("NonExistent"));
    }

    @Test
    void testGetStrategyCount() {
        when(strategyRegistry.getStrategyCount()).thenReturn(2);
        
        int result = strategyService.getStrategyCount();
        
        assertEquals(2, result);
        verify(strategyRegistry).getStrategyCount();
    }

    @Test
    void testGetEnabledStrategyCount() {
        when(strategyRegistry.getEnabledStrategyCount()).thenReturn(1);
        
        int result = strategyService.getEnabledStrategyCount();
        
        assertEquals(1, result);
        verify(strategyRegistry).getEnabledStrategyCount();
    }

    @Test
    void testExecuteSignalWithSellOrder() {
        TradingSignal sellSignal = createTradingSignal("AAPL", TradeType.SELL, "Strategy1");
        
        when(strategyRegistry.getEnabledStrategies()).thenReturn(List.of(mockStrategy1));
        when(mockStrategy1.analyze(marketData)).thenReturn(List.of(sellSignal));
        when(mockStrategy1.shouldExecute(sellSignal)).thenReturn(true);
        
        strategyService.registerStrategy(mockStrategy1);
        
        strategyService.executeStrategies(marketData);
        
        verify(tradingService).executeSellOrder(eq("AAPL"), eq(new BigDecimal("100")), isNull());
    }

    @Test
    void testExecuteSignalWithTradingException() {
        TradingSignal signal = createTradingSignal("AAPL", TradeType.BUY, "Strategy1");
        
        when(strategyRegistry.getEnabledStrategies()).thenReturn(List.of(mockStrategy1));
        when(mockStrategy1.analyze(marketData)).thenReturn(List.of(signal));
        when(mockStrategy1.shouldExecute(signal)).thenReturn(true);
        when(tradingService.executeBuyOrder(any(), any(), any())).thenThrow(new RuntimeException("Trading error"));
        
        strategyService.registerStrategy(mockStrategy1);
        
        // Should not throw exception, just log error
        assertDoesNotThrow(() -> strategyService.executeStrategies(marketData));
        
        // Performance should still be updated for signal generation
        StrategyPerformance performance = strategyService.getStrategyPerformance("Strategy1");
        assertEquals(1, performance.getTotalSignalsGenerated());
        assertEquals(1, performance.getTotalSignalsExecuted()); // Execution was attempted
    }

    private TradingSignal createTradingSignal(String symbol, TradeType tradeType, String strategyName) {
        return TradingSignal.builder()
                .symbol(symbol)
                .tradeType(tradeType)
                .quantity(new BigDecimal("100"))
                .targetPrice(new BigDecimal("150.00"))
                .strategyName(strategyName)
                .confidence(0.8)
                .build();
    }
}