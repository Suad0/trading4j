package com.quanttrading.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class SymbolValidator implements ConstraintValidator<ValidSymbol, String> {
    
    private static final Pattern SYMBOL_PATTERN = Pattern.compile("^[A-Z]{1,5}$");
    
    @Override
    public void initialize(ValidSymbol constraintAnnotation) {
        // No initialization needed
    }
    
    @Override
    public boolean isValid(String symbol, ConstraintValidatorContext context) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return false;
        }
        
        return SYMBOL_PATTERN.matcher(symbol.trim().toUpperCase()).matches();
    }
}