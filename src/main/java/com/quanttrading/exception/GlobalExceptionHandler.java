package com.quanttrading.exception;

import com.quanttrading.dto.ErrorResponse;
import com.quanttrading.util.LoggingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Global exception handler for all REST controllers
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handle trading system specific exceptions
     */
    @ExceptionHandler(TradingSystemException.class)
    public ResponseEntity<ErrorResponse> handleTradingSystemException(
            TradingSystemException ex, WebRequest request) {
        
        HttpStatus status = determineHttpStatus(ex);
        ErrorResponse errorResponse = createErrorResponse(
            ex.getClass().getSimpleName(),
            ex.getMessage(),
            request.getDescription(false),
            status.value()
        );
        
        // Add specific metadata based on exception type
        addExceptionMetadata(errorResponse, ex);
        
        LoggingUtils.logOperationError("EXCEPTION_HANDLING", null, 
            ex.getClass().getSimpleName(), ex.getMessage(), ex);
        
        return new ResponseEntity<>(errorResponse, status);
    }
    
    /**
     * Handle validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        ErrorResponse errorResponse = createErrorResponse(
            "VALIDATION_ERROR",
            "Request validation failed",
            request.getDescription(false),
            HttpStatus.BAD_REQUEST.value()
        );
        
        List<ErrorResponse.ValidationError> validationErrors = new ArrayList<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            Object rejectedValue = ((FieldError) error).getRejectedValue();
            validationErrors.add(new ErrorResponse.ValidationError(fieldName, errorMessage, rejectedValue));
        });
        
        errorResponse.setValidationErrors(validationErrors);
        
        logger.warn("Validation error: {} validation errors found", validationErrors.size());
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handle bind exceptions
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(
            BindException ex, WebRequest request) {
        
        ErrorResponse errorResponse = createErrorResponse(
            "BINDING_ERROR",
            "Request binding failed",
            request.getDescription(false),
            HttpStatus.BAD_REQUEST.value()
        );
        
        List<ErrorResponse.ValidationError> validationErrors = new ArrayList<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = error instanceof FieldError ? ((FieldError) error).getField() : error.getObjectName();
            String errorMessage = error.getDefaultMessage();
            Object rejectedValue = error instanceof FieldError ? ((FieldError) error).getRejectedValue() : null;
            validationErrors.add(new ErrorResponse.ValidationError(fieldName, errorMessage, rejectedValue));
        });
        
        errorResponse.setValidationErrors(validationErrors);
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handle constraint violation exceptions
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {
        
        ErrorResponse errorResponse = createErrorResponse(
            "CONSTRAINT_VIOLATION",
            "Constraint validation failed",
            request.getDescription(false),
            HttpStatus.BAD_REQUEST.value()
        );
        
        List<ErrorResponse.ValidationError> validationErrors = new ArrayList<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            Object rejectedValue = violation.getInvalidValue();
            validationErrors.add(new ErrorResponse.ValidationError(fieldName, errorMessage, rejectedValue));
        }
        
        errorResponse.setValidationErrors(validationErrors);
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handle missing request parameters
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameterException(
            MissingServletRequestParameterException ex, WebRequest request) {
        
        ErrorResponse errorResponse = createErrorResponse(
            "MISSING_PARAMETER",
            String.format("Required parameter '%s' is missing", ex.getParameterName()),
            request.getDescription(false),
            HttpStatus.BAD_REQUEST.value()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handle method argument type mismatch
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        
        ErrorResponse errorResponse = createErrorResponse(
            "TYPE_MISMATCH",
            String.format("Parameter '%s' should be of type '%s'", 
                ex.getName(), ex.getRequiredType().getSimpleName()),
            request.getDescription(false),
            HttpStatus.BAD_REQUEST.value()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handle HTTP message not readable
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, WebRequest request) {
        
        ErrorResponse errorResponse = createErrorResponse(
            "MALFORMED_REQUEST",
            "Request body is malformed or missing",
            request.getDescription(false),
            HttpStatus.BAD_REQUEST.value()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handle method not supported
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex, WebRequest request) {
        
        ErrorResponse errorResponse = createErrorResponse(
            "METHOD_NOT_SUPPORTED",
            String.format("HTTP method '%s' is not supported for this endpoint", ex.getMethod()),
            request.getDescription(false),
            HttpStatus.METHOD_NOT_ALLOWED.value()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.METHOD_NOT_ALLOWED);
    }
    
    /**
     * Handle no handler found
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(
            NoHandlerFoundException ex, WebRequest request) {
        
        ErrorResponse errorResponse = createErrorResponse(
            "ENDPOINT_NOT_FOUND",
            String.format("No handler found for %s %s", ex.getHttpMethod(), ex.getRequestURL()),
            request.getDescription(false),
            HttpStatus.NOT_FOUND.value()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }
    
    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {
        
        ErrorResponse errorResponse = createErrorResponse(
            "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred",
            request.getDescription(false),
            HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        
        logger.error("Unexpected error occurred", ex);
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * Create standard error response
     */
    private ErrorResponse createErrorResponse(String error, String message, String path, int status) {
        ErrorResponse errorResponse = new ErrorResponse(error, message, path, status);
        errorResponse.setCorrelationId(LoggingUtils.getCorrelationId());
        return errorResponse;
    }
    
    /**
     * Determine HTTP status based on exception type
     */
    private HttpStatus determineHttpStatus(TradingSystemException ex) {
        if (ex instanceof InvalidOrderException || ex instanceof ConfigurationException) {
            return HttpStatus.BAD_REQUEST;
        } else if (ex instanceof InsufficientFundsException) {
            return HttpStatus.CONFLICT;
        } else if (ex instanceof ApiConnectionException || ex instanceof MarketDataException) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        } else if (ex instanceof PortfolioException || ex instanceof StrategyExecutionException) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
    
    /**
     * Add exception-specific metadata to error response
     */
    private void addExceptionMetadata(ErrorResponse errorResponse, TradingSystemException ex) {
        Map<String, Object> metadata = new HashMap<>();
        
        if (ex instanceof StrategyExecutionException) {
            metadata.put("strategyName", ((StrategyExecutionException) ex).getStrategyName());
        } else if (ex instanceof MarketDataException) {
            metadata.put("symbol", ((MarketDataException) ex).getSymbol());
        } else if (ex instanceof ConfigurationException) {
            metadata.put("configKey", ((ConfigurationException) ex).getConfigKey());
        }
        
        if (!metadata.isEmpty()) {
            errorResponse.setMetadata(metadata);
        }
    }
}