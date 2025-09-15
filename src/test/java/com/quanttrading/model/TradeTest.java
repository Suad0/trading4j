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
class TradeTest {

    @PersistenceContext
    private EntityManager entityManager;
    
    private Validator validator;
    private Trade trade;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        trade = new Trade("ORDER_123", "AAPL", TradeType.BUY, 
                         new BigDecimal("10"), new BigDecimal("150.00"), OrderStatus.PENDING);
    }

    @Test
    void testTradeCreation() {
        assertEquals("ORDER_123", trade.getOrderId());
        assertEquals("AAPL", trade.getSymbol());
        assertEquals(TradeType.BUY, trade.getType());
        assertEquals(new BigDecimal("10"), trade.getQuantity());
        assertEquals(new BigDecimal("150.00"), trade.getPrice());
        assertEquals(OrderStatus.PENDING, trade.getStatus());
        assertNotNull(trade.getCreatedAt());
        assertNotNull(trade.getUpdatedAt());
        assertNull(trade.getExecutedAt());
        assertNull(trade.getStrategyName());
    }

    @Test
    void testTradeValidation() {
        Set<ConstraintViolation<Trade>> violations = validator.validate(trade);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testInvalidSymbol() {
        trade.setSymbol("");
        Set<ConstraintViolation<Trade>> violations = validator.validate(trade);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("symbol")));
    }

    @Test
    void testNullTradeType() {
        trade.setType(null);
        Set<ConstraintViolation<Trade>> violations = validator.validate(trade);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("type")));
    }

    @Test
    void testZeroQuantity() {
        trade.setQuantity(BigDecimal.ZERO);
        Set<ConstraintViolation<Trade>> violations = validator.validate(trade);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("quantity")));
    }

    @Test
    void testNegativeQuantity() {
        trade.setQuantity(new BigDecimal("-5"));
        Set<ConstraintViolation<Trade>> violations = validator.validate(trade);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("quantity")));
    }

    @Test
    void testZeroPrice() {
        trade.setPrice(BigDecimal.ZERO);
        Set<ConstraintViolation<Trade>> violations = validator.validate(trade);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("price")));
    }

    @Test
    void testNegativePrice() {
        trade.setPrice(new BigDecimal("-100.00"));
        Set<ConstraintViolation<Trade>> violations = validator.validate(trade);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("price")));
    }

    @Test
    void testTotalValueCalculation() {
        BigDecimal expectedTotal = new BigDecimal("1500.00"); // 10 * 150.00
        assertEquals(expectedTotal, trade.getTotalValue());
    }

    @Test
    void testStatusUpdateToFilled() {
        assertNull(trade.getExecutedAt());
        
        trade.setStatus(OrderStatus.FILLED);
        
        assertEquals(OrderStatus.FILLED, trade.getStatus());
        assertNotNull(trade.getExecutedAt());
    }

    @Test
    void testStatusUpdateToCancelled() {
        trade.setStatus(OrderStatus.CANCELLED);
        
        assertEquals(OrderStatus.CANCELLED, trade.getStatus());
        assertNull(trade.getExecutedAt()); // Should not set executed time for cancelled orders
    }

    @Test
    void testTimestampUpdate() {
        LocalDateTime originalUpdatedAt = trade.getUpdatedAt();
        
        // Wait a small amount to ensure timestamp difference
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        trade.setStatus(OrderStatus.SUBMITTED);
        assertTrue(trade.getUpdatedAt().isAfter(originalUpdatedAt));
    }

    @Test
    void testStrategyName() {
        trade.setStrategyName("SMA_CROSSOVER");
        assertEquals("SMA_CROSSOVER", trade.getStrategyName());
    }

    @Test
    void testSellTrade() {
        Trade sellTrade = new Trade("ORDER_456", "GOOGL", TradeType.SELL, 
                                   new BigDecimal("5"), new BigDecimal("2500.00"), OrderStatus.PENDING);
        
        assertEquals(TradeType.SELL, sellTrade.getType());
        assertEquals(new BigDecimal("12500.00"), sellTrade.getTotalValue()); // 5 * 2500.00
    }

    @Test
    void testPersistence() {
        entityManager.persist(trade);
        entityManager.flush();
        entityManager.clear();
        
        Trade found = entityManager.find(Trade.class, "ORDER_123");
        assertNotNull(found);
        assertEquals("ORDER_123", found.getOrderId());
        assertEquals("AAPL", found.getSymbol());
        assertEquals(TradeType.BUY, found.getType());
        assertEquals(new BigDecimal("10"), found.getQuantity());
        assertEquals(new BigDecimal("150.00"), found.getPrice());
        assertEquals(OrderStatus.PENDING, found.getStatus());
    }

    @Test
    void testAllOrderStatuses() {
        for (OrderStatus status : OrderStatus.values()) {
            trade.setStatus(status);
            assertEquals(status, trade.getStatus());
            
            Set<ConstraintViolation<Trade>> violations = validator.validate(trade);
            assertTrue(violations.isEmpty());
        }
    }

    @Test
    void testAllTradeTypes() {
        for (TradeType type : TradeType.values()) {
            trade.setType(type);
            assertEquals(type, trade.getType());
            
            Set<ConstraintViolation<Trade>> violations = validator.validate(trade);
            assertTrue(violations.isEmpty());
        }
    }
}