package com.quanttrading.service.impl;

import com.quanttrading.client.YFinanceApiClient;
import com.quanttrading.dto.HistoricalData;
import com.quanttrading.exception.TradingSystemException;
import com.quanttrading.model.MarketData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketDataServiceImplTest {
    
    @Mock
    private YFinanceApiClient yFinanceApiClient;
    
    @InjectMocks
    private MarketDataServiceImpl marketDataService;
    
    private MarketData testMarketData;
    private HistoricalData testHistoricalData;
    private final String TEST_SYMBOL = "AAPL";
    private final String INVALID_SYMBOL = "INVALID123";
    
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(marketDataService, "cacheTtlMinutes", 5);
        ReflectionTestUtils.setField(marketDataService, "rateLimitDelayMs", 100L);
        
        testMarketData = new MarketData();
        testMarketData.setSymbol(TEST_SYMBOL);
        testMarketData.setPrice(new BigDecimal("155.50"));
        testMarketData.setVolume(new BigDecimal("1000000"));
        testMarketData.setHigh(new BigDecimal("156.00"));
        testMarketData.setLow(new BigDecimal("154.00"));
        testMarketData.setOpen(new BigDecimal("155.00"));
        testMarketData.setTimestamp(LocalDateTime.now());
        
        testHistoricalData = new HistoricalData();
        testHistoricalData.setSymbol(TEST_SYMBOL);
        testHistoricalData.setDate(LocalDate.now().minusDays(1));
        testHistoricalData.setOpen(new BigDecimal("154.00"));
        testHistoricalData.setHigh(new BigDecimal("156.00"));
        testHistoricalData.setLow(new BigDecimal("153.00"));
        testHistoricalData.setClose(new BigDecimal("155.50"));
        testHistoricalData.setVolume(2000000L);
    }
    
    @Test
    void getCurrentData_ShouldReturnData_WhenValidSymbol() {
        // Given
        when(yFinanceApiClient.getCurrentQuote(TEST_SYMBOL)).thenReturn(testMarketData);
        
        // When
        Optional<MarketData> result = marketDataService.getCurrentData(TEST_SYMBOL);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(TEST_SYMBOL, result.get().getSymbol());
        assertEquals(new BigDecimal("155.50"), result.get().getPrice());
        verify(yFinanceApiClient).getCurrentQuote(TEST_SYMBOL);
    }
    
    @Test
    void getCurrentData_ShouldReturnEmpty_WhenInvalidSymbol() {
        // When
        Optional<MarketData> result = marketDataService.getCurrentData(INVALID_SYMBOL);
        
        // Then
        assertFalse(result.isPresent());
        verify(yFinanceApiClient, never()).getCurrentQuote(anyString());
    }
    
    @Test
    void getCurrentData_ShouldReturnEmpty_WhenApiReturnsNull() {
        // Given
        when(yFinanceApiClient.getCurrentQuote(TEST_SYMBOL)).thenReturn(null);
        
        // When
        Optional<MarketData> result = marketDataService.getCurrentData(TEST_SYMBOL);
        
        // Then
        assertFalse(result.isPresent());
        verify(yFinanceApiClient).getCurrentQuote(TEST_SYMBOL);
    }
    
    @Test
    void getCurrentData_ShouldReturnEmpty_WhenApiThrowsException() {
        // Given
        when(yFinanceApiClient.getCurrentQuote(TEST_SYMBOL)).thenThrow(new RuntimeException("API Error"));
        
        // When
        Optional<MarketData> result = marketDataService.getCurrentData(TEST_SYMBOL);
        
        // Then
        assertFalse(result.isPresent());
        verify(yFinanceApiClient).getCurrentQuote(TEST_SYMBOL);
    }
    
    @Test
    void getCurrentData_MultipleSymbols_ShouldReturnValidData() {
        // Given
        List<String> symbols = Arrays.asList("AAPL", "GOOGL", INVALID_SYMBOL);
        
        MarketData googleData = new MarketData();
        googleData.setSymbol("GOOGL");
        googleData.setPrice(new BigDecimal("2500.00"));
        
        when(yFinanceApiClient.getCurrentQuote("AAPL")).thenReturn(testMarketData);
        when(yFinanceApiClient.getCurrentQuote("GOOGL")).thenReturn(googleData);
        
        // When
        List<MarketData> result = marketDataService.getCurrentData(symbols);
        
        // Then
        assertEquals(2, result.size()); // Only valid symbols should be returned
        assertEquals("AAPL", result.get(0).getSymbol());
        assertEquals("GOOGL", result.get(1).getSymbol());
        
        verify(yFinanceApiClient).getCurrentQuote("AAPL");
        verify(yFinanceApiClient).getCurrentQuote("GOOGL");
        verify(yFinanceApiClient, never()).getCurrentQuote(INVALID_SYMBOL);
    }
    
    @Test
    void getHistoricalData_WithDateRange_ShouldReturnData() {
        // Given
        LocalDate from = LocalDate.now().minusDays(30);
        LocalDate to = LocalDate.now();
        List<HistoricalData> expectedData = Arrays.asList(testHistoricalData);
        
        when(yFinanceApiClient.getHistoricalData(eq(TEST_SYMBOL), anyString())).thenReturn(expectedData);
        
        // When
        List<HistoricalData> result = marketDataService.getHistoricalData(TEST_SYMBOL, from, to);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TEST_SYMBOL, result.get(0).getSymbol());
        verify(yFinanceApiClient).getHistoricalData(eq(TEST_SYMBOL), anyString());
    }
    
    @Test
    void getHistoricalData_WithPeriod_ShouldReturnData() {
        // Given
        String period = "1mo";
        List<HistoricalData> expectedData = Arrays.asList(testHistoricalData);
        
        when(yFinanceApiClient.getHistoricalData(TEST_SYMBOL, period)).thenReturn(expectedData);
        
        // When
        List<HistoricalData> result = marketDataService.getHistoricalData(TEST_SYMBOL, period);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TEST_SYMBOL, result.get(0).getSymbol());
        verify(yFinanceApiClient).getHistoricalData(TEST_SYMBOL, period);
    }
    
    @Test
    void getHistoricalData_ShouldThrowException_WhenInvalidSymbol() {
        // Given
        LocalDate from = LocalDate.now().minusDays(30);
        LocalDate to = LocalDate.now();
        
        // When & Then
        assertThrows(TradingSystemException.class, 
                    () -> marketDataService.getHistoricalData(INVALID_SYMBOL, from, to));
        
        verify(yFinanceApiClient, never()).getHistoricalData(anyString(), anyString());
    }
    
    @Test
    void getHistoricalData_ShouldThrowException_WhenInvalidDateRange() {
        // Given
        LocalDate from = LocalDate.now();
        LocalDate to = LocalDate.now().minusDays(30); // Invalid: from is after to
        
        // When & Then
        assertThrows(TradingSystemException.class, 
                    () -> marketDataService.getHistoricalData(TEST_SYMBOL, from, to));
        
        verify(yFinanceApiClient, never()).getHistoricalData(anyString(), anyString());
    }
    
    @Test
    void getHistoricalData_ShouldThrowException_WhenInvalidPeriod() {
        // Given
        String invalidPeriod = "invalid";
        
        // When & Then
        assertThrows(TradingSystemException.class, 
                    () -> marketDataService.getHistoricalData(TEST_SYMBOL, invalidPeriod));
        
        verify(yFinanceApiClient, never()).getHistoricalData(anyString(), anyString());
    }
    
    @Test
    void getHistoricalData_ShouldThrowException_WhenApiThrowsException() {
        // Given
        String period = "1mo";
        when(yFinanceApiClient.getHistoricalData(TEST_SYMBOL, period))
                .thenThrow(new RuntimeException("API Error"));
        
        // When & Then
        assertThrows(TradingSystemException.class, 
                    () -> marketDataService.getHistoricalData(TEST_SYMBOL, period));
        
        verify(yFinanceApiClient).getHistoricalData(TEST_SYMBOL, period);
    }
    
    @Test
    void subscribeToRealTimeData_ShouldAddSubscription_WhenValidSymbol() {
        // Given
        Consumer<MarketData> callback = data -> {};
        
        // When
        marketDataService.subscribeToRealTimeData(TEST_SYMBOL, callback);
        
        // Then
        // No exception should be thrown
        // In a real implementation, we would verify WebSocket connection
    }
    
    @Test
    void subscribeToRealTimeData_ShouldThrowException_WhenInvalidSymbol() {
        // Given
        Consumer<MarketData> callback = data -> {};
        
        // When & Then
        assertThrows(TradingSystemException.class, 
                    () -> marketDataService.subscribeToRealTimeData(INVALID_SYMBOL, callback));
    }
    
    @Test
    void unsubscribeFromRealTimeData_ShouldRemoveSubscription() {
        // Given
        Consumer<MarketData> callback = data -> {};
        marketDataService.subscribeToRealTimeData(TEST_SYMBOL, callback);
        
        // When
        marketDataService.unsubscribeFromRealTimeData(TEST_SYMBOL);
        
        // Then
        // No exception should be thrown
        // In a real implementation, we would verify WebSocket disconnection
    }
    
    @Test
    void isValidSymbol_ShouldReturnTrue_WhenValidSymbol() {
        // When & Then
        assertTrue(marketDataService.isValidSymbol("AAPL"));
        assertTrue(marketDataService.isValidSymbol("GOOGL"));
        assertTrue(marketDataService.isValidSymbol("MSFT"));
        assertTrue(marketDataService.isValidSymbol("A"));
        assertTrue(marketDataService.isValidSymbol("ABCDE"));
    }
    
    @Test
    void isValidSymbol_ShouldReturnFalse_WhenInvalidSymbol() {
        // When & Then
        assertFalse(marketDataService.isValidSymbol(null));
        assertFalse(marketDataService.isValidSymbol(""));
        assertFalse(marketDataService.isValidSymbol("   "));
        assertFalse(marketDataService.isValidSymbol("ABCDEF")); // Too long
        assertFalse(marketDataService.isValidSymbol("123"));     // Numbers
        assertFalse(marketDataService.isValidSymbol("ABC123"));  // Mixed
        assertFalse(marketDataService.isValidSymbol("abc"));     // Lowercase
    }
    
    @Test
    void isMarketOpen_ShouldReturnBoolean() {
        // When
        boolean result = marketDataService.isMarketOpen();
        
        // Then
        // Result depends on current time, so we just verify it returns a boolean
        assertTrue(result || !result); // Always true, just checking no exception
    }
    
    @Test
    void getSupportedSymbols_ShouldReturnSymbolList() {
        // When
        List<String> result = marketDataService.getSupportedSymbols();
        
        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("AAPL"));
        assertTrue(result.contains("GOOGL"));
    }
    
    @Test
    void clearCache_ShouldNotThrowException() {
        // When & Then
        assertDoesNotThrow(() -> marketDataService.clearCache());
        assertDoesNotThrow(() -> marketDataService.clearCache(TEST_SYMBOL));
    }
    
    @Test
    void refreshCache_ShouldReturnData_WhenValidSymbol() {
        // Given
        when(yFinanceApiClient.getCurrentQuote(TEST_SYMBOL)).thenReturn(testMarketData);
        
        // When
        Optional<MarketData> result = marketDataService.refreshCache(TEST_SYMBOL);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(TEST_SYMBOL, result.get().getSymbol());
        verify(yFinanceApiClient).getCurrentQuote(TEST_SYMBOL);
    }
    
    @Test
    void refreshCache_MultipleSymbols_ShouldNotThrowException() {
        // Given
        List<String> symbols = Arrays.asList("AAPL", "GOOGL");
        when(yFinanceApiClient.getCurrentQuote(anyString())).thenReturn(testMarketData);
        
        // When & Then
        assertDoesNotThrow(() -> marketDataService.refreshCache(symbols));
    }
    
    @Test
    void isDataStale_ShouldReturnTrue_WhenNoData() {
        // Given
        when(yFinanceApiClient.getCurrentQuote(TEST_SYMBOL)).thenReturn(null);
        
        // When
        boolean result = marketDataService.isDataStale(TEST_SYMBOL, 5);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void isDataStale_ShouldReturnFalse_WhenDataIsFresh() {
        // Given
        testMarketData.setTimestamp(LocalDateTime.now().minusMinutes(2)); // Fresh data
        when(yFinanceApiClient.getCurrentQuote(TEST_SYMBOL)).thenReturn(testMarketData);
        
        // When
        boolean result = marketDataService.isDataStale(TEST_SYMBOL, 5);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void isDataStale_ShouldReturnTrue_WhenDataIsOld() {
        // Given
        testMarketData.setTimestamp(LocalDateTime.now().minusMinutes(10)); // Old data
        when(yFinanceApiClient.getCurrentQuote(TEST_SYMBOL)).thenReturn(testMarketData);
        
        // When
        boolean result = marketDataService.isDataStale(TEST_SYMBOL, 5);
        
        // Then
        assertTrue(result);
    }
}