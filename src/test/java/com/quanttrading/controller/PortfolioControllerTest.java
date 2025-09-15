package com.quanttrading.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quanttrading.dto.PerformanceMetrics;
import com.quanttrading.dto.PortfolioSummaryResponse;
import com.quanttrading.dto.PositionResponse;
import com.quanttrading.exception.TradingSystemException;
import com.quanttrading.model.Portfolio;
import com.quanttrading.model.Position;
import com.quanttrading.service.PortfolioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PortfolioController.class)
class PortfolioControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private PortfolioService portfolioService;
    
    private Portfolio testPortfolio;
    private Position testPosition1;
    private Position testPosition2;
    private PerformanceMetrics testMetrics;
    
    @BeforeEach
    void setUp() {
        // Create test portfolio
        testPortfolio = new Portfolio("test-account", new BigDecimal("10000.00"), new BigDecimal("15000.00"));
        testPortfolio.setLastUpdated(LocalDateTime.now());
        
        // Create test positions
        testPosition1 = new Position("AAPL", new BigDecimal("10"), new BigDecimal("150.00"));
        testPosition1.setCurrentPrice(new BigDecimal("155.00"));
        testPosition1.setLastUpdated(LocalDateTime.now());
        
        testPosition2 = new Position("GOOGL", new BigDecimal("5"), new BigDecimal("2000.00"));
        testPosition2.setCurrentPrice(new BigDecimal("2100.00"));
        testPosition2.setLastUpdated(LocalDateTime.now());
        
        // Create test performance metrics
        testMetrics = new PerformanceMetrics(
            new BigDecimal("15000.00"),
            new BigDecimal("10000.00"),
            new BigDecimal("5000.00"),
            new BigDecimal("550.00")
        );
        testMetrics.setPositionCount(2);
        testMetrics.setDailyPnL(new BigDecimal("100.00"));
        testMetrics.setDailyReturn(new BigDecimal("0.67"));
        testMetrics.setTotalReturn(new BigDecimal("50.00"));
    }
    
    @Test
    void getPortfolioSummary_Success() throws Exception {
        // Given
        when(portfolioService.getCurrentPortfolio()).thenReturn(testPortfolio);
        when(portfolioService.calculatePerformance()).thenReturn(testMetrics);
        
        // When & Then
        mockMvc.perform(get("/api/portfolio")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("test-account"))
                .andExpect(jsonPath("$.totalValue").value(15000.00))
                .andExpect(jsonPath("$.cashBalance").value(10000.00))
                .andExpect(jsonPath("$.positionsValue").value(5000.00))
                .andExpect(jsonPath("$.totalUnrealizedPnL").value(550.00))
                .andExpect(jsonPath("$.positionCount").value(2));
        
        verify(portfolioService).getCurrentPortfolio();
        verify(portfolioService).calculatePerformance();
    }
    
    @Test
    void getPortfolioSummary_ServiceException() throws Exception {
        // Given
        when(portfolioService.getCurrentPortfolio()).thenThrow(new TradingSystemException("Portfolio not found"));
        
        // When & Then
        mockMvc.perform(get("/api/portfolio")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
        
        verify(portfolioService).getCurrentPortfolio();
    }
    
    @Test
    void getPositions_Success() throws Exception {
        // Given
        List<Position> positions = Arrays.asList(testPosition1, testPosition2);
        when(portfolioService.getPositions()).thenReturn(positions);
        
        // When & Then
        mockMvc.perform(get("/api/portfolio/positions")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$[0].quantity").value(10))
                .andExpect(jsonPath("$[0].averagePrice").value(150.00))
                .andExpect(jsonPath("$[0].currentPrice").value(155.00))
                .andExpect(jsonPath("$[1].symbol").value("GOOGL"))
                .andExpect(jsonPath("$[1].quantity").value(5))
                .andExpect(jsonPath("$[1].averagePrice").value(2000.00))
                .andExpect(jsonPath("$[1].currentPrice").value(2100.00));
        
        verify(portfolioService).getPositions();
    }
    
    @Test
    void getActivePositions_Success() throws Exception {
        // Given
        List<Position> activePositions = Arrays.asList(testPosition1, testPosition2);
        when(portfolioService.getActivePositions()).thenReturn(activePositions);
        
        // When & Then
        mockMvc.perform(get("/api/portfolio/positions/active")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        
        verify(portfolioService).getActivePositions();
    }
    
    @Test
    void getPosition_Found() throws Exception {
        // Given
        when(portfolioService.getPosition("AAPL")).thenReturn(Optional.of(testPosition1));
        
        // When & Then
        mockMvc.perform(get("/api/portfolio/positions/AAPL")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.quantity").value(10))
                .andExpect(jsonPath("$.averagePrice").value(150.00))
                .andExpect(jsonPath("$.currentPrice").value(155.00))
                .andExpect(jsonPath("$.marketValue").value(1550.00))
                .andExpect(jsonPath("$.unrealizedPnL").value(50.00))
                .andExpect(jsonPath("$.costBasis").value(1500.00));
        
        verify(portfolioService).getPosition("AAPL");
    }
    
    @Test
    void getPosition_NotFound() throws Exception {
        // Given
        when(portfolioService.getPosition("TSLA")).thenReturn(Optional.empty());
        
        // When & Then
        mockMvc.perform(get("/api/portfolio/positions/TSLA")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
        
        verify(portfolioService).getPosition("TSLA");
    }
    
    @Test
    void getPosition_LowerCaseSymbol() throws Exception {
        // Given
        when(portfolioService.getPosition("AAPL")).thenReturn(Optional.of(testPosition1));
        
        // When & Then
        mockMvc.perform(get("/api/portfolio/positions/aapl")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("AAPL"));
        
        verify(portfolioService).getPosition("AAPL");
    }
    
    @Test
    void getPerformanceMetrics_Success() throws Exception {
        // Given
        when(portfolioService.calculatePerformance()).thenReturn(testMetrics);
        
        // When & Then
        mockMvc.perform(get("/api/portfolio/performance")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalValue").value(15000.00))
                .andExpect(jsonPath("$.cashBalance").value(10000.00))
                .andExpect(jsonPath("$.positionsValue").value(5000.00))
                .andExpect(jsonPath("$.totalUnrealizedPnL").value(550.00))
                .andExpect(jsonPath("$.positionCount").value(2))
                .andExpect(jsonPath("$.dailyPnL").value(100.00))
                .andExpect(jsonPath("$.dailyReturn").value(0.67))
                .andExpect(jsonPath("$.totalReturn").value(50.00));
        
        verify(portfolioService).calculatePerformance();
    }
    
    @Test
    void synchronizePortfolio_Success() throws Exception {
        // Given
        when(portfolioService.getCurrentPortfolio()).thenReturn(testPortfolio);
        when(portfolioService.calculatePerformance()).thenReturn(testMetrics);
        doNothing().when(portfolioService).synchronizePortfolio(anyString());
        
        // When & Then
        mockMvc.perform(post("/api/portfolio/sync")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("test-account"))
                .andExpect(jsonPath("$.totalValue").value(15000.00));
        
        verify(portfolioService, times(2)).getCurrentPortfolio();
        verify(portfolioService).synchronizePortfolio("test-account");
        verify(portfolioService).calculatePerformance();
    }
    
    @Test
    void synchronizePortfolio_ServiceException() throws Exception {
        // Given
        when(portfolioService.getCurrentPortfolio()).thenReturn(testPortfolio);
        doThrow(new TradingSystemException("Sync failed")).when(portfolioService).synchronizePortfolio(anyString());
        
        // When & Then
        mockMvc.perform(post("/api/portfolio/sync")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
        
        verify(portfolioService).getCurrentPortfolio();
        verify(portfolioService).synchronizePortfolio("test-account");
    }
    
    @Test
    void getPositions_ServiceException() throws Exception {
        // Given
        when(portfolioService.getPositions()).thenThrow(new TradingSystemException("Database error"));
        
        // When & Then
        mockMvc.perform(get("/api/portfolio/positions")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
        
        verify(portfolioService).getPositions();
    }
    
    @Test
    void getPerformanceMetrics_ServiceException() throws Exception {
        // Given
        when(portfolioService.calculatePerformance()).thenThrow(new TradingSystemException("Calculation error"));
        
        // When & Then
        mockMvc.perform(get("/api/portfolio/performance")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
        
        verify(portfolioService).calculatePerformance();
    }
}