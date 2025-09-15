package com.quanttrading.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.Validation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple unit tests for entity validation without Spring context
 */
class SimpleEntityTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testPortfolioValidation() {
        Portfolio portfolio = new Portfolio("TEST_ACCOUNT", 
                                           new BigDecimal("10000.00"), 
                                           new BigDecimal("15000.00"));
        
        Set<ConstraintViolation<Portfolio>> violations = validator.validate(portfolio);
        assertTrue(violations.isEmpty());
        
        assertEquals("TEST_ACCOUNT", portfolio.getAccountId());
        assertEquals(new BigDecimal("10000.00"), portfolio.getCashBalance());
        assertEquals(new BigDecimal("15000.00"), portfolio.getTotalValue());
        assertNotNull(portfolio.getLastUpdated());
    }

    @Test
    void testPositionValidation() {
        Position position = new Position("AAPL", new BigDecimal("10"), new BigDecimal("150.00"));
        
        Set<ConstraintViolation<Position>> violations = validator.validate(position);
        assertTrue(violations.isEmpty());
        
        assertEquals("AAPL", position.getSymbol());
        assertEquals(new BigDecimal("10"), position.getQuantity());
        assertEquals(new BigDecimal("150.00"), position.getAveragePrice());
        assertNotNull(position.getLastUpdated());
    }

    @Test
    void testTradeValidation() {
        Trade trade = new Trade("ORDER_123", "AAPL", TradeType.BUY, 
                               new BigDecimal("10"), new BigDecimal("150.00"), OrderStatus.PENDING);
        
        Set<ConstraintViolation<Trade>> violations = validator.validate(trade);
        assertTrue(violations.isEmpty());
        
        assertEquals("ORDER_123", trade.getOrderId());
        assertEquals("AAPL", trade.getSymbol());
        assertEquals(TradeType.BUY, trade.getType());
        assertEquals(new BigDecimal("10"), trade.getQuantity());
        assertEquals(new BigDecimal("150.00"), trade.getPrice());
        assertEquals(OrderStatus.PENDING, trade.getStatus());
    }

    @Test
    void testMarketDataValidation() {
        MarketData marketData = new MarketData("AAPL", 
                                              new BigDecimal("155.50"), 
                                              new BigDecimal("1000000"), 
                                              new BigDecimal("157.00"), 
                                              new BigDecimal("154.00"), 
                                              new BigDecimal("156.00"), 
                                              LocalDateTime.now());
        
        Set<ConstraintViolation<MarketData>> violations = validator.validate(marketData);
        assertTrue(violations.isEmpty());
        
        assertEquals("AAPL", marketData.getSymbol());
        assertEquals(new BigDecimal("155.50"), marketData.getPrice());
        assertEquals(new BigDecimal("1000000"), marketData.getVolume());
        assertTrue(marketData.isValidOHLC());
    }

    @Test
    void testPositionUnrealizedPnLCalculation() {
        Position position = new Position("AAPL", new BigDecimal("10"), new BigDecimal("150.00"));
        position.setCurrentPrice(new BigDecimal("160.00"));
        
        // Should calculate: (160.00 - 150.00) * 10 = 100.00
        assertEquals(new BigDecimal("100.00"), position.getUnrealizedPnL());
    }

    @Test
    void testPortfolioPositionRelationship() {
        Portfolio portfolio = new Portfolio("TEST_ACCOUNT", 
                                           new BigDecimal("10000.00"), 
                                           new BigDecimal("15000.00"));
        Position position = new Position("AAPL", new BigDecimal("10"), new BigDecimal("150.00"));
        
        portfolio.addPosition(position);
        
        assertEquals(1, portfolio.getPositions().size());
        assertEquals(portfolio, position.getPortfolio());
        assertTrue(portfolio.getPositions().contains(position));
    }

    @Test
    void testTradeStatusUpdate() {
        Trade trade = new Trade("ORDER_123", "AAPL", TradeType.BUY, 
                               new BigDecimal("10"), new BigDecimal("150.00"), OrderStatus.PENDING);
        
        assertNull(trade.getExecutedAt());
        
        trade.setStatus(OrderStatus.FILLED);
        
        assertEquals(OrderStatus.FILLED, trade.getStatus());
        assertNotNull(trade.getExecutedAt());
    }

    @Test
    void testInvalidEntityValidation() {
        // Test invalid portfolio with negative cash balance
        Portfolio invalidPortfolio = new Portfolio("TEST", new BigDecimal("-1000"), new BigDecimal("5000"));
        Set<ConstraintViolation<Portfolio>> portfolioViolations = validator.validate(invalidPortfolio);
        assertFalse(portfolioViolations.isEmpty());
        
        // Test invalid position with empty symbol
        Position invalidPosition = new Position("", new BigDecimal("10"), new BigDecimal("150.00"));
        Set<ConstraintViolation<Position>> positionViolations = validator.validate(invalidPosition);
        assertFalse(positionViolations.isEmpty());
        
        // Test invalid trade with zero quantity
        Trade invalidTrade = new Trade("ORDER_123", "AAPL", TradeType.BUY, 
                                      BigDecimal.ZERO, new BigDecimal("150.00"), OrderStatus.PENDING);
        Set<ConstraintViolation<Trade>> tradeViolations = validator.validate(invalidTrade);
        assertFalse(tradeViolations.isEmpty());
    }
}