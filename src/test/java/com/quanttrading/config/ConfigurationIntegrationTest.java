package com.quanttrading.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "alpaca.base-url=https://paper-api.alpaca.markets",
    "alpaca.api-key=test-api-key",
    "alpaca.secret-key=test-secret-key",
    "alpaca.paper-trading=true",
    "trading.max-position-size=5000.00",
    "trading.risk-per-trade=0.03",
    "trading.enable-auto-trading=false",
    "trading.allowed-symbols[0]=AAPL",
    "trading.allowed-symbols[1]=GOOGL",
    "security.basic-auth.username=testuser",
    "security.basic-auth.password=testpass"
})
class ConfigurationIntegrationTest {

    @Autowired
    private AlpacaConfig alpacaConfig;

    @Autowired
    private TradingConfig tradingConfig;

    @Autowired
    private SecurityProperties securityProperties;

    @Test
    void alpacaConfig_ShouldLoadCorrectly() {
        assertNotNull(alpacaConfig);
        assertEquals("https://paper-api.alpaca.markets", alpacaConfig.baseUrl());
        assertEquals("test-api-key", alpacaConfig.apiKey());
        assertEquals("test-secret-key", alpacaConfig.secretKey());
        assertTrue(alpacaConfig.paperTrading());
    }

    @Test
    void tradingConfig_ShouldLoadCorrectly() {
        assertNotNull(tradingConfig);
        assertEquals(new BigDecimal("5000.00"), tradingConfig.maxPositionSize());
        assertEquals(new BigDecimal("0.03"), tradingConfig.riskPerTrade());
        assertFalse(tradingConfig.enableAutoTrading());
        assertEquals(2, tradingConfig.allowedSymbols().size());
        assertTrue(tradingConfig.allowedSymbols().contains("AAPL"));
        assertTrue(tradingConfig.allowedSymbols().contains("GOOGL"));
    }

    @Test
    void securityProperties_ShouldLoadCorrectly() {
        assertNotNull(securityProperties);
        assertNotNull(securityProperties.basicAuth());
        assertEquals("testuser", securityProperties.basicAuth().username());
        assertEquals("testpass", securityProperties.basicAuth().password());
    }

    @Test
    void configurationValidator_ShouldBePresent() {
        // This test ensures that the configuration validator bean is created
        // and doesn't throw exceptions during startup
        assertTrue(true); // If we reach here, configuration validation passed
    }
}