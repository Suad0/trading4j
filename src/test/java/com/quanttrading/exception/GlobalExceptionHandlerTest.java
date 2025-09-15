package com.quanttrading.exception;

import com.quanttrading.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    private WebRequest createMockWebRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/test/endpoint");
        return new ServletWebRequest(request);
    }

    @Test
    void shouldHandleTradingSystemException() {
        // Given
        TradingSystemException exception = new TradingSystemException("Test trading system error");
        WebRequest request = createMockWebRequest();

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleTradingSystemException(exception, request);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("TradingSystemException", errorResponse.getError());
        assertEquals("Test trading system error", errorResponse.getMessage());
        assertEquals(500, errorResponse.getStatus());
        assertNotNull(errorResponse.getTimestamp());
    }

    @Test
    void shouldHandleApiConnectionException() {
        // Given
        ApiConnectionException exception = new ApiConnectionException("Test API connection error");
        WebRequest request = createMockWebRequest();

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleTradingSystemException(exception, request);

        // Then
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("ApiConnectionException", errorResponse.getError());
        assertEquals("Test API connection error", errorResponse.getMessage());
        assertEquals(503, errorResponse.getStatus());
    }

    @Test
    void shouldHandleInsufficientFundsException() {
        // Given
        InsufficientFundsException exception = new InsufficientFundsException("Test insufficient funds error");
        WebRequest request = createMockWebRequest();

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleTradingSystemException(exception, request);

        // Then
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("InsufficientFundsException", errorResponse.getError());
        assertEquals("Test insufficient funds error", errorResponse.getMessage());
        assertEquals(409, errorResponse.getStatus());
    }

    @Test
    void shouldHandleInvalidOrderException() {
        // Given
        InvalidOrderException exception = new InvalidOrderException("Test invalid order error");
        WebRequest request = createMockWebRequest();

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleTradingSystemException(exception, request);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("InvalidOrderException", errorResponse.getError());
        assertEquals("Test invalid order error", errorResponse.getMessage());
        assertEquals(400, errorResponse.getStatus());
    }

    @Test
    void shouldHandleStrategyExecutionExceptionWithMetadata() {
        // Given
        StrategyExecutionException exception = new StrategyExecutionException("TestStrategy", "Test strategy execution error");
        WebRequest request = createMockWebRequest();

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleTradingSystemException(exception, request);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("StrategyExecutionException", errorResponse.getError());
        assertTrue(errorResponse.getMessage().contains("TestStrategy"));
        assertEquals(500, errorResponse.getStatus());
        assertNotNull(errorResponse.getMetadata());
        assertEquals("TestStrategy", errorResponse.getMetadata().get("strategyName"));
    }

    @Test
    void shouldHandleMarketDataExceptionWithMetadata() {
        // Given
        MarketDataException exception = new MarketDataException("AAPL", "Test market data error");
        WebRequest request = createMockWebRequest();

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleTradingSystemException(exception, request);

        // Then
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("MarketDataException", errorResponse.getError());
        assertTrue(errorResponse.getMessage().contains("AAPL"));
        assertEquals(503, errorResponse.getStatus());
        assertNotNull(errorResponse.getMetadata());
        assertEquals("AAPL", errorResponse.getMetadata().get("symbol"));
    }

    @Test
    void shouldHandleConfigurationExceptionWithMetadata() {
        // Given
        ConfigurationException exception = new ConfigurationException("test.config", "Test configuration error");
        WebRequest request = createMockWebRequest();

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleTradingSystemException(exception, request);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("ConfigurationException", errorResponse.getError());
        assertTrue(errorResponse.getMessage().contains("test.config"));
        assertEquals(400, errorResponse.getStatus());
        assertNotNull(errorResponse.getMetadata());
        assertEquals("test.config", errorResponse.getMetadata().get("configKey"));
    }

    @Test
    void shouldHandlePortfolioException() {
        // Given
        PortfolioException exception = new PortfolioException("Test portfolio error");
        WebRequest request = createMockWebRequest();

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleTradingSystemException(exception, request);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("PortfolioException", errorResponse.getError());
        assertEquals("Test portfolio error", errorResponse.getMessage());
        assertEquals(500, errorResponse.getStatus());
    }

    @Test
    void shouldHandleGenericException() {
        // Given
        RuntimeException exception = new RuntimeException("Test generic error");
        WebRequest request = createMockWebRequest();

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(exception, request);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("INTERNAL_SERVER_ERROR", errorResponse.getError());
        assertEquals("An unexpected error occurred", errorResponse.getMessage());
        assertEquals(500, errorResponse.getStatus());
    }

    @Test
    void shouldDetermineCorrectHttpStatusForExceptions() {
        WebRequest request = createMockWebRequest();

        // Test BAD_REQUEST exceptions
        ResponseEntity<ErrorResponse> invalidOrderResponse = exceptionHandler.handleTradingSystemException(
                new InvalidOrderException("test"), request);
        assertEquals(HttpStatus.BAD_REQUEST, invalidOrderResponse.getStatusCode());

        ResponseEntity<ErrorResponse> configResponse = exceptionHandler.handleTradingSystemException(
                new ConfigurationException("key", "test"), request);
        assertEquals(HttpStatus.BAD_REQUEST, configResponse.getStatusCode());

        // Test CONFLICT exceptions
        ResponseEntity<ErrorResponse> fundsResponse = exceptionHandler.handleTradingSystemException(
                new InsufficientFundsException("test"), request);
        assertEquals(HttpStatus.CONFLICT, fundsResponse.getStatusCode());

        // Test SERVICE_UNAVAILABLE exceptions
        ResponseEntity<ErrorResponse> apiResponse = exceptionHandler.handleTradingSystemException(
                new ApiConnectionException("test"), request);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, apiResponse.getStatusCode());

        ResponseEntity<ErrorResponse> marketDataResponse = exceptionHandler.handleTradingSystemException(
                new MarketDataException("AAPL", "test"), request);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, marketDataResponse.getStatusCode());

        // Test INTERNAL_SERVER_ERROR exceptions
        ResponseEntity<ErrorResponse> portfolioResponse = exceptionHandler.handleTradingSystemException(
                new PortfolioException("test"), request);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, portfolioResponse.getStatusCode());

        ResponseEntity<ErrorResponse> strategyResponse = exceptionHandler.handleTradingSystemException(
                new StrategyExecutionException("strategy", "test"), request);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, strategyResponse.getStatusCode());
    }
}