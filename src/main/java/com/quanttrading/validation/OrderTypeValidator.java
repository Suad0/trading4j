package com.quanttrading.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;

public class OrderTypeValidator implements ConstraintValidator<ValidOrderType, String> {
    
    private static final Set<String> VALID_ORDER_TYPES = Set.of(
        "MARKET", "LIMIT", "STOP", "STOP_LIMIT"
    );
    
    @Override
    public void initialize(ValidOrderType constraintAnnotation) {
        // No initialization needed
    }
    
    @Override
    public boolean isValid(String orderType, ConstraintValidatorContext context) {
        if (orderType == null || orderType.trim().isEmpty()) {
            return false;
        }
        
        return VALID_ORDER_TYPES.contains(orderType.trim().toUpperCase());
    }
}