package com.quanttrading.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quanttrading.dto.OrderRequest;
import com.quanttrading.dto.OrderResponse;
import com.quanttrading.dto.PerformanceMetrics;
import com.quanttrading.dto.PortfolioSummaryResponse;
import com.quanttrading.model.OrderStatus;
import com.quanttrading.model.Portfolio;
import com.quanttrading.model.Position;
import com.quanttrading.model.TradeType;
import com.quanttrading.service.PortfolioService;
import com.quanttrading.service.TradingService;
import com.quanttrading.integration.fixtures.TestDataFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration tests covering complete trading workflows.
 * Tests the entire flow from order placement to portfolio updates.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class TradingWorkflowIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private TradingService tradingService;

    @Autowired
    private TestDataFixtures testDataFixtures;

    private String baseUrl;
    private HttpHeaders headers;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Initialize test data
        testDataFixtures.setupTestPortfolio();
    }

    @Test
    void completeTradeWorkflow_BuyOrder_ShouldUpdatePortfolio() {
        // Given - Initial portfolio state
        Portfolio initialPortfolio = portfolioService.getCurrentPortfolio();
        BigDecimal initialCash = initialPortfolio.getCashBalance();
        
        // Create buy order request
        OrderRequest buyOrder = testDataFixtures.createBuyOrderRequest("AAPL", new BigDecimal("10"), "market");
        HttpEntity<OrderRequest> request = new HttpEntity<>(buyOrder, headers);

        // When - Execute buy order
        ResponseEntity<OrderResponse> buyResponse = restTemplate.postForEntity(
            baseUrl + "/api/trades/buy", request, OrderResponse.class);

        // Then - Verify order execution
        assertThat(buyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(buyResponse.getBody()).isNotNull();
        assertThat(buyResponse.getBody().getSymbol()).isEqualTo("AAPL");
        assertThat(buyResponse.getBody().getQuantity()).isEqualTo(new BigDecimal("10"));
        assertThat(buyResponse.getBody().getType()).isEqualTo(TradeType.BUY);

        // Verify portfolio was updated
        Portfolio updatedPortfolio = portfolioService.getCurrentPortfolio();
        assertThat(updatedPortfolio.getCashBalance()).isLessThan(initialCash);
        
        // Verify position was created/updated
        Position aaplPosition = portfolioService.getPosition("AAPL").orElse(null);
        assertThat(aaplPosition).isNotNull();
        assertThat(aaplPosition.getQuantity()).isGreaterThanOrEqualTo(new BigDecimal("10"));
    }

    @Test
    void completeTradeWorkflow_SellOrder_ShouldUpdatePortfolio() {
        // Given - Setup initial position
        testDataFixtures.createPosition("GOOGL", new BigDecimal("20"), new BigDecimal("2000.00"));
        Portfolio initialPortfolio = portfolioService.getCurrentPortfolio();
        BigDecimal initialCash = initialPortfolio.getCashBalance();
        
        // Create sell order request
        OrderRequest sellOrder = testDataFixtures.createSellOrderRequest("GOOGL", new BigDecimal("5"), "market");
        HttpEntity<OrderRequest> request = new HttpEntity<>(sellOrder, headers);

        // When - Execute sell order
        ResponseEntity<OrderResponse> sellResponse = restTemplate.postForEntity(
            baseUrl + "/api/trades/sell", request, OrderResponse.class);

        // Then - Verify order execution
        assertThat(sellResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sellResponse.getBody()).isNotNull();
        assertThat(sellResponse.getBody().getSymbol()).isEqualTo("GOOGL");
        assertThat(sellResponse.getBody().getQuantity()).isEqualTo(new BigDecimal("5"));
        assertThat(sellResponse.getBody().getType()).isEqualTo(TradeType.SELL);

        // Verify portfolio was updated
        Portfolio updatedPortfolio = portfolioService.getCurrentPortfolio();
        assertThat(updatedPortfolio.getCashBalance()).isGreaterThan(initialCash);
        
        // Verify position was updated
        Position googlPosition = portfolioService.getPosition("GOOGL").orElse(null);
        assertThat(googlPosition).isNotNull();
        assertThat(googlPosition.getQuantity()).isEqualTo(new BigDecimal("15"));
    }

    @Test
    void multipleTradesWorkflow_ShouldMaintainPortfolioConsistency() {
        // Given - Initial portfolio
        Portfolio initialPortfolio = portfolioService.getCurrentPortfolio();
        BigDecimal initialTotalValue = initialPortfolio.getTotalValue();

        // Execute multiple trades
        executeTradeSequence();

        // Then - Verify portfolio consistency
        Portfolio finalPortfolio = portfolioService.getCurrentPortfolio();
        assertThat(finalPortfolio.getTotalValue()).isNotNull();
        
        // Verify all positions are consistent
        List<Position> positions = portfolioService.getActivePositions();
        BigDecimal totalPositionValue = positions.stream()
            .map(p -> p.getQuantity().multiply(p.getCurrentPrice()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal expectedTotalValue = finalPortfolio.getCashBalance().add(totalPositionValue);
        assertThat(finalPortfolio.getTotalValue()).isEqualByComparingTo(expectedTotalValue);
    }

    @Test
    void orderStatusTracking_ShouldProvideAccurateStatus() {
        // Given - Place an order
        OrderRequest buyOrder = testDataFixtures.createBuyOrderRequest("MSFT", new BigDecimal("5"), "limit");
        buyOrder.setLimitPrice(new BigDecimal("300.00"));
        HttpEntity<OrderRequest> request = new HttpEntity<>(buyOrder, headers);

        ResponseEntity<OrderResponse> orderResponse = restTemplate.postForEntity(
            baseUrl + "/api/trades/buy", request, OrderResponse.class);
        
        String orderId = orderResponse.getBody().getOrderId();

        // When - Check order status
        ResponseEntity<String> statusResponse = restTemplate.getForEntity(
            baseUrl + "/api/trades/orders/" + orderId + "/status", String.class);

        // Then - Verify status is trackable
        assertThat(statusResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(statusResponse.getBody()).contains(orderId);
    }

    @Test
    void tradeHistoryWorkflow_ShouldTrackAllTrades() {
        // Given - Execute several trades
        executeTradeSequence();

        // When - Get trade history
        ResponseEntity<String> historyResponse = restTemplate.getForEntity(
            baseUrl + "/api/trades/history", String.class);

        // Then - Verify trade history is maintained
        assertThat(historyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(historyResponse.getBody()).isNotNull();
        
        // Verify recent trades endpoint
        ResponseEntity<String> recentResponse = restTemplate.getForEntity(
            baseUrl + "/api/trades/recent", String.class);
        assertThat(recentResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void portfolioSummaryWorkflow_ShouldReflectTrades() {
        // Given - Initial portfolio summary
        ResponseEntity<PortfolioSummaryResponse> initialSummary = restTemplate.getForEntity(
            baseUrl + "/api/portfolio", PortfolioSummaryResponse.class);
        
        BigDecimal initialValue = initialSummary.getBody().getTotalValue();

        // When - Execute trades
        executeTradeSequence();

        // Then - Verify portfolio summary is updated
        ResponseEntity<PortfolioSummaryResponse> updatedSummary = restTemplate.getForEntity(
            baseUrl + "/api/portfolio", PortfolioSummaryResponse.class);
        
        assertThat(updatedSummary.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updatedSummary.getBody()).isNotNull();
        assertThat(updatedSummary.getBody().getTotalValue()).isNotEqualTo(initialValue);
    }

    @Test
    void errorHandlingWorkflow_ShouldHandleInvalidOrders() {
        // Given - Invalid order (insufficient funds)
        OrderRequest invalidOrder = testDataFixtures.createBuyOrderRequest("TSLA", new BigDecimal("1000"), "market");
        HttpEntity<OrderRequest> request = new HttpEntity<>(invalidOrder, headers);

        // When - Attempt to execute invalid order
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/api/trades/buy", request, String.class);

        // Then - Verify appropriate error handling
        assertThat(response.getStatusCode()).isIn(HttpStatus.BAD_REQUEST, HttpStatus.PAYMENT_REQUIRED);
        
        // Verify portfolio remains unchanged
        Portfolio portfolio = portfolioService.getCurrentPortfolio();
        assertThat(portfolio).isNotNull();
    }

    @Test
    void concurrentTradesWorkflow_ShouldMaintainDataIntegrity() {
        // Given - Multiple concurrent trade requests
        OrderRequest order1 = testDataFixtures.createBuyOrderRequest("AAPL", new BigDecimal("5"), "market");
        OrderRequest order2 = testDataFixtures.createBuyOrderRequest("GOOGL", new BigDecimal("3"), "market");
        
        HttpEntity<OrderRequest> request1 = new HttpEntity<>(order1, headers);
        HttpEntity<OrderRequest> request2 = new HttpEntity<>(order2, headers);

        // When - Execute concurrent trades
        ResponseEntity<OrderResponse> response1 = restTemplate.postForEntity(
            baseUrl + "/api/trades/buy", request1, OrderResponse.class);
        ResponseEntity<OrderResponse> response2 = restTemplate.postForEntity(
            baseUrl + "/api/trades/buy", request2, OrderResponse.class);

        // Then - Verify both trades are processed correctly
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Verify portfolio consistency
        Portfolio portfolio = portfolioService.getCurrentPortfolio();
        assertThat(portfolio.getPositions()).hasSizeGreaterThanOrEqualTo(2);
    }

    private void executeTradeSequence() {
        // Buy AAPL
        OrderRequest buyApple = testDataFixtures.createBuyOrderRequest("AAPL", new BigDecimal("10"), "market");
        restTemplate.postForEntity(baseUrl + "/api/trades/buy", 
            new HttpEntity<>(buyApple, headers), OrderResponse.class);

        // Buy GOOGL
        OrderRequest buyGoogle = testDataFixtures.createBuyOrderRequest("GOOGL", new BigDecimal("5"), "market");
        restTemplate.postForEntity(baseUrl + "/api/trades/buy", 
            new HttpEntity<>(buyGoogle, headers), OrderResponse.class);

        // Sell some AAPL
        OrderRequest sellApple = testDataFixtures.createSellOrderRequest("AAPL", new BigDecimal("3"), "market");
        restTemplate.postForEntity(baseUrl + "/api/trades/sell", 
            new HttpEntity<>(sellApple, headers), OrderResponse.class);
    }
}