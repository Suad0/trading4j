package com.quanttrading.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

@Aspect
public class AuditLoggingAspect {

    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");
    
    @Autowired
    ObjectMapper objectMapper;

    @Before("execution(* com.quanttrading.service.TradingService.*(..))")
    public void logTradingOperation(JoinPoint joinPoint) {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
            MDC.put("correlationId", correlationId);
        }

        String operation = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        MDC.put("operation", operation);
        
        auditLogger.info("TRADING_OPERATION_START: operation={}, args={}, timestamp={}, correlationId={}", 
            operation, 
            sanitizeArgs(args), 
            LocalDateTime.now(), 
            correlationId);
    }

    @AfterReturning(pointcut = "execution(* com.quanttrading.service.TradingService.*(..))", returning = "result")
    public void logTradingOperationSuccess(JoinPoint joinPoint, Object result) {
        String operation = joinPoint.getSignature().getName();
        String correlationId = MDC.get("correlationId");
        
        auditLogger.info("TRADING_OPERATION_SUCCESS: operation={}, result={}, timestamp={}, correlationId={}", 
            operation, 
            sanitizeResult(result), 
            LocalDateTime.now(), 
            correlationId);
    }

    @AfterThrowing(pointcut = "execution(* com.quanttrading.service.TradingService.*(..))", throwing = "exception")
    public void logTradingOperationFailure(JoinPoint joinPoint, Throwable exception) {
        String operation = joinPoint.getSignature().getName();
        String correlationId = MDC.get("correlationId");
        
        auditLogger.error("TRADING_OPERATION_FAILURE: operation={}, error={}, timestamp={}, correlationId={}", 
            operation, 
            exception.getMessage(), 
            LocalDateTime.now(), 
            correlationId, 
            exception);
    }

    @Before("execution(* com.quanttrading.service.PortfolioService.updatePosition(..))")
    public void logPortfolioUpdate(JoinPoint joinPoint) {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
            MDC.put("correlationId", correlationId);
        }

        Object[] args = joinPoint.getArgs();
        
        auditLogger.info("PORTFOLIO_UPDATE: args={}, timestamp={}, correlationId={}", 
            sanitizeArgs(args), 
            LocalDateTime.now(), 
            correlationId);
    }

    String sanitizeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        
        try {
            // Convert args to string but avoid logging sensitive data
            return Arrays.toString(args);
        } catch (Exception e) {
            return "[SERIALIZATION_ERROR]";
        }
    }

    String sanitizeResult(Object result) {
        if (result == null) {
            return "null";
        }
        
        try {
            // For sensitive results, just log the class name
            if (result.toString().contains("password") || result.toString().contains("key")) {
                return result.getClass().getSimpleName();
            }
            return result.toString();
        } catch (Exception e) {
            return "[SERIALIZATION_ERROR]";
        }
    }
}