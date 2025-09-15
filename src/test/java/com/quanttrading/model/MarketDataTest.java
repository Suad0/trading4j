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
class MarketDataTest {

    @PersistenceContext
    private EntityManager entityManager;
    
    private Validator validator;
    private MarketData marketData;
    private LocalDateTime testTimestamp;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        testTimestamp = LocalDateTime.now().minusHours(1);
        marketData = new MarketData("AAPL", 
                                   new BigDecimal("155.50"), 
                                   new BigDecimal("1000000"), 
                                   new BigDecimal("157.00"), 
                                   new BigDecimal("154.00"), 
                                   new BigDecimal("156.00"), 
                                   testTimestamp);
    }

    @Test
    void testMarketDataCreation() {
        assertEquals("AAPL", marketData.getSymbol());
        assertEquals(new BigDecimal("155.50"), marketData.getPrice());
        assertEquals(new BigDecimal("1000000"), marketData.getVolume());
        assertEquals(new BigDecimal("157.00"), marketData.getHigh());
        assertEquals(new BigDecimal("154.00"), marketData.getLow());
        assertEquals(new BigDecimal("156.00"), marketData.getOpen());
        assertEquals(testTimestamp, marketData.getTimestamp());
        assertNotNull(marketData.getCreatedAt());
    }

    @Test
    void testMarketDataValidation() {
        Set<ConstraintViolation<MarketData>> violations = validator.validate(marketData);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testInvalidSymbol() {
        marketData.setSymbol("");
        Set<ConstraintViolation<MarketData>> violations = validator.validate(marketData);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("symbol")));
    }

    @Test
    void testNullSymbol() {
        marketData.setSymbol(null);
        Set<ConstraintViolation<MarketData>> violations = validator.validate(marketData);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("symbol")));
    }

    @Test
    void testNegativePrice() {
        marketData.setPrice(new BigDecimal("-10.00"));
        Set<ConstraintViolation<MarketData>> violations = validator.validate(marketData);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("price")));
    }

    @Test
    void testNegativeVolume() {
        marketData.setVolume(new BigDecimal("-1000"));
        Set<ConstraintViolation<MarketData>> violations = validator.validate(marketData);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("volume")));
    }

    @Test
    void testNegativeHigh() {
        marketData.setHigh(new BigDecimal("-10.00"));
        Set<ConstraintViolation<MarketData>> violations = validator.validate(marketData);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("high")));
    }

    @Test
    void testNegativeLow() {
        marketData.setLow(new BigDecimal("-10.00"));
        Set<ConstraintViolation<MarketData>> violations = validator.validate(marketData);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("low")));
    }

    @Test
    void testNegativeOpen() {
        marketData.setOpen(new BigDecimal("-10.00"));
        Set<ConstraintViolation<MarketData>> violations = validator.validate(marketData);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("open")));
    }

    @Test
    void testGetClose() {
        assertEquals(marketData.getPrice(), marketData.getClose());
    }

    @Test
    void testValidOHLC() {
        assertTrue(marketData.isValidOHLC());
    }

    @Test
    void testInvalidOHLCHighLowerThanLow() {
        marketData.setHigh(new BigDecimal("150.00"));
        marketData.setLow(new BigDecimal("160.00"));
        assertFalse(marketData.isValidOHLC());
    }

    @Test
    void testInvalidOHLCHighLowerThanOpen() {
        marketData.setHigh(new BigDecimal("150.00"));
        marketData.setOpen(new BigDecimal("160.00"));
        assertFalse(marketData.isValidOHLC());
    }

    @Test
    void testInvalidOHLCHighLowerThanPrice() {
        marketData.setHigh(new BigDecimal("150.00"));
        marketData.setPrice(new BigDecimal("160.00"));
        assertFalse(marketData.isValidOHLC());
    }

    @Test
    void testInvalidOHLCLowHigherThanOpen() {
        marketData.setLow(new BigDecimal("160.00"));
        marketData.setOpen(new BigDecimal("150.00"));
        assertFalse(marketData.isValidOHLC());
    }

    @Test
    void testInvalidOHLCLowHigherThanPrice() {
        marketData.setLow(new BigDecimal("160.00"));
        marketData.setPrice(new BigDecimal("150.00"));
        assertFalse(marketData.isValidOHLC());
    }

    @Test
    void testZeroValues() {
        MarketData zeroData = new MarketData("TEST", 
                                           BigDecimal.ZERO, 
                                           BigDecimal.ZERO, 
                                           BigDecimal.ZERO, 
                                           BigDecimal.ZERO, 
                                           BigDecimal.ZERO, 
                                           LocalDateTime.now());
        
        Set<ConstraintViolation<MarketData>> violations = validator.validate(zeroData);
        assertTrue(violations.isEmpty()); // Zero values should be valid
        assertTrue(zeroData.isValidOHLC());
    }

    @Test
    void testPersistence() {
        entityManager.persist(marketData);
        entityManager.flush();
        entityManager.clear();
        
        MarketData found = entityManager.find(MarketData.class, marketData.getId());
        assertNotNull(found);
        assertEquals("AAPL", found.getSymbol());
        assertEquals(new BigDecimal("155.50"), found.getPrice());
        assertEquals(new BigDecimal("1000000"), found.getVolume());
        assertEquals(new BigDecimal("157.00"), found.getHigh());
        assertEquals(new BigDecimal("154.00"), found.getLow());
        assertEquals(new BigDecimal("156.00"), found.getOpen());
        assertEquals(testTimestamp, found.getTimestamp());
    }

    @Test
    void testPrePersistCreatedAt() {
        MarketData newData = new MarketData();
        newData.setSymbol("GOOGL");
        newData.setPrice(new BigDecimal("2500.00"));
        newData.setVolume(new BigDecimal("500000"));
        newData.setHigh(new BigDecimal("2510.00"));
        newData.setLow(new BigDecimal("2490.00"));
        newData.setOpen(new BigDecimal("2495.00"));
        newData.setTimestamp(LocalDateTime.now());
        
        assertNotNull(newData.getCreatedAt()); // Should be set in constructor
        
        LocalDateTime originalCreatedAt = newData.getCreatedAt();
        newData.setCreatedAt(null);
        
        entityManager.persist(newData);
        entityManager.flush();
        
        assertNotNull(newData.getCreatedAt()); // Should be set in @PrePersist
    }

    @Test
    void testEqualPricesOHLC() {
        MarketData equalData = new MarketData("TEST", 
                                            new BigDecimal("100.00"), 
                                            new BigDecimal("1000"), 
                                            new BigDecimal("100.00"), 
                                            new BigDecimal("100.00"), 
                                            new BigDecimal("100.00"), 
                                            LocalDateTime.now());
        
        assertTrue(equalData.isValidOHLC());
        assertEquals(equalData.getPrice(), equalData.getClose());
    }
}