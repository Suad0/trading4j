package com.quanttrading.integration;

import com.quanttrading.dto.PerformanceMetrics;
import com.quanttrading.dto.PortfolioSummaryResponse;
import com.quanttrading.dto.PositionResponse;
import com.quanttrading.model.Portfolio;
import com.quanttrading.model.Position;
import com.quanttrading.service.MarketDataService;
import com.quanttrading.service.PortfolioService;
import com.quanttrading.integration.fixtures.TestDataFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for portfolio management and performance calculation scenarios.
 * Tests portfolio operations, position management, and performance metrics.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class PortfolioManagementIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private MarketDataService marketDataService;

    @Autowired
    private TestDataFixtures testDataFixtures;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        testDataFixtures.setupTestPortfolio();
    }

    @Test
    void portfolioSummary_ShouldCalculateCorrectTotals() {
        // Given - Portfolio with multiple positions
        testDataFixtures.createPosition("AAPL", new BigDecimal("100"), new BigDecimal("150.00"));
        testDataFixtures.createPosition("GOOGL", new BigDecimal("50"), new BigDecimal("2000.00"));
        testDataFixtures.createPosition("MSFT", new BigDecimal("75"), new BigDecimal("300.00"));

        // When - Get portfolio summary
        ResponseEntity<PortfolioSummaryResponse> response = restTemplate.getForEntity(
            baseUrl + "/api/portfolio", PortfolioSummaryResponse.class);

        // Then - Verify calculations
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PortfolioSummaryResponse summary = response.getBody();
        assertThat(summary).isNotNull();
        
        // Verify total value calculation
        BigDecimal expectedPositionValue = new BigDecimal("100").multiply(new BigDecimal("150.00"))
            .add(new BigDecimal("50").multiply(new BigDecimal("2000.00")))
            .add(new BigDecimal("75").multiply(new BigDecimal("300.00")));
        
        BigDecimal expectedTotalValue = summary.getCashBalance().add(expectedPositionValue);
        assertThat(summary.getTotalValue()).isEqualByComparingTo(expectedTotalValue);
        
        // Verify position count
        assertThat(summary.getPositionCount()).isEqualTo(3);
    }

    @Test
    void portfolioPositions_ShouldReturnAllPositions() {
        // Given - Multiple positions
        testDataFixtures.createPosition("AAPL", new BigDecimal("100"), new BigDecimal("150.00"));
        testDataFixtures.createPosition("GOOGL", new BigDecimal("50"), new BigDecimal("2000.00"));
        testDataFixtures.createPosition("TSLA", new BigDecimal("25"), new BigDecimal("800.00"));

        // When - Get all positions
        ResponseEntity<List<PositionResponse>> response = restTemplate.exchange(
            baseUrl + "/api/portfolio/positions",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<PositionResponse>>() {}
        );

        // Then - Verify positions
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<PositionResponse> positions = response.getBody();
        assertThat(positions).isNotNull();
        assertThat(positions).hasSize(3);
        
        // Verify position details
        PositionResponse aaplPosition = positions.stream()
            .filter(p -> "AAPL".equals(p.getSymbol()))
            .findFirst()
            .orElse(null);
        
        assertThat(aaplPosition).isNotNull();
        assertThat(aaplPosition.getQuantity()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(aaplPosition.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    void performanceMetrics_ShouldCalculateCorrectly() {
        // Given - Portfolio with positions and trades
        setupPortfolioWithTrades();

        // When - Get performance metrics
        ResponseEntity<PerformanceMetrics> response = restTemplate.getForEntity(
            baseUrl + "/api/portfolio/performance", PerformanceMetrics.class);

        // Then - Verify performance calculations
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PerformanceMetrics metrics = response.getBody();
        assertThat(metrics).isNotNull();
        
        // Verify basic metrics are present
        assertThat(metrics.getTotalValue()).isNotNull();
        assertThat(metrics.getTotalPnL()).isNotNull();
        assertThat(metrics.getDailyPnL()).isNotNull();
        assertThat(metrics.getTotalReturn()).isNotNull();
        
        // Verify metrics are reasonable
        assertThat(metrics.getTotalValue()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void performanceMetrics_WithProfitablePositions_ShouldShowPositivePnL() {
        // Given - Profitable positions (current price > average price)
        testDataFixtures.createPositionWithTrades("AAPL", 
            new BigDecimal("100"), new BigDecimal("140.00"), new BigDecimal("160.00"));

        // When - Calculate performance
        ResponseEntity<PerformanceMetrics> response = restTemplate.getForEntity(
            baseUrl + "/api/portfolio/performance", PerformanceMetrics.class);

        // Then - Verify positive P&L
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PerformanceMetrics metrics = response.getBody();
        assertThat(metrics).isNotNull();
        assertThat(metrics.getTotalPnL()).isGreaterThan(BigDecimal.ZERO);
        assertThat(metrics.getTotalReturn()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void performanceMetrics_WithLosingPositions_ShouldShowNegativePnL() {
        // Given - Losing positions (current price < average price)
        testDataFixtures.createPositionWithTrades("GOOGL", 
            new BigDecimal("50"), new BigDecimal("2100.00"), new BigDecimal("1900.00"));

        // When - Calculate performance
        ResponseEntity<PerformanceMetrics> response = restTemplate.getForEntity(
            baseUrl + "/api/portfolio/performance", PerformanceMetrics.class);

        // Then - Verify negative P&L
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PerformanceMetrics metrics = response.getBody();
        assertThat(metrics).isNotNull();
        assertThat(metrics.getTotalPnL()).isLessThan(BigDecimal.ZERO);
        assertThat(metrics.getTotalReturn()).isLessThan(BigDecimal.ZERO);
    }

    @Test
    void positionUpdates_AfterTrades_ShouldReflectCorrectAveragePrice() {
        // Given - Initial position
        testDataFixtures.createPosition("MSFT", new BigDecimal("50"), new BigDecimal("300.00"));
        
        // When - Add more shares at different price
        portfolioService.updatePosition("MSFT", new BigDecimal("50"), new BigDecimal("320.00"));

        // Then - Verify average price calculation
        Position position = portfolioService.getPosition("MSFT").orElse(null);
        assertThat(position).isNotNull();
        assertThat(position.getQuantity()).isEqualByComparingTo(new BigDecimal("100"));
        
        // Average price should be (50*300 + 50*320) / 100 = 310
        BigDecimal expectedAverage = new BigDecimal("310.00");
        assertThat(position.getAveragePrice()).isEqualByComparingTo(expectedAverage);
    }

    @Test
    void portfolioSynchronization_ShouldUpdateFromExternalSource() {
        // Given - Portfolio with outdated positions
        Portfolio initialPortfolio = portfolioService.getCurrentPortfolio();
        
        // When - Synchronize portfolio
        portfolioService.synchronizePortfolio(initialPortfolio.getAccountId());

        // Then - Verify synchronization occurred
        Portfolio syncedPortfolio = portfolioService.getCurrentPortfolio();
        assertThat(syncedPortfolio).isNotNull();
        assertThat(syncedPortfolio.getLastUpdated()).isAfter(initialPortfolio.getLastUpdated());
    }

    @Test
    void activePositions_ShouldExcludeZeroQuantityPositions() {
        // Given - Mix of active and zero positions
        testDataFixtures.createPosition("AAPL", new BigDecimal("100"), new BigDecimal("150.00"));
        testDataFixtures.createPosition("GOOGL", BigDecimal.ZERO, new BigDecimal("2000.00"));
        testDataFixtures.createPosition("MSFT", new BigDecimal("50"), new BigDecimal("300.00"));

        // When - Get active positions
        List<Position> activePositions = portfolioService.getActivePositions();

        // Then - Verify only non-zero positions are returned
        assertThat(activePositions).hasSize(2);
        assertThat(activePositions.stream().map(Position::getSymbol))
            .containsExactlyInAnyOrder("AAPL", "MSFT");
        assertThat(activePositions.stream().allMatch(p -> p.getQuantity().compareTo(BigDecimal.ZERO) > 0))
            .isTrue();
    }

    @Test
    void portfolioValue_WithMarketDataUpdates_ShouldReflectCurrentPrices() {
        // Given - Positions with initial prices
        testDataFixtures.createPosition("AAPL", new BigDecimal("100"), new BigDecimal("150.00"));
        BigDecimal initialValue = portfolioService.getCurrentPortfolio().getTotalValue();

        // When - Update market prices
        testDataFixtures.updateMarketPrice("AAPL", new BigDecimal("160.00"));
        portfolioService.updateCurrentPrices(portfolioService.getCurrentPortfolio().getAccountId());

        // Then - Verify portfolio value reflects new prices
        BigDecimal updatedValue = portfolioService.getCurrentPortfolio().getTotalValue();
        assertThat(updatedValue).isGreaterThan(initialValue);
        
        // Verify the increase matches the price change
        BigDecimal expectedIncrease = new BigDecimal("100").multiply(new BigDecimal("10.00")); // 100 shares * $10 increase
        assertThat(updatedValue.subtract(initialValue)).isEqualByComparingTo(expectedIncrease);
    }

    @Test
    void performanceCalculation_OverTime_ShouldTrackReturns() {
        // Given - Portfolio with historical performance
        setupPortfolioWithHistoricalData();

        // When - Calculate performance metrics
        PerformanceMetrics metrics = portfolioService.calculatePerformance();

        // Then - Verify performance tracking
        assertThat(metrics).isNotNull();
        assertThat(metrics.getTotalReturn()).isNotNull();
        assertThat(metrics.getDailyPnL()).isNotNull();
        
        // Verify return calculation is reasonable
        if (metrics.getTotalValue().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal returnPercentage = metrics.getTotalReturn().multiply(new BigDecimal("100"));
            assertThat(returnPercentage.abs()).isLessThan(new BigDecimal("1000")); // Reasonable return range
        }
    }

    @Test
    void multipleAccountPortfolios_ShouldBeHandledSeparately() {
        // Given - Multiple account portfolios
        String account1 = "test-account-1";
        String account2 = "test-account-2";
        
        testDataFixtures.createPositionForAccount(account1, "AAPL", new BigDecimal("100"), new BigDecimal("150.00"));
        testDataFixtures.createPositionForAccount(account2, "GOOGL", new BigDecimal("50"), new BigDecimal("2000.00"));

        // When - Get positions for each account
        List<Position> account1Positions = portfolioService.getPositions(account1);
        List<Position> account2Positions = portfolioService.getPositions(account2);

        // Then - Verify account separation
        assertThat(account1Positions).hasSize(1);
        assertThat(account1Positions.get(0).getSymbol()).isEqualTo("AAPL");
        
        assertThat(account2Positions).hasSize(1);
        assertThat(account2Positions.get(0).getSymbol()).isEqualTo("GOOGL");
    }

    private void setupPortfolioWithTrades() {
        // Create positions with some profit/loss
        testDataFixtures.createPositionWithTrades("AAPL", 
            new BigDecimal("100"), new BigDecimal("145.00"), new BigDecimal("155.00"));
        testDataFixtures.createPositionWithTrades("GOOGL", 
            new BigDecimal("25"), new BigDecimal("2050.00"), new BigDecimal("1980.00"));
        testDataFixtures.createPositionWithTrades("MSFT", 
            new BigDecimal("50"), new BigDecimal("295.00"), new BigDecimal("305.00"));
    }

    private void setupPortfolioWithHistoricalData() {
        // Create portfolio with historical performance data
        testDataFixtures.createPortfolioWithHistory();
    }
}