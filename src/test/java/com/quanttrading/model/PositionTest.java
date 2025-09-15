package com.quanttrading.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import jakarta.persistence.PersistenceContext;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.Validation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@SpringJUnitConfig
class PositionTest {

    @PersistenceContext
    private EntityManager entityManager;
    
    private Validator validator;
    private Position position;
    private Portfolio portfolio;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        portfolio = new Portfolio("TEST_ACCOUNT", 
                                 new BigDecimal("10000.00"), 
                                 new BigDecimal("15000.00"));
        
        position = new Position("AAPL", new BigDecimal("10"), new BigDecimal("150.00"));
    }

    @Test
    void testPositionCreation() {
        assertEquals("AAPL", position.getSymbol());
        assertEquals(new BigDecimal("10"), position.getQuantity());
        assertEquals(new BigDecimal("150.00"), position.getAveragePrice());
        assertNotNull(position.getLastUpdated());
        assertNull(position.getCurrentPrice());
        assertNull(position.getUnrealizedPnL());
    }

    @Test
    void testPositionValidation() {
        Set<ConstraintViolation<Position>> violations = validator.validate(position);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testInvalidSymbol() {
        position.setSymbol("");
        Set<ConstraintViolation<Position>> violations = validator.validate(position);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("symbol")));
    }

    @Test
    void testNullQuantity() {
        position.setQuantity(null);
        Set<ConstraintViolation<Position>> violations = validator.validate(position);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("quantity")));
    }

    @Test
    void testNegativeAveragePrice() {
        position.setAveragePrice(new BigDecimal("-10.00"));
        Set<ConstraintViolation<Position>> violations = validator.validate(position);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("averagePrice")));
    }

    @Test
    void testUnrealizedPnLCalculation() {
        position.setCurrentPrice(new BigDecimal("160.00"));
        
        // Should calculate: (160.00 - 150.00) * 10 = 100.00
        assertEquals(new BigDecimal("100.00"), position.getUnrealizedPnL());
    }

    @Test
    void testNegativeUnrealizedPnL() {
        position.setCurrentPrice(new BigDecimal("140.00"));
        
        // Should calculate: (140.00 - 150.00) * 10 = -100.00
        assertEquals(new BigDecimal("-100.00"), position.getUnrealizedPnL());
    }

    @Test
    void testMarketValue() {
        position.setCurrentPrice(new BigDecimal("160.00"));
        
        // Should calculate: 160.00 * 10 = 1600.00
        assertEquals(new BigDecimal("1600.00"), position.getMarketValue());
    }

    @Test
    void testMarketValueWithoutCurrentPrice() {
        assertEquals(BigDecimal.ZERO, position.getMarketValue());
    }

    @Test
    void testPortfolioRelationship() {
        portfolio.addPosition(position);
        
        assertEquals(portfolio, position.getPortfolio());
        assertTrue(portfolio.getPositions().contains(position));
    }

    @Test
    void testTimestampUpdate() {
        LocalDateTime originalTimestamp = position.getLastUpdated();
        
        // Wait a small amount to ensure timestamp difference
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        position.setQuantity(new BigDecimal("15"));
        assertTrue(position.getLastUpdated().isAfter(originalTimestamp));
    }

    @Test
    void testPersistenceWithPortfolio() {
        entityManager.persist(portfolio);
        portfolio.addPosition(position);
        entityManager.persist(position);
        entityManager.flush();
        entityManager.clear();
        
        Position found = entityManager.find(Position.class, position.getId());
        assertNotNull(found);
        assertEquals("AAPL", found.getSymbol());
        assertEquals(new BigDecimal("10"), found.getQuantity());
        assertEquals(new BigDecimal("150.00"), found.getAveragePrice());
        assertNotNull(found.getPortfolio());
        assertEquals("TEST_ACCOUNT", found.getPortfolio().getAccountId());
    }

    @Test
    void testZeroQuantityPosition() {
        position.setQuantity(BigDecimal.ZERO);
        Set<ConstraintViolation<Position>> violations = validator.validate(position);
        assertTrue(violations.isEmpty()); // Zero quantity should be valid
    }
}