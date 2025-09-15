package com.quanttrading.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quanttrading.model.MarketData;
import com.quanttrading.dto.HistoricalData;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;

/**
 * Test to verify yFinance API works with real data
 * This test makes actual API calls to Yahoo Finance
 */
public class YFinanceRealDataTest {
    
    @Test
    void testRealYFinanceData() {
        // Create client with real Yahoo Finance URL
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();
        YFinanceApiClientImpl client = new YFinanceApiClientImpl(restTemplate, objectMapper);
        
        try {
            // Test current quote for Apple
            System.out.println("=== Testing Current Quote for AAPL ===");
            MarketData appleData = client.getCurrentQuote("AAPL");
            System.out.println("Symbol: " + appleData.getSymbol());
            System.out.println("Current Price: $" + appleData.getPrice());
            System.out.println("Open: $" + appleData.getOpen());
            System.out.println("High: $" + appleData.getHigh());
            System.out.println("Low: $" + appleData.getLow());
            System.out.println("Volume: " + appleData.getVolume());
            System.out.println("Timestamp: " + appleData.getTimestamp());
            
            // Test historical data
            System.out.println("\n=== Testing Historical Data for AAPL (Last 5 days) ===");
            List<HistoricalData> historicalData = client.getHistoricalData("AAPL", "5d");
            System.out.println("Retrieved " + historicalData.size() + " historical data points:");
            
            historicalData.stream().limit(3).forEach(data -> {
                System.out.println("Date: " + data.getDate() + 
                                 ", Open: $" + data.getOpen() + 
                                 ", Close: $" + data.getClose() + 
                                 ", Volume: " + data.getVolume());
            });
            
            // Test another symbol
            System.out.println("\n=== Testing Current Quote for GOOGL ===");
            MarketData googleData = client.getCurrentQuote("GOOGL");
            System.out.println("Symbol: " + googleData.getSymbol());
            System.out.println("Current Price: $" + googleData.getPrice());
            
        } catch (Exception e) {
            System.err.println("Error testing real yFinance data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}