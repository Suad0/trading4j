package com.quanttrading.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class OrderTypeValidatorTest {

    private OrderTypeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new OrderTypeValidator();
        validator.initialize(null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MARKET", "LIMIT", "STOP", "STOP_LIMIT"})
    void isValid_WithValidOrderTypes_ShouldReturnTrue(String orderType) {
        assertTrue(validator.isValid(orderType, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"market", "limit", "stop", "stop_limit"})
    void isValid_WithLowercaseOrderTypes_ShouldReturnTrue(String orderType) {
        // Validator should handle case conversion
        assertTrue(validator.isValid(orderType, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"INVALID", "FOK", "IOC", "GTC", "", " "})
    void isValid_WithInvalidOrderTypes_ShouldReturnFalse(String orderType) {
        assertFalse(validator.isValid(orderType, null));
    }

    @Test
    void isValid_WithNullOrderType_ShouldReturnFalse() {
        assertFalse(validator.isValid(null, null));
    }

    @Test
    void isValid_WithWhitespaceOrderType_ShouldReturnTrue() {
        // Should trim whitespace and validate
        assertTrue(validator.isValid(" MARKET ", null));
    }
}