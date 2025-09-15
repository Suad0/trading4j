package com.quanttrading.controller;

import com.quanttrading.dto.PerformanceMetrics;
import com.quanttrading.model.Portfolio;
import com.quanttrading.model.Position;
import com.quanttrading.service.PortfolioService;
import com.quanttrading.service.StrategyPerformance;
import com.quanttrading.service.StrategyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MonitoringController.class)
class MonitoringControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StrategyService strategyService;

    @MockBean
    private PortfolioService portfolioService;

    @Test
    void getStrategyPerformance_ShouldReturnAllStrategies() throws Exception {
        // Given
        StrategyPerformance strategy1 = new StrategyPerformance("SMA Strategy");
        StrategyPerformance strategy2 = new StrategyPerformance("RSI Strategy");
        
        Map<String, StrategyPerformance> strategies = new HashMap<>();
        strategies.put("SMA Strategy", strategy1);
        strategies.put("RSI Strategy", strategy2);
        
        when(strategyService.getStrategyPerformance())
            .thenReturn(strategies);

        // When & Then
        mockMvc.perform(get("/api/monitoring/strategies/performance"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isMap())
            .andExpect(jsonPath("$.['SMA Strategy'].strategyName").value("SMA Strategy"))
            .andExpect(jsonPath("$.['RSI Strategy'].strategyName").value("RSI Strategy"));
    }

    @Test
    void getStrategyPerformanceByName_WhenStrategyExists_ShouldReturnStrategy() throws Exception {
        // Given
        StrategyPerformance strategy = new StrategyPerformance("SMA Strategy");
        
        when(strategyService.getStrategyPerformance("SMA Strategy"))
            .thenReturn(strategy);

        // When & Then
        mockMvc.perform(get("/api/monitoring/strategies/SMA Strategy/performance"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.strategyName").value("SMA Strategy"));
    }

    @Test
    void getStrategyPerformanceByName_WhenStrategyNotFound_ShouldReturn404() throws Exception {
        // Given
        when(strategyService.getStrategyPerformance("NonExistent"))
            .thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/monitoring/strategies/NonExistent/performance"))
            .andExpect(status().isNotFound());
    }

    @Test
    void getPortfolioPerformance_ShouldReturnPerformanceMetrics() throws Exception {
        // Given
        PerformanceMetrics performance = new PerformanceMetrics();
        performance.setTotalUnrealizedPnL(new BigDecimal("1500.00"));
        performance.setDailyPnL(new BigDecimal("150.00"));
        performance.setTotalReturn(new BigDecimal("0.15"));
        
        when(portfolioService.calculatePerformance()).thenReturn(performance);

        // When & Then
        mockMvc.perform(get("/api/monitoring/portfolio/performance"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.totalUnrealizedPnL").value(1500.00))
            .andExpect(jsonPath("$.dailyPnL").value(150.00))
            .andExpect(jsonPath("$.totalReturn").value(0.15));
    }

    @Test
    void getHealthSummary_ShouldReturnHealthInformation() throws Exception {
        // Given
        Portfolio portfolio = new Portfolio();
        portfolio.setTotalValue(new BigDecimal("25000.00"));
        portfolio.setCashBalance(new BigDecimal("5000.00"));
        
        Position position = new Position();
        position.setSymbol("AAPL");
        
        PerformanceMetrics performance = new PerformanceMetrics();
        performance.setDailyPnL(new BigDecimal("200.00"));
        performance.setTotalUnrealizedPnL(new BigDecimal("2000.00"));
        
        when(portfolioService.getCurrentPortfolio()).thenReturn(portfolio);
        when(portfolioService.getPositions()).thenReturn(Collections.singletonList(position));
        when(portfolioService.calculatePerformance()).thenReturn(performance);

        // When & Then
        mockMvc.perform(get("/api/monitoring/health/summary"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.portfolio_value").value(25000.00))
            .andExpect(jsonPath("$.cash_balance").value(5000.00))
            .andExpect(jsonPath("$.active_positions").value(1))
            .andExpect(jsonPath("$.daily_pnl").value(200.00))
            .andExpect(jsonPath("$.total_pnl").value(2000.00))
            .andExpect(jsonPath("$.status").value("healthy"));
    }

    @Test
    void getHealthSummary_WhenExceptionOccurs_ShouldReturnErrorStatus() throws Exception {
        // Given
        when(portfolioService.getCurrentPortfolio())
            .thenThrow(new RuntimeException("Service unavailable"));

        // When & Then
        mockMvc.perform(get("/api/monitoring/health/summary"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value("error"))
            .andExpect(jsonPath("$.error").value("Service unavailable"));
    }

    @Test
    void getSystemStats_ShouldReturnSystemStatistics() throws Exception {
        // Given
        Position position1 = new Position();
        position1.setSymbol("AAPL");
        Position position2 = new Position();
        position2.setSymbol("GOOGL");
        
        StrategyPerformance strategy1 = new StrategyPerformance("SMA Strategy");
        strategy1.recordSignalGenerated(); // Make it active
        StrategyPerformance strategy2 = new StrategyPerformance("RSI Strategy");
        
        Map<String, StrategyPerformance> strategies = new HashMap<>();
        strategies.put("SMA Strategy", strategy1);
        strategies.put("RSI Strategy", strategy2);
        
        when(portfolioService.getPositions())
            .thenReturn(Arrays.asList(position1, position2));
        when(strategyService.getStrategyPerformance())
            .thenReturn(strategies);

        // When & Then
        mockMvc.perform(get("/api/monitoring/stats"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.total_positions").value(2))
            .andExpect(jsonPath("$.total_strategies").value(2))
            .andExpect(jsonPath("$.total_trades").value(0))
            .andExpect(jsonPath("$.average_win_rate").value(0.0));
    }
}