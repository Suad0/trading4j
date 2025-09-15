package com.quanttrading.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quanttrading.dto.HistoricalData;
import com.quanttrading.exception.TradingSystemException;
import com.quanttrading.model.MarketData;
import com.quanttrading.service.MarketDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MarketDataController.class)
class MarketDataControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private MarketDataService marketDataService;
    
    private MarketData testMarketData;
    private HistoricalData testHistoricalData1;
    private HistoricalData testHistoricalData2;
    
    @BeforeEach
    void setUp() {
        // Create test market data
        testMarketData = new MarketData(
            "AAPL",
            new BigDecimal("155.00"),
            new BigDecimal("1000000"),
            new BigDecimal("157.00"),
            new BigDecimal("153.00"),
            new BigDecimal("154.00"),
            LocalDateTime.now()
        );
        
        // Create test historical data
        testHistoricalData1 = new HistoricalData(
            "AAPL",
            LocalDate.now().minusDays(1),
            new BigDecimal("154.00"),
            new BigDecimal("156.00"),
            new BigDecimal("153.00"),
            new BigDecimal("155.00"),
            1000000L
        );
        
        testHistoricalData2 = new HistoricalData(
            "AAPL",
            LocalDate.now(),
            new BigDecimal("155.00"),
            new BigDecimal("157.00"),
            new BigDecimal("154.00"),
            new BigDecimal("156.50"),
            1200000L
        );
    }
    
    @Test
    void getCurrentMarketData_Success() throws Exception {
        // Given
        when(marketDataService.isValidSymbol("AAPL")).thenReturn(true);
        when(marketDataService.getCurrentData("AAPL")).thenReturn(Optional.of(testMarketData));
        when(marketDataService.isMarketOpen()).thenReturn(true);
        
        // When & Then
        mockMvc.perform(get("/api/market/AAPL")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.price").value(155.00))
                .andExpect(jsonPath("$.volume").value(1000000))
                .andExpect(jsonPath("$.high").value(157.00))
                .andExpect(jsonPath("$.low").value(153.00))
                .andExpect(jsonPath("$.open").value(154.00))
                .andExpect(jsonPath("$.change").value(1.00))
                .andExpect(jsonPath("$.marketOpen").value(true))
                .andExpect(header().string("Cache-Control", "max-age=60"));
        
        verify(marketDataService).isValidSymbol("AAPL");
        verify(marketDataService).getCurrentData("AAPL");
        verify(marketDataService).isMarketOpen();
    }
    
    @Test
    void getCurrentMarketData_InvalidSymbol() throws Exception {
        // Given
        when(marketDataService.isValidSymbol("INVALID")).thenReturn(false);
        
        // When & Then
        mockMvc.perform(get("/api/market/INVALID")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        
        verify(marketDataService).isValidSymbol("INVALID");
        verify(marketDataService, never()).getCurrentData(anyString());
    }
    
    @Test
    void getCurrentMarketData_NotFound() throws Exception {
        // Given
        when(marketDataService.isValidSymbol("AAPL")).thenReturn(true);
        when(marketDataService.getCurrentData("AAPL")).thenReturn(Optional.empty());
        
        // When & Then
        mockMvc.perform(get("/api/market/AAPL")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
        
        verify(marketDataService).isValidSymbol("AAPL");
        verify(marketDataService).getCurrentData("AAPL");
    }
    
    @Test
    void getCurrentMarketData_ServiceException() throws Exception {
        // Given
        when(marketDataService.isValidSymbol("AAPL")).thenReturn(true);
        when(marketDataService.getCurrentData("AAPL"))
            .thenThrow(new TradingSystemException("API error"));
        
        // When & Then
        mockMvc.perform(get("/api/market/AAPL")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
        
        verify(marketDataService).isValidSymbol("AAPL");
        verify(marketDataService).getCurrentData("AAPL");
    }
    
    @Test
    void getCurrentMarketDataBatch_Success() throws Exception {
        // Given
        MarketData testMarketData2 = new MarketData(
            "GOOGL",
            new BigDecimal("2100.00"),
            new BigDecimal("500000"),
            new BigDecimal("2110.00"),
            new BigDecimal("2090.00"),
            new BigDecimal("2095.00"),
            LocalDateTime.now()
        );
        
        when(marketDataService.isValidSymbol("AAPL")).thenReturn(true);
        when(marketDataService.isValidSymbol("GOOGL")).thenReturn(true);
        when(marketDataService.getCurrentData(Arrays.asList("AAPL", "GOOGL")))
            .thenReturn(Arrays.asList(testMarketData, testMarketData2));
        when(marketDataService.isMarketOpen()).thenReturn(true);
        
        // When & Then
        mockMvc.perform(get("/api/market/batch?symbols=AAPL,GOOGL")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$[0].price").value(155.00))
                .andExpect(jsonPath("$[1].symbol").value("GOOGL"))
                .andExpect(jsonPath("$[1].price").value(2100.00))
                .andExpect(header().string("Cache-Control", "max-age=60"));
        
        verify(marketDataService).isValidSymbol("AAPL");
        verify(marketDataService).isValidSymbol("GOOGL");
        verify(marketDataService).getCurrentData(Arrays.asList("AAPL", "GOOGL"));
    }
    
    @Test
    void getCurrentMarketDataBatch_InvalidSymbol() throws Exception {
        // Given
        when(marketDataService.isValidSymbol("AAPL")).thenReturn(true);
        when(marketDataService.isValidSymbol("INVALID")).thenReturn(false);
        
        // When & Then
        mockMvc.perform(get("/api/market/batch?symbols=AAPL,INVALID")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        
        verify(marketDataService).isValidSymbol("AAPL");
        verify(marketDataService).isValidSymbol("INVALID");
        verify(marketDataService, never()).getCurrentData(anyList());
    }
    
    @Test
    void getHistoricalData_Success() throws Exception {
        // Given
        LocalDate from = LocalDate.now().minusDays(2);
        LocalDate to = LocalDate.now();
        List<HistoricalData> historicalData = Arrays.asList(testHistoricalData1, testHistoricalData2);
        
        when(marketDataService.isValidSymbol("AAPL")).thenReturn(true);
        when(marketDataService.getHistoricalData("AAPL", from, to)).thenReturn(historicalData);
        
        // When & Then
        mockMvc.perform(get("/api/market/AAPL/history")
                .param("from", from.toString())
                .param("to", to.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$[0].close").value(155.00))
                .andExpect(jsonPath("$[1].symbol").value("AAPL"))
                .andExpect(jsonPath("$[1].close").value(156.50))
                .andExpect(header().string("Cache-Control", "max-age=300"));
        
        verify(marketDataService).isValidSymbol("AAPL");
        verify(marketDataService).getHistoricalData("AAPL", from, to);
    }
    
    @Test
    void getHistoricalData_InvalidDateRange() throws Exception {
        // Given
        LocalDate from = LocalDate.now();
        LocalDate to = LocalDate.now().minusDays(1); // Invalid: from is after to
        
        when(marketDataService.isValidSymbol("AAPL")).thenReturn(true);
        
        // When & Then
        mockMvc.perform(get("/api/market/AAPL/history")
                .param("from", from.toString())
                .param("to", to.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        
        verify(marketDataService).isValidSymbol("AAPL");
        verify(marketDataService, never()).getHistoricalData(anyString(), any(LocalDate.class), any(LocalDate.class));
    }
    
    @Test
    void getHistoricalDataByPeriod_Success() throws Exception {
        // Given
        List<HistoricalData> historicalData = Arrays.asList(testHistoricalData1, testHistoricalData2);
        
        when(marketDataService.isValidSymbol("AAPL")).thenReturn(true);
        when(marketDataService.getHistoricalData("AAPL", "1mo")).thenReturn(historicalData);
        
        // When & Then
        mockMvc.perform(get("/api/market/AAPL/history/1mo")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                .andExpect(header().string("Cache-Control", "max-age=300"));
        
        verify(marketDataService).isValidSymbol("AAPL");
        verify(marketDataService).getHistoricalData("AAPL", "1mo");
    }
    
    @Test
    void getHistoricalDataByPeriod_ServiceException() throws Exception {
        // Given
        when(marketDataService.isValidSymbol("AAPL")).thenReturn(true);
        when(marketDataService.getHistoricalData("AAPL", "invalid"))
            .thenThrow(new TradingSystemException("Invalid period"));
        
        // When & Then
        mockMvc.perform(get("/api/market/AAPL/history/invalid")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
        
        verify(marketDataService).isValidSymbol("AAPL");
        verify(marketDataService).getHistoricalData("AAPL", "invalid");
    }
    
    @Test
    void getMarketStatus_Success() throws Exception {
        // Given
        List<String> supportedSymbols = Arrays.asList("AAPL", "GOOGL", "MSFT");
        
        when(marketDataService.isMarketOpen()).thenReturn(true);
        when(marketDataService.getSupportedSymbols()).thenReturn(supportedSymbols);
        
        // When & Then
        mockMvc.perform(get("/api/market/status")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.open").value(true))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.supportedSymbols.length()").value(3))
                .andExpect(jsonPath("$.supportedSymbols[0]").value("AAPL"))
                .andExpect(jsonPath("$.timezone").value("America/New_York"))
                .andExpect(header().string("Cache-Control", "max-age=60"));
        
        verify(marketDataService).isMarketOpen();
        verify(marketDataService).getSupportedSymbols();
    }
    
    @Test
    void refreshMarketData_Success() throws Exception {
        // Given
        when(marketDataService.isValidSymbol("AAPL")).thenReturn(true);
        when(marketDataService.refreshCache("AAPL")).thenReturn(Optional.of(testMarketData));
        when(marketDataService.isMarketOpen()).thenReturn(false);
        
        // When & Then
        mockMvc.perform(post("/api/market/AAPL/refresh")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.price").value(155.00))
                .andExpect(jsonPath("$.marketOpen").value(false));
        
        verify(marketDataService).isValidSymbol("AAPL");
        verify(marketDataService).refreshCache("AAPL");
        verify(marketDataService).isMarketOpen();
    }
    
    @Test
    void refreshMarketData_NotFound() throws Exception {
        // Given
        when(marketDataService.isValidSymbol("AAPL")).thenReturn(true);
        when(marketDataService.refreshCache("AAPL")).thenReturn(Optional.empty());
        
        // When & Then
        mockMvc.perform(post("/api/market/AAPL/refresh")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
        
        verify(marketDataService).isValidSymbol("AAPL");
        verify(marketDataService).refreshCache("AAPL");
    }
    
    @Test
    void clearCache_AllCache_Success() throws Exception {
        // Given
        doNothing().when(marketDataService).clearCache();
        
        // When & Then
        mockMvc.perform(delete("/api/market/cache")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        
        verify(marketDataService).clearCache();
    }
    
    @Test
    void clearCache_SpecificSymbol_Success() throws Exception {
        // Given
        when(marketDataService.isValidSymbol("AAPL")).thenReturn(true);
        doNothing().when(marketDataService).clearCache("AAPL");
        
        // When & Then
        mockMvc.perform(delete("/api/market/cache?symbol=AAPL")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        
        verify(marketDataService).isValidSymbol("AAPL");
        verify(marketDataService).clearCache("AAPL");
    }
    
    @Test
    void clearCache_InvalidSymbol() throws Exception {
        // Given
        when(marketDataService.isValidSymbol("INVALID")).thenReturn(false);
        
        // When & Then
        mockMvc.perform(delete("/api/market/cache?symbol=INVALID")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        
        verify(marketDataService).isValidSymbol("INVALID");
        verify(marketDataService, never()).clearCache(anyString());
    }
    
    @Test
    void checkDataStale_Fresh() throws Exception {
        // Given
        when(marketDataService.isValidSymbol("AAPL")).thenReturn(true);
        when(marketDataService.isDataStale("AAPL", 5)).thenReturn(false);
        
        // When & Then
        mockMvc.perform(get("/api/market/AAPL/stale")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Data for AAPL is fresh"));
        
        verify(marketDataService).isValidSymbol("AAPL");
        verify(marketDataService).isDataStale("AAPL", 5);
    }
    
    @Test
    void checkDataStale_Stale() throws Exception {
        // Given
        when(marketDataService.isValidSymbol("AAPL")).thenReturn(true);
        when(marketDataService.isDataStale("AAPL", 10)).thenReturn(true);
        
        // When & Then
        mockMvc.perform(get("/api/market/AAPL/stale?maxAge=10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Data for AAPL is stale"));
        
        verify(marketDataService).isValidSymbol("AAPL");
        verify(marketDataService).isDataStale("AAPL", 10);
    }
    
    @Test
    void checkDataStale_InvalidMaxAge() throws Exception {
        // Given
        when(marketDataService.isValidSymbol("AAPL")).thenReturn(true);
        
        // When & Then
        mockMvc.perform(get("/api/market/AAPL/stale?maxAge=0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Max age must be between 1 and 1440 minutes"));
        
        verify(marketDataService).isValidSymbol("AAPL");
        verify(marketDataService, never()).isDataStale(anyString(), anyInt());
    }
    
    @Test
    void getCurrentMarketData_LowerCaseSymbol() throws Exception {
        // Given
        when(marketDataService.isValidSymbol("aapl")).thenReturn(false); // lowercase is invalid
        
        // When & Then
        mockMvc.perform(get("/api/market/aapl")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        
        verify(marketDataService).isValidSymbol("aapl");
        verify(marketDataService, never()).getCurrentData(anyString());
    }
}