package com.quanttrading.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.quanttrading.config.AlpacaConfig;
import com.quanttrading.dto.AccountInfo;
import com.quanttrading.dto.OrderRequest;
import com.quanttrading.dto.OrderResponse;
import com.quanttrading.exception.ApiConnectionException;
import com.quanttrading.exception.InvalidOrderException;
import com.quanttrading.model.OrderStatus;
import com.quanttrading.model.Position;
import com.quanttrading.model.TradeType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

class AlpacaApiClientImplTest {
    
    private WireMockServer wireMockServer;
    private AlpacaApiClientImpl alpacaApiClient;
    private AlpacaConfig alpacaConfig;
    
    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);
        
        alpacaConfig = new AlpacaConfig("http://localhost:8089", "test-api-key", "test-secret-key", true);
        
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();
        
        alpacaApiClient = new AlpacaApiClientImpl(alpacaConfig, restTemplate, objectMapper);
    }
    
    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }
    
    @Test
    void testPlaceOrder_Success() {
        // Given
        String orderResponseJson = """
            {
                "id": "order-123",
                "symbol": "AAPL",
                "qty": "10",
                "side": "buy",
                "type": "market",
                "status": "submitted",
                "submitted_at": "2023-12-01T10:00:00Z",
                "filled_qty": "0",
                "filled_avg_price": null,
                "filled_at": null
            }
            """;
        
        stubFor(post(urlEqualTo("/v2/orders"))
            .withHeader("APCA-API-KEY-ID", equalTo("test-api-key"))
            .withHeader("APCA-API-SECRET-KEY", equalTo("test-secret-key"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody(orderResponseJson)));
        
        OrderRequest request = new OrderRequest("AAPL", new BigDecimal("10"), TradeType.BUY, "market");
        
        // When
        OrderResponse response = alpacaApiClient.placeOrder(request);
        
        // Then
        assertNotNull(response);
        assertEquals("order-123", response.getOrderId());
        assertEquals("AAPL", response.getSymbol());
        assertEquals(new BigDecimal("10"), response.getQuantity());
        assertEquals(TradeType.BUY, response.getType());
        assertEquals(OrderStatus.SUBMITTED, response.getStatus());
    }
    
    @Test
    void testPlaceOrder_InvalidRequest() {
        // Given
        OrderRequest request = new OrderRequest("", new BigDecimal("-1"), TradeType.BUY, "market");
        
        // When & Then
        assertThrows(InvalidOrderException.class, () -> alpacaApiClient.placeOrder(request));
    }
    
    @Test
    void testPlaceOrder_ApiError() {
        // Given
        stubFor(post(urlEqualTo("/v2/orders"))
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"message\": \"Invalid order\"}")));
        
        OrderRequest request = new OrderRequest("AAPL", new BigDecimal("10"), TradeType.BUY, "market");
        
        // When & Then
        assertThrows(InvalidOrderException.class, () -> alpacaApiClient.placeOrder(request));
    }
    
    @Test
    void testGetOrderStatus_Success() {
        // Given
        String orderStatusJson = """
            {
                "id": "order-123",
                "status": "filled"
            }
            """;
        
        stubFor(get(urlEqualTo("/v2/orders/order-123"))
            .withHeader("APCA-API-KEY-ID", equalTo("test-api-key"))
            .withHeader("APCA-API-SECRET-KEY", equalTo("test-secret-key"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(orderStatusJson)));
        
        // When
        OrderStatus status = alpacaApiClient.getOrderStatus("order-123");
        
        // Then
        assertEquals(OrderStatus.FILLED, status);
    }
    
    @Test
    void testGetAccountInfo_Success() {
        // Given
        String accountInfoJson = """
            {
                "id": "account-123",
                "cash": "10000.00",
                "portfolio_value": "15000.00",
                "buying_power": "20000.00",
                "daytrading_buying_power": "40000.00",
                "status": "ACTIVE",
                "pattern_day_trader": false
            }
            """;
        
        stubFor(get(urlEqualTo("/v2/account"))
            .withHeader("APCA-API-KEY-ID", equalTo("test-api-key"))
            .withHeader("APCA-API-SECRET-KEY", equalTo("test-secret-key"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(accountInfoJson)));
        
        // When
        AccountInfo accountInfo = alpacaApiClient.getAccountInfo();
        
        // Then
        assertNotNull(accountInfo);
        assertEquals("account-123", accountInfo.getAccountId());
        assertEquals(new BigDecimal("10000.00"), accountInfo.getCash());
        assertEquals(new BigDecimal("15000.00"), accountInfo.getPortfolioValue());
        assertEquals(new BigDecimal("20000.00"), accountInfo.getBuyingPower());
        assertEquals("ACTIVE", accountInfo.getStatus());
        assertFalse(accountInfo.isPatternDayTrader());
    }
    
    @Test
    void testGetPositions_Success() {
        // Given
        String positionsJson = """
            [
                {
                    "symbol": "AAPL",
                    "qty": "10",
                    "avg_entry_price": "150.00",
                    "market_value": "1600.00",
                    "unrealized_pl": "100.00"
                },
                {
                    "symbol": "GOOGL",
                    "qty": "5",
                    "avg_entry_price": "2500.00",
                    "market_value": "13000.00",
                    "unrealized_pl": "500.00"
                }
            ]
            """;
        
        stubFor(get(urlEqualTo("/v2/positions"))
            .withHeader("APCA-API-KEY-ID", equalTo("test-api-key"))
            .withHeader("APCA-API-SECRET-KEY", equalTo("test-secret-key"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(positionsJson)));
        
        // When
        List<Position> positions = alpacaApiClient.getPositions();
        
        // Then
        assertNotNull(positions);
        assertEquals(2, positions.size());
        
        Position applePosition = positions.get(0);
        assertEquals("AAPL", applePosition.getSymbol());
        assertEquals(new BigDecimal("10"), applePosition.getQuantity());
        assertEquals(new BigDecimal("150.00"), applePosition.getAveragePrice());
        assertEquals(new BigDecimal("100.00"), applePosition.getUnrealizedPnL());
    }
    
    @Test
    void testConnectionError() {
        // Given
        wireMockServer.stop(); // Simulate connection failure
        
        OrderRequest request = new OrderRequest("AAPL", new BigDecimal("10"), TradeType.BUY, "market");
        
        // When & Then
        assertThrows(ApiConnectionException.class, () -> alpacaApiClient.placeOrder(request));
    }
    
    @Test
    void testLimitOrderValidation() {
        // Given
        OrderRequest limitOrder = new OrderRequest("AAPL", new BigDecimal("10"), TradeType.BUY, "limit");
        limitOrder.setLimitPrice(new BigDecimal("150.00"));
        
        String orderResponseJson = """
            {
                "id": "order-456",
                "symbol": "AAPL",
                "qty": "10",
                "side": "buy",
                "type": "limit",
                "status": "submitted",
                "submitted_at": "2023-12-01T10:00:00Z"
            }
            """;
        
        stubFor(post(urlEqualTo("/v2/orders"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody(orderResponseJson)));
        
        // When
        OrderResponse response = alpacaApiClient.placeOrder(limitOrder);
        
        // Then
        assertNotNull(response);
        assertEquals("order-456", response.getOrderId());
    }
    
    @Test
    void testLimitOrderWithoutPrice_ThrowsException() {
        // Given
        OrderRequest limitOrder = new OrderRequest("AAPL", new BigDecimal("10"), TradeType.BUY, "limit");
        // No limit price set
        
        // When & Then
        assertThrows(InvalidOrderException.class, () -> alpacaApiClient.placeOrder(limitOrder));
    }
}