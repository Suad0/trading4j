package com.quanttrading.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class StrategyPerformanceTest {

    private StrategyPerformance performance;

    @BeforeEach
    void setUp() {
        performance = new StrategyPerformance("TestStrategy");
    }

    @Test
    void testConstructor() {
        assertEquals("TestStrategy", performance.getStrategyName());
        assertEquals(0, performance.getTotalSignalsGenerated());
        assertEquals(0, performance.getTotalSignalsExecuted());
        assertEquals(BigDecimal.ZERO, performance.getTotalProfitLoss());
        assertEquals(BigDecimal.ZERO, performance.getTotalVolume());
        assertEquals(0, performance.getWinningTrades());
        assertEquals(0, performance.getLosingTrades());
        assertEquals(BigDecimal.ZERO, performance.getLargestWin());
        assertEquals(BigDecimal.ZERO, performance.getLargestLoss());
        assertNull(performance.getFirstSignalTime());
        assertNull(performance.getLastSignalTime());
        assertNotNull(performance.getLastUpdated());
    }

    @Test
    void testConstructorWithNullName() {
        assertThrows(NullPointerException.class, () -> new StrategyPerformance(null));
    }

    @Test
    void testRecordSignalGenerated() {
        LocalDateTime beforeFirst = LocalDateTime.now();
        
        performance.recordSignalGenerated();
        
        assertEquals(1, performance.getTotalSignalsGenerated());
        assertNotNull(performance.getFirstSignalTime());
        assertNotNull(performance.getLastSignalTime());
        assertTrue(performance.getFirstSignalTime().isAfter(beforeFirst) || 
                  performance.getFirstSignalTime().isEqual(beforeFirst));
        assertEquals(performance.getFirstSignalTime(), performance.getLastSignalTime());
        
        // Record another signal
        performance.recordSignalGenerated();
        
        assertEquals(2, performance.getTotalSignalsGenerated());
        assertTrue(performance.getLastSignalTime().isAfter(performance.getFirstSignalTime()) ||
                  performance.getLastSignalTime().isEqual(performance.getFirstSignalTime()));
    }

    @Test
    void testRecordSignalExecuted() {
        BigDecimal volume = new BigDecimal("100.00");
        
        performance.recordSignalExecuted(volume);
        
        assertEquals(1, performance.getTotalSignalsExecuted());
        assertEquals(volume, performance.getTotalVolume());
        
        // Record another execution
        performance.recordSignalExecuted(new BigDecimal("50.00"));
        
        assertEquals(2, performance.getTotalSignalsExecuted());
        assertEquals(new BigDecimal("150.00"), performance.getTotalVolume());
    }

    @Test
    void testRecordSignalExecutedWithNullVolume() {
        performance.recordSignalExecuted(null);
        
        assertEquals(1, performance.getTotalSignalsExecuted());
        assertEquals(BigDecimal.ZERO, performance.getTotalVolume());
    }

    @Test
    void testRecordTradeProfitLoss() {
        // Record a winning trade
        BigDecimal profit = new BigDecimal("50.00");
        performance.recordTradeProfitLoss(profit);
        
        assertEquals(profit, performance.getTotalProfitLoss());
        assertEquals(1, performance.getWinningTrades());
        assertEquals(0, performance.getLosingTrades());
        assertEquals(profit, performance.getLargestWin());
        assertEquals(BigDecimal.ZERO, performance.getLargestLoss());
        
        // Record a losing trade
        BigDecimal loss = new BigDecimal("-30.00");
        performance.recordTradeProfitLoss(loss);
        
        assertEquals(new BigDecimal("20.00"), performance.getTotalProfitLoss());
        assertEquals(1, performance.getWinningTrades());
        assertEquals(1, performance.getLosingTrades());
        assertEquals(profit, performance.getLargestWin());
        assertEquals(loss, performance.getLargestLoss());
        
        // Record a larger win
        BigDecimal biggerProfit = new BigDecimal("100.00");
        performance.recordTradeProfitLoss(biggerProfit);
        
        assertEquals(2, performance.getWinningTrades());
        assertEquals(biggerProfit, performance.getLargestWin());
        
        // Record a larger loss
        BigDecimal biggerLoss = new BigDecimal("-80.00");
        performance.recordTradeProfitLoss(biggerLoss);
        
        assertEquals(2, performance.getLosingTrades());
        assertEquals(biggerLoss, performance.getLargestLoss());
    }

    @Test
    void testRecordTradeProfitLossWithNullValue() {
        performance.recordTradeProfitLoss(null);
        
        assertEquals(BigDecimal.ZERO, performance.getTotalProfitLoss());
        assertEquals(0, performance.getWinningTrades());
        assertEquals(0, performance.getLosingTrades());
    }

    @Test
    void testRecordTradeProfitLossWithZeroValue() {
        performance.recordTradeProfitLoss(BigDecimal.ZERO);
        
        assertEquals(BigDecimal.ZERO, performance.getTotalProfitLoss());
        assertEquals(0, performance.getWinningTrades());
        assertEquals(0, performance.getLosingTrades());
    }

    @Test
    void testGetWinRate() {
        // No trades
        assertEquals(0.0, performance.getWinRate());
        
        // Add winning trades
        performance.recordTradeProfitLoss(new BigDecimal("50.00"));
        performance.recordTradeProfitLoss(new BigDecimal("30.00"));
        performance.recordTradeProfitLoss(new BigDecimal("-20.00"));
        
        // 2 wins out of 3 trades = 66.67%
        assertEquals(2.0/3.0, performance.getWinRate(), 0.001);
    }

    @Test
    void testGetExecutionRate() {
        // No signals
        assertEquals(0.0, performance.getExecutionRate());
        
        // Generate signals
        performance.recordSignalGenerated();
        performance.recordSignalGenerated();
        performance.recordSignalGenerated();
        
        // Execute some signals
        performance.recordSignalExecuted(new BigDecimal("100"));
        performance.recordSignalExecuted(new BigDecimal("50"));
        
        // 2 executions out of 3 signals = 66.67%
        assertEquals(2.0/3.0, performance.getExecutionRate(), 0.001);
    }

    @Test
    void testGetAverageProfitPerTrade() {
        // No trades
        assertEquals(BigDecimal.ZERO, performance.getAverageProfitPerTrade());
        
        // Add trades
        performance.recordTradeProfitLoss(new BigDecimal("100.00"));
        performance.recordTradeProfitLoss(new BigDecimal("-50.00"));
        performance.recordTradeProfitLoss(new BigDecimal("25.00"));
        
        // Total P&L: 75.00, Total trades: 3, Average: 25.00
        BigDecimal expected = new BigDecimal("25.0000");
        assertEquals(expected, performance.getAverageProfitPerTrade());
    }

    @Test
    void testReset() {
        // Set up some data
        performance.recordSignalGenerated();
        performance.recordSignalExecuted(new BigDecimal("100"));
        performance.recordTradeProfitLoss(new BigDecimal("50.00"));
        
        // Verify data is set
        assertEquals(1, performance.getTotalSignalsGenerated());
        assertEquals(1, performance.getTotalSignalsExecuted());
        assertNotEquals(BigDecimal.ZERO, performance.getTotalProfitLoss());
        
        // Reset
        performance.reset();
        
        // Verify everything is reset
        assertEquals(0, performance.getTotalSignalsGenerated());
        assertEquals(0, performance.getTotalSignalsExecuted());
        assertEquals(BigDecimal.ZERO, performance.getTotalProfitLoss());
        assertEquals(BigDecimal.ZERO, performance.getTotalVolume());
        assertEquals(0, performance.getWinningTrades());
        assertEquals(0, performance.getLosingTrades());
        assertEquals(BigDecimal.ZERO, performance.getLargestWin());
        assertEquals(BigDecimal.ZERO, performance.getLargestLoss());
        assertNull(performance.getFirstSignalTime());
        assertNull(performance.getLastSignalTime());
        assertNotNull(performance.getLastUpdated());
    }

    @Test
    void testToString() {
        performance.recordSignalGenerated();
        performance.recordSignalGenerated();
        performance.recordSignalExecuted(new BigDecimal("100"));
        performance.recordTradeProfitLoss(new BigDecimal("50.00"));
        
        String toString = performance.toString();
        
        assertTrue(toString.contains("TestStrategy"));
        assertTrue(toString.contains("totalSignalsGenerated=2"));
        assertTrue(toString.contains("totalSignalsExecuted=1"));
        assertTrue(toString.contains("totalProfitLoss=50"));
        // Use regex to handle different locale formatting (. or , as decimal separator)
        assertTrue(toString.matches(".*winRate=100[.,]00%.*"));
        assertTrue(toString.matches(".*executionRate=50[.,]00%.*"));
    }
}