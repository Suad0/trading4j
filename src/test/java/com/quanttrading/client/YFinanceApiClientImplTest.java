package com.quanttrading.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.quanttrading.dto.HistoricalData;
import com.quanttrading.exception.ApiConnectionException;
import com.quanttrading.model.MarketData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

class YFinanceApiClientImplTest {
    
    private WireMockServer wireMockServer;
    private YFinanceApiClientImpl yFinanceApiClient;
    
    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(8090);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8090);
        
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Create client with mock server URL
        yFinanceApiClient = new YFinanceApiClientImpl(restTemplate, objectMapper, "http://localhost:8090");
    }
    
    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }
    
    @Test
    void testGetCurrentQuote_Success() {
        // Given
        String quoteResponseJson = """
            {
                "chart": {
                    "result": [{
                        "meta": {
                            "symbol": "AAPL",
                            "regularMarketPrice": 150.25
                        },
                        "timestamp": [1701432000],
                        "indicators": {
                            "quote": [{
                                "open": [149.50],
                                "high": [151.00],
                                "low": [148.75],
                                "close": [150.25],
                                "volume": [50000000]
                            }]
                        }
                    }]
                }
            }
            """;
        
        stubFor(get(urlPathEqualTo("/v8/finance/chart/AAPL"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(quoteResponseJson)));
        
        // When
        MarketData marketData = yFinanceApiClient.getCurrentQuote("AAPL");
        
        // Then
        assertNotNull(marketData);
        assertEquals("AAPL", marketData.getSymbol());
        assertEquals(new BigDecimal("150.25"), marketData.getPrice());
        assertEquals(0, new BigDecimal("149.50").compareTo(marketData.getOpen()));
        assertEquals(0, new BigDecimal("151.00").compareTo(marketData.getHigh()));
        assertEquals(new BigDecimal("148.75"), marketData.getLow());
        assertEquals(new BigDecimal("50000000"), marketData.getVolume());
    }
    
    @Test
    void testGetCurrentQuote_InvalidSymbol() {
        // Given
        String errorResponseJson = """
            {
                "chart": {
                    "result": null,
                    "error": {
                        "code": "Not Found",
                        "description": "No data found, symbol may be delisted"
                    }
                }
            }
            """;
        
        stubFor(get(urlPathEqualTo("/v8/finance/chart/INVALID"))
            .willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "application/json")
                .withBody(errorResponseJson)));
        
        // When & Then
        assertThrows(ApiConnectionException.class, () -> yFinanceApiClient.getCurrentQuote("INVALID"));
    }
    
    @Test
    void testGetHistoricalData_WithDates_Success() {
        // Given
        String historicalResponseJson = """
            {
                "chart": {
                    "result": [{
                        "meta": {
                            "symbol": "AAPL"
                        },
                        "timestamp": [1701432000, 1701518400],
                        "indicators": {
                            "quote": [{
                                "open": [149.50, 150.00],
                                "high": [151.00, 152.50],
                                "low": [148.75, 149.25],
                                "close": [150.25, 151.75],
                                "volume": [50000000, 45000000]
                            }]
                        }
                    }]
                }
            }
            """;
        
        stubFor(get(urlPathMatching("/v8/finance/chart/AAPL"))
            .withQueryParam("period1", matching("\\d+"))
            .withQueryParam("period2", matching("\\d+"))
            .withQueryParam("interval", equalTo("1d"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(historicalResponseJson)));
        
        LocalDate from = LocalDate.of(2023, 12, 1);
        LocalDate to = LocalDate.of(2023, 12, 2);
        
        // When
        List<HistoricalData> historicalData = yFinanceApiClient.getHistoricalData("AAPL", from, to);
        
        // Then
        assertNotNull(historicalData);
        assertEquals(2, historicalData.size());
        
        HistoricalData firstDay = historicalData.get(0);
        assertEquals("AAPL", firstDay.getSymbol());
        assertEquals(0, new BigDecimal("149.50").compareTo(firstDay.getOpen()));
        assertEquals(0, new BigDecimal("151.00").compareTo(firstDay.getHigh()));
        assertEquals(new BigDecimal("148.75"), firstDay.getLow());
        assertEquals(new BigDecimal("150.25"), firstDay.getClose());
        assertEquals(Long.valueOf(50000000), firstDay.getVolume());
    }
    
    @Test
    void testGetHistoricalData_WithPeriod_Success() {
        // Given
        String historicalResponseJson = """
            {
                "chart": {
                    "result": [{
                        "meta": {
                            "symbol": "GOOGL"
                        },
                        "timestamp": [1701432000],
                        "indicators": {
                            "quote": [{
                                "open": [2500.00],
                                "high": [2550.00],
                                "low": [2480.00],
                                "close": [2525.00],
                                "volume": [1000000]
                            }]
                        }
                    }]
                }
            }
            """;
        
        stubFor(get(urlPathMatching("/v8/finance/chart/GOOGL"))
            .withQueryParam("range", equalTo("1mo"))
            .withQueryParam("interval", equalTo("1d"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(historicalResponseJson)));
        
        // When
        List<HistoricalData> historicalData = yFinanceApiClient.getHistoricalData("GOOGL", "1mo");
        
        // Then
        assertNotNull(historicalData);
        assertEquals(1, historicalData.size());
        
        HistoricalData data = historicalData.get(0);
        assertEquals("GOOGL", data.getSymbol());
        assertEquals(0, new BigDecimal("2500.00").compareTo(data.getOpen()));
        assertEquals(0, new BigDecimal("2525.00").compareTo(data.getClose()));
    }
    
    @Test
    void testConnectionError() {
        // Given
        wireMockServer.stop(); // Simulate connection failure
        
        // When & Then
        assertThrows(ApiConnectionException.class, () -> yFinanceApiClient.getCurrentQuote("AAPL"));
    }
    
    @Test
    void testInvalidJsonResponse() {
        // Given
        stubFor(get(urlPathEqualTo("/v8/finance/chart/AAPL"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("invalid json")));
        
        // When & Then
        assertThrows(ApiConnectionException.class, () -> yFinanceApiClient.getCurrentQuote("AAPL"));
    }
    
    @Test
    void testEmptyResponse() {
        // Given
        String emptyResponseJson = """
            {
                "chart": {
                    "result": []
                }
            }
            """;
        
        stubFor(get(urlPathEqualTo("/v8/finance/chart/AAPL"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(emptyResponseJson)));
        
        // When & Then
        assertThrows(ApiConnectionException.class, () -> yFinanceApiClient.getCurrentQuote("AAPL"));
    }
    

}