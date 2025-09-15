package com.quanttrading.integration.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Utility class providing common operations and helpers for integration tests.
 * Contains reusable methods for API testing, data validation, and test setup.
 */
public class IntegrationTestUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Create HTTP headers with JSON content type
     */
    public static HttpHeaders createJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * Create HTTP entity with JSON headers and body
     */
    public static <T> HttpEntity<T> createJsonEntity(T body) {
        return new HttpEntity<>(body, createJsonHeaders());
    }

    /**
     * Convert object to JSON string
     */
    public static String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert object to JSON", e);
        }
    }

    /**
     * Convert JSON string to object
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert JSON to object", e);
        }
    }

    /**
     * Perform GET request and expect success
     */
    public static ResultActions performGetAndExpectOk(MockMvc mockMvc, String url) throws Exception {
        return mockMvc.perform(get(url)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /**
     * Perform POST request with JSON body and expect success
     */
    public static ResultActions performPostAndExpectOk(MockMvc mockMvc, String url, Object body) throws Exception {
        return mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(body)))
                .andExpect(status().isOk());
    }

    /**
     * Perform PUT request with JSON body and expect success
     */
    public static ResultActions performPutAndExpectOk(MockMvc mockMvc, String url, Object body) throws Exception {
        return mockMvc.perform(put(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(body)))
                .andExpect(status().isOk());
    }

    /**
     * Perform DELETE request and expect success
     */
    public static ResultActions performDeleteAndExpectOk(MockMvc mockMvc, String url) throws Exception {
        return mockMvc.perform(delete(url)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /**
     * Extract response body as string from MvcResult
     */
    public static String getResponseBody(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString();
    }

    /**
     * Extract response body as object from MvcResult
     */
    public static <T> T getResponseBodyAsObject(MvcResult result, Class<T> clazz) throws Exception {
        String json = getResponseBody(result);
        return fromJson(json, clazz);
    }

    /**
     * Compare BigDecimal values with tolerance
     */
    public static boolean isEqualWithTolerance(BigDecimal actual, BigDecimal expected, BigDecimal tolerance) {
        if (actual == null || expected == null) {
            return actual == expected;
        }
        return actual.subtract(expected).abs().compareTo(tolerance) <= 0;
    }

    /**
     * Compare BigDecimal values with default tolerance (0.01)
     */
    public static boolean isEqualWithTolerance(BigDecimal actual, BigDecimal expected) {
        return isEqualWithTolerance(actual, expected, new BigDecimal("0.01"));
    }

    /**
     * Round BigDecimal to 2 decimal places
     */
    public static BigDecimal roundToTwoDecimals(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Generate random BigDecimal within range
     */
    public static BigDecimal randomBigDecimal(BigDecimal min, BigDecimal max) {
        BigDecimal range = max.subtract(min);
        BigDecimal randomFactor = new BigDecimal(Math.random());
        return min.add(range.multiply(randomFactor));
    }

    /**
     * Generate random positive BigDecimal
     */
    public static BigDecimal randomPositiveBigDecimal(BigDecimal max) {
        return randomBigDecimal(BigDecimal.ZERO, max);
    }

    /**
     * Wait for condition to be true with timeout
     */
    public static boolean waitForCondition(java.util.function.BooleanSupplier condition, 
                                         long timeoutSeconds) throws InterruptedException {
        long endTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSeconds);
        
        while (System.currentTimeMillis() < endTime) {
            if (condition.getAsBoolean()) {
                return true;
            }
            Thread.sleep(100); // Check every 100ms
        }
        
        return false;
    }

    /**
     * Format LocalDateTime for logging
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    /**
     * Create test timestamp
     */
    public static LocalDateTime createTestTimestamp() {
        return LocalDateTime.now().withNano(0); // Remove nanoseconds for easier comparison
    }

    /**
     * Validate that a string is a valid UUID
     */
    public static boolean isValidUUID(String uuid) {
        try {
            java.util.UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Generate test account ID
     */
    public static String generateTestAccountId() {
        return "test-account-" + System.currentTimeMillis();
    }

    /**
     * Generate test order ID
     */
    public static String generateTestOrderId() {
        return "test-order-" + System.currentTimeMillis();
    }

    /**
     * Validate percentage value (0-100)
     */
    public static boolean isValidPercentage(BigDecimal percentage) {
        return percentage != null && 
               percentage.compareTo(BigDecimal.ZERO) >= 0 && 
               percentage.compareTo(new BigDecimal("100")) <= 0;
    }

    /**
     * Validate price value (positive)
     */
    public static boolean isValidPrice(BigDecimal price) {
        return price != null && price.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Validate quantity value (non-negative)
     */
    public static boolean isValidQuantity(BigDecimal quantity) {
        return quantity != null && quantity.compareTo(BigDecimal.ZERO) >= 0;
    }

    /**
     * Calculate percentage change
     */
    public static BigDecimal calculatePercentageChange(BigDecimal oldValue, BigDecimal newValue) {
        if (oldValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return newValue.subtract(oldValue)
                .divide(oldValue, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    /**
     * Sleep for specified milliseconds (for testing timing-dependent operations)
     */
    public static void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Sleep interrupted", e);
        }
    }

    /**
     * Create test symbol (ensures consistent test data)
     */
    public static String createTestSymbol(String base) {
        return base.toUpperCase();
    }

    /**
     * Validate symbol format
     */
    public static boolean isValidSymbol(String symbol) {
        return symbol != null && 
               symbol.matches("^[A-Z]{1,5}$") && 
               symbol.length() >= 1 && 
               symbol.length() <= 5;
    }

    /**
     * Create test description with timestamp
     */
    public static String createTestDescription(String base) {
        return base + " - " + formatDateTime(LocalDateTime.now());
    }

    /**
     * Retry operation with exponential backoff
     */
    public static <T> T retryWithBackoff(java.util.function.Supplier<T> operation, 
                                       int maxRetries, long initialDelayMs) throws InterruptedException {
        Exception lastException = null;
        long delay = initialDelayMs;
        
        for (int i = 0; i < maxRetries; i++) {
            try {
                return operation.get();
            } catch (Exception e) {
                lastException = e;
                if (i < maxRetries - 1) {
                    Thread.sleep(delay);
                    delay *= 2; // Exponential backoff
                }
            }
        }
        
        throw new RuntimeException("Operation failed after " + maxRetries + " retries", lastException);
    }

    /**
     * Assert that two BigDecimal values are approximately equal
     */
    public static void assertApproximatelyEqual(BigDecimal actual, BigDecimal expected, BigDecimal tolerance) {
        if (!isEqualWithTolerance(actual, expected, tolerance)) {
            throw new AssertionError(String.format(
                "Values are not approximately equal. Expected: %s, Actual: %s, Tolerance: %s",
                expected, actual, tolerance));
        }
    }

    /**
     * Assert that two BigDecimal values are approximately equal with default tolerance
     */
    public static void assertApproximatelyEqual(BigDecimal actual, BigDecimal expected) {
        assertApproximatelyEqual(actual, expected, new BigDecimal("0.01"));
    }
}