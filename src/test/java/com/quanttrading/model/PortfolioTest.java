package com.quanttrading.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
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
class PortfolioTest {

    @PersistenceContext
    private EntityManager entityManager;
    
    private Validator validator;
    private Portfolio portfolio;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        portfolio = new Portfolio("TEST_ACCOUNT", 
                                 new BigDecimal("10000.00"), 
                                 new BigDecimal("15000.00"));
    }

    @Test
    void testPortfolioCreation() {
        assertNotNull(portfolio.getAccountId());
        assertEquals("TEST_ACCOUNT", portfolio.getAccountId());
        assertEquals(new BigDecimal("10000.00"), portfolio.getCashBalance());
        assertEquals(new BigDecimal("15000.00"), portfolio.getTotalValue());
        assertNotNull(portfolio.getLastUpdated());
        assertTrue(portfolio.getPositions().isEmpty());
    }

    @Test
    void testPortfolioValidation() {
        Set<ConstraintViolation<Portfolio>> violations = validator.validate(portfolio);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testInvalidCashBalance() {
        portfolio.setCashBalance(new BigDecimal("-1000.00"));
        Set<ConstraintViolation<Portfolio>> violations = validator.validate(portfolio);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("cashBalance")));
    }

    @Test
    void testInvalidTotalValue() {
        portfolio.setTotalValue(new BigDecimal("-5000.00"));
        Set<ConstraintViolation<Portfolio>> violations = validator.validate(portfolio);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("totalValue")));
    }

    @Test
    void testNullAccountId() {
        portfolio.setAccountId(null);
        Set<ConstraintViolation<Portfolio>> violations = validator.validate(portfolio);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testAddPosition() {
        Position position = new Position("AAPL", new BigDecimal("10"), new BigDecimal("150.00"));
        portfolio.addPosition(position);
        
        assertEquals(1, portfolio.getPositions().size());
        assertEquals(portfolio, position.getPortfolio());
        assertTrue(portfolio.getPositions().contains(position));
    }

    @Test
    void testRemovePosition() {
        Position position = new Position("AAPL", new BigDecimal("10"), new BigDecimal("150.00"));
        portfolio.addPosition(position);
        
        portfolio.removePosition(position);
        
        assertEquals(0, portfolio.getPositions().size());
        assertNull(position.getPortfolio());
        assertFalse(portfolio.getPositions().contains(position));
    }

    @Test
    void testTimestampUpdate() {
        LocalDateTime originalTimestamp = portfolio.getLastUpdated();
        
        // Wait a small amount to ensure timestamp difference
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        portfolio.setCashBalance(new BigDecimal("12000.00"));
        assertTrue(portfolio.getLastUpdated().isAfter(originalTimestamp));
    }

    @Test
    void testPersistence() {
        entityManager.persist(portfolio);
        entityManager.flush();
        entityManager.clear();
        
        Portfolio found = entityManager.find(Portfolio.class, "TEST_ACCOUNT");
        assertNotNull(found);
        assertEquals("TEST_ACCOUNT", found.getAccountId());
        assertEquals(new BigDecimal("10000.00"), found.getCashBalance());
        assertEquals(new BigDecimal("15000.00"), found.getTotalValue());
    }
}