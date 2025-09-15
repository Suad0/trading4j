package com.quanttrading.strategy;

import com.quanttrading.model.TradeType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TradingSignalTest {

    @Test
    void testBuilderWithRequiredFields() {
        TradingSignal signal = TradingSignal.builder()
                .symbol("AAPL")
                .tradeType(TradeType.BUY)
                .quantity(new BigDecimal("100"))
                .strategyName("TestStrategy")
                .build();

        assertEquals("AAPL", signal.getSymbol());
        assertEquals(TradeType.BUY, signal.getTradeType());
        assertEquals(new BigDecimal("100"), signal.getQuantity());
        assertEquals("TestStrategy", signal.getStrategyName());
        assertEquals(0.5, signal.getConfidence()); // Default confidence
        assertNotNull(signal.getTimestamp());
    }

    @Test
    void testBuilderWithAllFields() {
        LocalDateTime timestamp = LocalDateTime.now();
        TradingSignal signal = TradingSignal.builder()
                .symbol("TSLA")
                .tradeType(TradeType.SELL)
                .quantity(new BigDecimal("50"))
                .targetPrice(new BigDecimal("200.00"))
                .stopLoss(new BigDecimal("180.00"))
                .takeProfit(new BigDecimal("220.00"))
                .strategyName("SMAStrategy")
                .timestamp(timestamp)
                .confidence(0.8)
                .reason("SMA crossover signal")
                .build();

        assertEquals("TSLA", signal.getSymbol());
        assertEquals(TradeType.SELL, signal.getTradeType());
        assertEquals(new BigDecimal("50"), signal.getQuantity());
        assertEquals(new BigDecimal("200.00"), signal.getTargetPrice());
        assertEquals(new BigDecimal("180.00"), signal.getStopLoss());
        assertEquals(new BigDecimal("220.00"), signal.getTakeProfit());
        assertEquals("SMAStrategy", signal.getStrategyName());
        assertEquals(timestamp, signal.getTimestamp());
        assertEquals(0.8, signal.getConfidence());
        assertEquals("SMA crossover signal", signal.getReason());
    }

    @Test
    void testBuilderValidation_MissingSymbol() {
        assertThrows(NullPointerException.class, () ->
                TradingSignal.builder()
                        .tradeType(TradeType.BUY)
                        .quantity(new BigDecimal("100"))
                        .strategyName("TestStrategy")
                        .build()
        );
    }

    @Test
    void testBuilderValidation_MissingTradeType() {
        assertThrows(NullPointerException.class, () ->
                TradingSignal.builder()
                        .symbol("AAPL")
                        .quantity(new BigDecimal("100"))
                        .strategyName("TestStrategy")
                        .build()
        );
    }

    @Test
    void testBuilderValidation_MissingQuantity() {
        assertThrows(NullPointerException.class, () ->
                TradingSignal.builder()
                        .symbol("AAPL")
                        .tradeType(TradeType.BUY)
                        .strategyName("TestStrategy")
                        .build()
        );
    }

    @Test
    void testBuilderValidation_MissingStrategyName() {
        assertThrows(NullPointerException.class, () ->
                TradingSignal.builder()
                        .symbol("AAPL")
                        .tradeType(TradeType.BUY)
                        .quantity(new BigDecimal("100"))
                        .build()
        );
    }

    @Test
    void testBuilderValidation_NegativeQuantity() {
        assertThrows(IllegalArgumentException.class, () ->
                TradingSignal.builder()
                        .symbol("AAPL")
                        .tradeType(TradeType.BUY)
                        .quantity(new BigDecimal("-100"))
                        .strategyName("TestStrategy")
                        .build()
        );
    }

    @Test
    void testBuilderValidation_ZeroQuantity() {
        assertThrows(IllegalArgumentException.class, () ->
                TradingSignal.builder()
                        .symbol("AAPL")
                        .tradeType(TradeType.BUY)
                        .quantity(BigDecimal.ZERO)
                        .strategyName("TestStrategy")
                        .build()
        );
    }

    @Test
    void testBuilderValidation_InvalidConfidence() {
        assertThrows(IllegalArgumentException.class, () ->
                TradingSignal.builder()
                        .symbol("AAPL")
                        .tradeType(TradeType.BUY)
                        .quantity(new BigDecimal("100"))
                        .strategyName("TestStrategy")
                        .confidence(-0.1)
                        .build()
        );

        assertThrows(IllegalArgumentException.class, () ->
                TradingSignal.builder()
                        .symbol("AAPL")
                        .tradeType(TradeType.BUY)
                        .quantity(new BigDecimal("100"))
                        .strategyName("TestStrategy")
                        .confidence(1.1)
                        .build()
        );
    }

    @Test
    void testEqualsAndHashCode() {
        LocalDateTime timestamp = LocalDateTime.now();
        
        TradingSignal signal1 = TradingSignal.builder()
                .symbol("AAPL")
                .tradeType(TradeType.BUY)
                .quantity(new BigDecimal("100"))
                .strategyName("TestStrategy")
                .timestamp(timestamp)
                .confidence(0.7)
                .build();

        TradingSignal signal2 = TradingSignal.builder()
                .symbol("AAPL")
                .tradeType(TradeType.BUY)
                .quantity(new BigDecimal("100"))
                .strategyName("TestStrategy")
                .timestamp(timestamp)
                .confidence(0.7)
                .build();

        assertEquals(signal1, signal2);
        assertEquals(signal1.hashCode(), signal2.hashCode());
    }

    @Test
    void testToString() {
        TradingSignal signal = TradingSignal.builder()
                .symbol("AAPL")
                .tradeType(TradeType.BUY)
                .quantity(new BigDecimal("100"))
                .strategyName("TestStrategy")
                .confidence(0.8)
                .reason("Test signal")
                .build();

        String toString = signal.toString();
        assertTrue(toString.contains("AAPL"));
        assertTrue(toString.contains("BUY"));
        assertTrue(toString.contains("100"));
        assertTrue(toString.contains("TestStrategy"));
        assertTrue(toString.contains("0.8"));
        assertTrue(toString.contains("Test signal"));
    }
}