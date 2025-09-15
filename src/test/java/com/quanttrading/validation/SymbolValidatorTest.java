package com.quanttrading.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class SymbolValidatorTest {

    private SymbolValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SymbolValidator();
        validator.initialize(null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"AAPL", "GOOGL", "MSFT", "TSLA", "A", "AMZN"})
    void isValid_WithValidSymbols_ShouldReturnTrue(String symbol) {
        assertTrue(validator.isValid(symbol, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"aapl", "googl", "msft"})
    void isValid_WithLowercaseSymbols_ShouldReturnTrue(String symbol) {
        // Validator should handle case conversion
        assertTrue(validator.isValid(symbol, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "TOOLONG", "123", "AAPL123", "AA-PL", "AA.PL"})
    void isValid_WithInvalidSymbols_ShouldReturnFalse(String symbol) {
        assertFalse(validator.isValid(symbol, null));
    }

    @Test
    void isValid_WithNullSymbol_ShouldReturnFalse() {
        assertFalse(validator.isValid(null, null));
    }

    @Test
    void isValid_WithWhitespaceSymbol_ShouldReturnTrue() {
        // Should trim whitespace and validate
        assertTrue(validator.isValid(" AAPL ", null));
    }
}