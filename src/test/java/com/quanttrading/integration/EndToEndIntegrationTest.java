package com.quanttrading.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quanttrading.dto.MarketDataResponse;
import com.quanttrading.dto.OrderRequest;
import com.quanttrading.dto.OrderResponse;
import com.quanttrading.dto.PerformanceMetrics;
import com.quanttrading.dto.PortfolioSummaryResponse;
import com.quanttrading.model.MarketData;
import com.quanttrading.model.Portfolio;
import com.quanttrading.model.TradeType;
import com.quanttrading.service.MarketDataService;
import com.quanttrading.service.PortfolioService;
import com.quanttrading.service.StrategyService;
import com.quanttrading.service.TradingService;
import com.quanttrading.strategy.SimpleMovingAverageStrategy;
import com.quanttrading.strategy.StrategyConfig;
import com.quanttrading.strategy.TradingSignal;
import com.quanttrading.integration.fixtures.TestDataFixtures;
import com.quanttrading.integration.mocks.MockMarketDataProvider;
import com.quanttrading.strategy.HistoricalMarketData;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive end-to-end integration test covering the complete trading system workflow.
 * Tests the integration of all system components from market data to strategy execution to portfolio management.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class EndToEndIntegrationTest {

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
    private MarketDataService marketDataService;

    @Autowired
    private StrategyService strategyService;

    @Autowired
    private TestDataFixtures testDataFixtures;

    @Autowired
    private MockMarketDataProvider mockMarketDataProvider;

    private String baseUrl;
    private HttpHeaders headers;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Setup test environment
        testDataFixtures.setupTestPortfolio();
        mockMarketDataProvider.setupMockData();
    }

    @Test
    void completeAutomatedTradingWorkflow_ShouldExecuteSuccessfully() {
        // Phase 1: Setup - Initialize portfolio and strategy
        Portfolio initialPortfolio = portfolioService.getCurrentPortfolio();
        BigDecimal initialCash = initialPortfolio.getCashBalance();
        
        // Register a trading strategy
        StrategyConfig strategyConfig = StrategyConfig.builder("E2E_SMA_Strategy")
            .parameter("shortPeriod", "5")
            .parameter("longPeriod", "20")
            .parameter("symbol", "AAPL")
            .build();
        
        SimpleMovingAverageStrategy strategy = new SimpleMovingAverageStrategy(strategyConfig, new HistoricalMarketData());
        strategyService.registerStrategy(strategy);

        // Phase 2: Market Data - Fetch and verify market data
        ResponseEntity<MarketDataResponse> marketDataResponse = restTemplate.getForEntity(
            baseUrl + "/api/market/AAPL", MarketDataResponse.class);
        
        assertThat(marketDataResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(marketDataResponse.getBody()).isNotNull();
        assertThat(marketDataResponse.getBody().getSymbol()).isEqualTo("AAPL");

        // Phase 3: Strategy Execution - Generate trading signals
        MarketData bullishData = mockMarketDataProvider.createBullishMarketData("AAPL");
        List<TradingSignal> signals = strategyService.executeStrategy("E2E_SMA_Strategy", bullishData);

        // Phase 4: Order Execution - Execute generated signals
        if (!signals.isEmpty()) {
            TradingSignal signal = signals.get(0);
            OrderRequest orderRequest = new OrderRequest();
            orderRequest.setSymbol(signal.getSymbol());
            orderRequest.setQuantity(signal.getQuantity());
            orderRequest.setType(signal.getTradeType());
            orderRequest.setOrderType("market");

            HttpEntity<OrderRequest> request = new HttpEntity<>(orderRequest, headers);
            
            ResponseEntity<OrderResponse> orderResponse;
            if (signal.getTradeType() == TradeType.BUY) {
                orderResponse = restTemplate.postForEntity(
                    baseUrl + "/api/trades/buy", request, OrderResponse.class);
            } else {
                orderResponse = restTemplate.postForEntity(
                    baseUrl + "/api/trades/sell", request, OrderResponse.class);
            }
            
            assertThat(orderResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(orderResponse.getBody()).isNotNull();
            assertThat(orderResponse.getBody().getSymbol()).isEqualTo(signal.getSymbol());
        }

        // Phase 5: Portfolio Update - Verify portfolio reflects trades
        ResponseEntity<PortfolioSummaryResponse> portfolioResponse = restTemplate.getForEntity(
            baseUrl + "/api/portfolio", PortfolioSummaryResponse.class);
        
        assertThat(portfolioResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(portfolioResponse.getBody()).isNotNull();
        
        // If trades were executed, cash balance should have changed
        if (!signals.isEmpty()) {
            assertThat(portfolioResponse.getBody().getCashBalance()).isNotEqualTo(initialCash);
        }

        // Phase 6: Performance Tracking - Verify performance metrics
        ResponseEntity<PerformanceMetrics> performanceResponse = restTemplate.getForEntity(
            baseUrl + "/api/portfolio/performance", PerformanceMetrics.class);
        
        assertThat(performanceResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(performanceResponse.getBody()).isNotNull();
        assertThat(performanceResponse.getBody().getTotalValue()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void multiSymbolTradingWorkflow_ShouldHandleMultipleAssets() {
        // Setup multiple strategies for different symbols
        String[] symbols = {"AAPL", "GOOGL", "MSFT"};
        
        for (String symbol : symbols) {
            StrategyConfig config = StrategyConfig.builder("SMA_" + symbol)
                .parameter("shortPeriod", "5")
                .parameter("longPeriod", "20")
                .parameter("symbol", symbol)
                .build();
            
            strategyService.registerStrategy(new SimpleMovingAverageStrategy(config, new HistoricalMarketData()));
        }

        // Execute trades for each symbol
        for (String symbol : symbols) {
            // Generate market data
            MarketData marketData = mockMarketDataProvider.createBullishMarketData(symbol);
            
            // Execute strategy
            List<TradingSignal> signals = strategyService.executeStrategy("SMA_" + symbol, marketData);
            
            // Execute trades if signals generated
            for (TradingSignal signal : signals) {
                OrderRequest orderRequest = testDataFixtures.createBuyOrderRequest(
                    signal.getSymbol(), signal.getQuantity(), "market");
                
                HttpEntity<OrderRequest> request = new HttpEntity<>(orderRequest, headers);
                ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
                    baseUrl + "/api/trades/buy", request, OrderResponse.class);
                
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            }
        }

        // Verify portfolio contains positions for multiple symbols
        ResponseEntity<PortfolioSummaryResponse> portfolioResponse = restTemplate.getForEntity(
            baseUrl + "/api/portfolio", PortfolioSummaryResponse.class);
        
        assertThat(portfolioResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(portfolioResponse.getBody().getPositionCount()).isGreaterThan(0);
    }

    @Test
    void riskManagementWorkflow_ShouldPreventOverexposure() {
        // Setup portfolio with limited cash
        testDataFixtures.setupTestPortfolio(testDataFixtures.getDefaultAccountId(), new BigDecimal("5000.00"));
        
        // Attempt to place large order that exceeds available funds
        OrderRequest largeOrder = testDataFixtures.createBuyOrderRequest("AAPL", new BigDecimal("100"), "market");
        HttpEntity<OrderRequest> request = new HttpEntity<>(largeOrder, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/api/trades/buy", request, String.class);
        
        // Should be rejected due to insufficient funds
        assertThat(response.getStatusCode()).isIn(HttpStatus.BAD_REQUEST, HttpStatus.PAYMENT_REQUIRED);
        
        // Verify portfolio remains unchanged
        Portfolio portfolio = portfolioService.getCurrentPortfolio();
        assertThat(portfolio.getCashBalance()).isEqualByComparingTo(new BigDecimal("5000.00"));
    }

    @Test
    void marketDataDrivenTradingWorkflow_ShouldRespondToMarketConditions() {
        // Setup strategy
        StrategyConfig config = StrategyConfig.builder("MarketResponsiveStrategy")
            .parameter("shortPeriod", "3")
            .parameter("longPeriod", "10")
            .parameter("symbol", "TSLA")
            .build();
        
        strategyService.registerStrategy(new SimpleMovingAverageStrategy(config, new HistoricalMarketData()));

        // Test different market conditions
        testMarketCondition("bullish", () -> mockMarketDataProvider.createBullishMarketData("TSLA"));
        testMarketCondition("bearish", () -> mockMarketDataProvider.createBearishMarketData("TSLA"));
        testMarketCondition("neutral", () -> mockMarketDataProvider.createNeutralMarketData("TSLA"));
        testMarketCondition("volatile", () -> mockMarketDataProvider.createVolatileMarketData("TSLA"));

        // Verify strategy performance tracking
        assertThat(strategyService.getStrategyPerformance("MarketResponsiveStrategy")).isNotNull();
    }

    @Test
    void concurrentTradingWorkflow_ShouldMaintainDataIntegrity() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(5);
        
        // Submit multiple concurrent trading operations
        CompletableFuture<?>[] futures = new CompletableFuture[10];
        
        for (int i = 0; i < 10; i++) {
            final int tradeIndex = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    String symbol = tradeIndex % 2 == 0 ? "AAPL" : "GOOGL";
                    OrderRequest order = testDataFixtures.createBuyOrderRequest(
                        symbol, new BigDecimal("5"), "market");
                    
                    HttpEntity<OrderRequest> request = new HttpEntity<>(order, headers);
                    ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
                        baseUrl + "/api/trades/buy", request, OrderResponse.class);
                    
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                } catch (Exception e) {
                    // Some trades may fail due to insufficient funds, which is expected
                }
            }, executor);
        }
        
        // Wait for all trades to complete
        CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);
        
        // Verify portfolio integrity
        Portfolio portfolio = portfolioService.getCurrentPortfolio();
        assertThat(portfolio).isNotNull();
        assertThat(portfolio.getTotalValue()).isGreaterThan(BigDecimal.ZERO);
        
        executor.shutdown();
    }

    @Test
    void errorRecoveryWorkflow_ShouldHandleFailuresGracefully() {
        // Test invalid symbol
        ResponseEntity<String> invalidSymbolResponse = restTemplate.getForEntity(
            baseUrl + "/api/market/INVALID", String.class);
        assertThat(invalidSymbolResponse.getStatusCode()).isIn(HttpStatus.BAD_REQUEST, HttpStatus.NOT_FOUND);

        // Test invalid order
        OrderRequest invalidOrder = new OrderRequest();
        invalidOrder.setSymbol("AAPL");
        invalidOrder.setQuantity(new BigDecimal("-10")); // Negative quantity
        invalidOrder.setType(TradeType.BUY);
        invalidOrder.setOrderType("market");
        
        HttpEntity<OrderRequest> request = new HttpEntity<>(invalidOrder, headers);
        ResponseEntity<String> invalidOrderResponse = restTemplate.postForEntity(
            baseUrl + "/api/trades/buy", request, String.class);
        assertThat(invalidOrderResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Verify system remains operational after errors
        ResponseEntity<PortfolioSummaryResponse> portfolioResponse = restTemplate.getForEntity(
            baseUrl + "/api/portfolio", PortfolioSummaryResponse.class);
        assertThat(portfolioResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void performanceOptimizationWorkflow_ShouldHandleLargeVolumes() {
        // Setup large portfolio
        testDataFixtures.setupLargePortfolio();
        
        // Execute multiple operations
        long startTime = System.currentTimeMillis();
        
        // Get portfolio summary
        ResponseEntity<PortfolioSummaryResponse> portfolioResponse = restTemplate.getForEntity(
            baseUrl + "/api/portfolio", PortfolioSummaryResponse.class);
        assertThat(portfolioResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Get performance metrics
        ResponseEntity<PerformanceMetrics> performanceResponse = restTemplate.getForEntity(
            baseUrl + "/api/portfolio/performance", PerformanceMetrics.class);
        assertThat(performanceResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Get trade history
        ResponseEntity<String> historyResponse = restTemplate.getForEntity(
            baseUrl + "/api/trades/history", String.class);
        assertThat(historyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        
        // Verify reasonable performance (should complete within 5 seconds)
        assertThat(executionTime).isLessThan(5000);
    }

    @Test
    void dataConsistencyWorkflow_ShouldMaintainAccuracy() {
        // Execute a series of trades and verify data consistency at each step
        BigDecimal initialCash = portfolioService.getCurrentPortfolio().getCashBalance();
        
        // Execute buy order
        OrderRequest buyOrder = testDataFixtures.createBuyOrderRequest("AAPL", new BigDecimal("10"), "market");
        HttpEntity<OrderRequest> buyRequest = new HttpEntity<>(buyOrder, headers);
        
        ResponseEntity<OrderResponse> buyResponse = restTemplate.postForEntity(
            baseUrl + "/api/trades/buy", buyRequest, OrderResponse.class);
        assertThat(buyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Verify portfolio consistency after buy
        Portfolio portfolioAfterBuy = portfolioService.getCurrentPortfolio();
        assertThat(portfolioAfterBuy.getCashBalance()).isLessThan(initialCash);
        
        // Execute sell order
        OrderRequest sellOrder = testDataFixtures.createSellOrderRequest("AAPL", new BigDecimal("5"), "market");
        HttpEntity<OrderRequest> sellRequest = new HttpEntity<>(sellOrder, headers);
        
        ResponseEntity<OrderResponse> sellResponse = restTemplate.postForEntity(
            baseUrl + "/api/trades/sell", sellRequest, OrderResponse.class);
        assertThat(sellResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Verify portfolio consistency after sell
        Portfolio portfolioAfterSell = portfolioService.getCurrentPortfolio();
        assertThat(portfolioAfterSell.getCashBalance()).isGreaterThan(portfolioAfterBuy.getCashBalance());
        
        // Verify position quantities are correct
        assertThat(portfolioService.getPosition("AAPL")).isPresent();
        assertThat(portfolioService.getPosition("AAPL").get().getQuantity())
            .isEqualByComparingTo(new BigDecimal("5"));
    }

    private void testMarketCondition(String conditionName, java.util.function.Supplier<MarketData> dataSupplier) {
        MarketData marketData = dataSupplier.get();
        List<TradingSignal> signals = strategyService.executeStrategy("MarketResponsiveStrategy", marketData);
        
        // Log the condition and signals for debugging
        System.out.println("Market condition: " + conditionName + ", Signals generated: " + signals.size());
        
        // Verify signals are reasonable (not null and contain valid data)
        assertThat(signals).isNotNull();
        for (TradingSignal signal : signals) {
            assertThat(signal.getSymbol()).isNotNull();
            assertThat(signal.getTradeType()).isNotNull();
            assertThat(signal.getQuantity()).isGreaterThan(BigDecimal.ZERO);
        }
    }
}