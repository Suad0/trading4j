package com.quanttrading.strategy;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StrategyConfigTest {

    @Test
    void testBuilderWithDefaults() {
        StrategyConfig config = StrategyConfig.builder("TestStrategy").build();

        assertEquals("TestStrategy", config.getStrategyName());
        assertEquals(new BigDecimal("1000"), config.getMaxPositionSize());
        assertEquals(new BigDecimal("0.02"), config.getRiskPerTrade());
        assertEquals(0.6, config.getMinConfidence());
        assertTrue(config.isEnabled());
        assertTrue(config.getParameters().isEmpty());
    }

    @Test
    void testBuilderWithCustomValues() {
        StrategyConfig config = StrategyConfig.builder("SMAStrategy")
                .maxPositionSize(new BigDecimal("5000"))
                .riskPerTrade(new BigDecimal("0.05"))
                .minConfidence(0.8)
                .enabled(false)
                .parameter("shortPeriod", 10)
                .parameter("longPeriod", 20)
                .build();

        assertEquals("SMAStrategy", config.getStrategyName());
        assertEquals(new BigDecimal("5000"), config.getMaxPositionSize());
        assertEquals(new BigDecimal("0.05"), config.getRiskPerTrade());
        assertEquals(0.8, config.getMinConfidence());
        assertFalse(config.isEnabled());
        assertEquals(2, config.getParameters().size());
        assertEquals(10, config.getParameter("shortPeriod", Integer.class));
        assertEquals(20, config.getParameter("longPeriod", Integer.class));
    }

    @Test
    void testParameterMethods() {
        StrategyConfig config = StrategyConfig.builder("TestStrategy")
                .parameter("intParam", 42)
                .parameter("stringParam", "test")
                .parameter("doubleParam", 3.14)
                .build();

        // Test getParameter with type
        assertEquals(42, config.getParameter("intParam", Integer.class));
        assertEquals("test", config.getParameter("stringParam", String.class));
        assertEquals(3.14, config.getParameter("doubleParam", Double.class));

        // Test getParameter with default value
        assertEquals(42, config.getParameter("intParam", Integer.class, 0));
        assertEquals(100, config.getParameter("missingParam", Integer.class, 100));

        // Test null parameter
        assertNull(config.getParameter("missingParam", String.class));
    }

    @Test
    void testParameterTypeValidation() {
        StrategyConfig config = StrategyConfig.builder("TestStrategy")
                .parameter("intParam", 42)
                .build();

        // Should work with correct type
        assertEquals(42, config.getParameter("intParam", Integer.class));

        // Should throw exception with wrong type
        assertThrows(IllegalArgumentException.class, () ->
                config.getParameter("intParam", String.class)
        );
    }

    @Test
    void testParametersMap() {
        Map<String, Object> params = Map.of(
                "param1", "value1",
                "param2", 123,
                "param3", true
        );

        StrategyConfig config = StrategyConfig.builder("TestStrategy")
                .parameters(params)
                .build();

        Map<String, Object> retrievedParams = config.getParameters();
        assertEquals(3, retrievedParams.size());
        assertEquals("value1", retrievedParams.get("param1"));
        assertEquals(123, retrievedParams.get("param2"));
        assertEquals(true, retrievedParams.get("param3"));

        // Ensure returned map is a copy (immutable)
        assertNotSame(params, retrievedParams);
    }

    @Test
    void testBuilderValidation_NullStrategyName() {
        assertThrows(NullPointerException.class, () ->
                StrategyConfig.builder(null)
        );
    }

    @Test
    void testBuilderValidation_NegativeMaxPositionSize() {
        assertThrows(IllegalArgumentException.class, () ->
                StrategyConfig.builder("TestStrategy")
                        .maxPositionSize(new BigDecimal("-100"))
                        .build()
        );
    }

    @Test
    void testBuilderValidation_ZeroMaxPositionSize() {
        assertThrows(IllegalArgumentException.class, () ->
                StrategyConfig.builder("TestStrategy")
                        .maxPositionSize(BigDecimal.ZERO)
                        .build()
        );
    }

    @Test
    void testBuilderValidation_InvalidRiskPerTrade() {
        // Negative risk
        assertThrows(IllegalArgumentException.class, () ->
                StrategyConfig.builder("TestStrategy")
                        .riskPerTrade(new BigDecimal("-0.01"))
                        .build()
        );

        // Zero risk
        assertThrows(IllegalArgumentException.class, () ->
                StrategyConfig.builder("TestStrategy")
                        .riskPerTrade(BigDecimal.ZERO)
                        .build()
        );

        // Risk > 1
        assertThrows(IllegalArgumentException.class, () ->
                StrategyConfig.builder("TestStrategy")
                        .riskPerTrade(new BigDecimal("1.1"))
                        .build()
        );
    }

    @Test
    void testBuilderValidation_InvalidMinConfidence() {
        // Negative confidence
        assertThrows(IllegalArgumentException.class, () ->
                StrategyConfig.builder("TestStrategy")
                        .minConfidence(-0.1)
                        .build()
        );

        // Confidence > 1
        assertThrows(IllegalArgumentException.class, () ->
                StrategyConfig.builder("TestStrategy")
                        .minConfidence(1.1)
                        .build()
        );
    }

    @Test
    void testEqualsAndHashCode() {
        StrategyConfig config1 = StrategyConfig.builder("TestStrategy")
                .maxPositionSize(new BigDecimal("1000"))
                .parameter("param1", "value1")
                .build();

        StrategyConfig config2 = StrategyConfig.builder("TestStrategy")
                .maxPositionSize(new BigDecimal("1000"))
                .parameter("param1", "value1")
                .build();

        assertEquals(config1, config2);
        assertEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    void testToString() {
        StrategyConfig config = StrategyConfig.builder("TestStrategy")
                .parameter("param1", "value1")
                .build();

        String toString = config.toString();
        assertTrue(toString.contains("TestStrategy"));
        assertTrue(toString.contains("param1"));
        assertTrue(toString.contains("value1"));
    }
}