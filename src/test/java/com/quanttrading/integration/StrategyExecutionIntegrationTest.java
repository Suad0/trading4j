package com.quanttrading.integration;

import com.quanttrading.model.MarketData;
import com.quanttrading.model.TradeType;
import com.quanttrading.service.MarketDataService;
import com.quanttrading.service.StrategyPerformance;
import com.quanttrading.service.StrategyService;
import com.quanttrading.service.TradingService;
import com.quanttrading.strategy.SimpleMovingAverageStrategy;
import com.quanttrading.strategy.StrategyConfig;
import com.quanttrading.strategy.TradingSignal;
import com.quanttrading.strategy.TradingStrategy;
import com.quanttrading.integration.fixtures.TestDataFixtures;
import com.quanttrading.integration.mocks.MockMarketDataProvider;
import com.quanttrading.strategy.HistoricalMarketData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for strategy execution with mock market data.
 * Tests strategy registration, execution, and performance tracking.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StrategyExecutionIntegrationTest {

    @Autowired
    private StrategyService strategyService;

    @Autowired
    private TradingService tradingService;

    @Autowired
    private MarketDataService marketDataService;

    @Autowired
    private TestDataFixtures testDataFixtures;

    @Autowired
    private MockMarketDataProvider mockMarketDataProvider;

    private TradingStrategy testStrategy;
    private StrategyConfig strategyConfig;

    @BeforeEach
    void setUp() {
        // Setup test portfolio and market data
        testDataFixtures.setupTestPortfolio();
        mockMarketDataProvider.setupMockData();
        
        // Create test strategy configuration
        strategyConfig = StrategyConfig.builder("TestSMAStrategy")
            .parameter("shortPeriod", "5")
            .parameter("longPeriod", "20")
            .parameter("symbol", "AAPL")
            .build();
        
        testStrategy = new SimpleMovingAverageStrategy(strategyConfig, new HistoricalMarketData());
    }

    @Test
    void strategyRegistration_ShouldAllowMultipleStrategies() {
        // Given - Multiple strategies
        StrategyConfig config1 = StrategyConfig.builder("SMA_AAPL")
            .parameter("shortPeriod", "5")
            .parameter("longPeriod", "20")
            .parameter("symbol", "AAPL")
            .build();
        
        StrategyConfig config2 = StrategyConfig.builder("SMA_GOOGL")
            .parameter("shortPeriod", "10")
            .parameter("longPeriod", "30")
            .parameter("symbol", "GOOGL")
            .build();

        TradingStrategy strategy1 = new SimpleMovingAverageStrategy(config1, new HistoricalMarketData());
        TradingStrategy strategy2 = new SimpleMovingAverageStrategy(config2, new HistoricalMarketData());

        // When - Register strategies
        strategyService.registerStrategy(strategy1);
        strategyService.registerStrategy(strategy2);

        // Then - Verify registration
        assertThat(strategyService.getAllStrategies()).hasSize(2);
        assertThat(strategyService.getStrategy("SMA_AAPL")).isEqualTo(strategy1);
        assertThat(strategyService.getStrategy("SMA_GOOGL")).isEqualTo(strategy2);
    }

    @Test
    void strategyExecution_WithBullishSignal_ShouldGenerateBuySignal() {
        // Given - Strategy and bullish market data
        strategyService.registerStrategy(testStrategy);
        MarketData bullishData = mockMarketDataProvider.createBullishMarketData("AAPL");

        // When - Execute strategy
        List<TradingSignal> signals = strategyService.executeStrategy("TestSMAStrategy", bullishData);

        // Then - Verify buy signal generated
        assertThat(signals).isNotEmpty();
        TradingSignal signal = signals.get(0);
        assertThat(signal.getSymbol()).isEqualTo("AAPL");
        assertThat(signal.getTradeType()).isEqualTo(TradeType.BUY);
        assertThat(signal.getConfidence()).isGreaterThan(0.0);
    }

    @Test
    void strategyExecution_WithBearishSignal_ShouldGenerateSellSignal() {
        // Given - Strategy and bearish market data
        strategyService.registerStrategy(testStrategy);
        testDataFixtures.createPosition("AAPL", new BigDecimal("100"), new BigDecimal("150.00"));
        MarketData bearishData = mockMarketDataProvider.createBearishMarketData("AAPL");

        // When - Execute strategy
        List<TradingSignal> signals = strategyService.executeStrategy("TestSMAStrategy", bearishData);

        // Then - Verify sell signal generated
        assertThat(signals).isNotEmpty();
        TradingSignal signal = signals.get(0);
        assertThat(signal.getSymbol()).isEqualTo("AAPL");
        assertThat(signal.getTradeType()).isEqualTo(TradeType.SELL);
        assertThat(signal.getConfidence()).isGreaterThan(0.0);
    }

    @Test
    void strategyExecution_WithNeutralSignal_ShouldGenerateNoSignal() {
        // Given - Strategy and neutral market data
        strategyService.registerStrategy(testStrategy);
        MarketData neutralData = mockMarketDataProvider.createNeutralMarketData("AAPL");

        // When - Execute strategy
        List<TradingSignal> signals = strategyService.executeStrategy("TestSMAStrategy", neutralData);

        // Then - Verify no signals generated
        assertThat(signals).isEmpty();
    }

    @Test
    void multipleStrategyExecution_ShouldExecuteAllEnabledStrategies() {
        // Given - Multiple enabled strategies
        StrategyConfig config1 = StrategyConfig.builder("SMA_AAPL_Short")
            .parameter("shortPeriod", "5")
            .parameter("longPeriod", "10")
            .parameter("symbol", "AAPL")
            .build();
        
        StrategyConfig config2 = StrategyConfig.builder("SMA_AAPL_Long")
            .parameter("shortPeriod", "10")
            .parameter("longPeriod", "30")
            .parameter("symbol", "AAPL")
            .build();

        TradingStrategy strategy1 = new SimpleMovingAverageStrategy(config1, new HistoricalMarketData());
        TradingStrategy strategy2 = new SimpleMovingAverageStrategy(config2, new HistoricalMarketData());

        strategyService.registerStrategy(strategy1);
        strategyService.registerStrategy(strategy2);

        MarketData marketData = mockMarketDataProvider.createBullishMarketData("AAPL");

        // When - Execute all strategies
        Map<String, List<TradingSignal>> results = strategyService.executeStrategies(marketData);

        // Then - Verify all strategies executed
        assertThat(results).hasSize(2);
        assertThat(results).containsKeys("SMA_AAPL_Short", "SMA_AAPL_Long");
    }

    @Test
    void strategyPerformanceTracking_ShouldTrackExecutionMetrics() {
        // Given - Strategy with execution history
        strategyService.registerStrategy(testStrategy);
        
        // Execute strategy multiple times
        for (int i = 0; i < 5; i++) {
            MarketData data = mockMarketDataProvider.createRandomMarketData("AAPL");
            strategyService.executeStrategy("TestSMAStrategy", data);
        }

        // When - Get performance metrics
        StrategyPerformance performance = strategyService.getStrategyPerformance("TestSMAStrategy");

        // Then - Verify performance tracking
        assertThat(performance).isNotNull();
        assertThat(performance.getStrategyName()).isEqualTo("TestSMAStrategy");
        assertThat(performance.getTotalSignalsGenerated()).isGreaterThanOrEqualTo(0);
        assertThat(performance.getTotalSignalsExecuted()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void strategyExecution_WithInsufficientData_ShouldHandleGracefully() {
        // Given - Strategy requiring historical data
        strategyService.registerStrategy(testStrategy);
        MarketData limitedData = new MarketData();
        limitedData.setSymbol("AAPL");
        limitedData.setPrice(new BigDecimal("150.00"));
        limitedData.setTimestamp(LocalDateTime.now());

        // When - Execute strategy with limited data
        List<TradingSignal> signals = strategyService.executeStrategy("TestSMAStrategy", limitedData);

        // Then - Verify graceful handling (no exceptions, may return empty signals)
        assertThat(signals).isNotNull();
    }

    @Test
    void strategyExecution_WithDisabledStrategy_ShouldNotExecute() {
        // Given - Disabled strategy
        strategyService.registerStrategy(testStrategy);
        strategyService.setStrategyEnabled("TestSMAStrategy", false);
        MarketData marketData = mockMarketDataProvider.createBullishMarketData("AAPL");

        // When - Execute disabled strategy
        List<TradingSignal> signals = strategyService.executeStrategy("TestSMAStrategy", marketData);

        // Then - Verify no execution
        assertThat(signals).isEmpty();
        
        StrategyPerformance performance = strategyService.getStrategyPerformance("TestSMAStrategy");
        assertThat(performance.getTotalSignalsGenerated()).isEqualTo(0);
    }

    @Test
    void strategySignalExecution_ShouldTriggerTrades() {
        // Given - Strategy that generates signals
        strategyService.registerStrategy(testStrategy);
        testDataFixtures.setupTradingAccount();
        MarketData bullishData = mockMarketDataProvider.createBullishMarketData("AAPL");

        // When - Execute strategy and process signals
        List<TradingSignal> signals = strategyService.executeStrategy("TestSMAStrategy", bullishData);
        
        // Process signals (simulate automatic execution)
        for (TradingSignal signal : signals) {
            if (signal.getTradeType() == TradeType.BUY) {
                tradingService.executeBuyOrder(signal.getSymbol(), signal.getQuantity(), "market");
            }
        }

        // Then - Verify trades were executed
        assertThat(tradingService.getRecentTrades(10)).isNotEmpty();
    }

    @Test
    void strategyBacktesting_WithHistoricalData_ShouldCalculatePerformance() {
        // Given - Strategy and historical market data
        strategyService.registerStrategy(testStrategy);
        List<MarketData> historicalData = mockMarketDataProvider.createHistoricalMarketData("AAPL", 30);

        // When - Backtest strategy
        int totalSignals = 0;
        for (MarketData data : historicalData) {
            List<TradingSignal> signals = strategyService.executeStrategy("TestSMAStrategy", data);
            totalSignals += signals.size();
        }

        // Then - Verify backtesting results
        StrategyPerformance performance = strategyService.getStrategyPerformance("TestSMAStrategy");
        assertThat(performance.getTotalSignalsGenerated()).isEqualTo(totalSignals);
        assertThat(performance.getTotalSignalsGenerated()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void strategyConfiguration_ShouldAffectSignalGeneration() {
        // Given - Two strategies with different configurations
        StrategyConfig aggressiveConfig = StrategyConfig.builder("AggressiveSMA")
            .parameter("shortPeriod", "3")
            .parameter("longPeriod", "7")
            .parameter("symbol", "AAPL")
            .build();
        
        StrategyConfig conservativeConfig = StrategyConfig.builder("ConservativeSMA")
            .parameter("shortPeriod", "20")
            .parameter("longPeriod", "50")
            .parameter("symbol", "AAPL")
            .build();

        TradingStrategy aggressiveStrategy = new SimpleMovingAverageStrategy(aggressiveConfig, new HistoricalMarketData());
        TradingStrategy conservativeStrategy = new SimpleMovingAverageStrategy(conservativeConfig, new HistoricalMarketData());

        strategyService.registerStrategy(aggressiveStrategy);
        strategyService.registerStrategy(conservativeStrategy);

        MarketData marketData = mockMarketDataProvider.createVolatileMarketData("AAPL");

        // When - Execute both strategies
        List<TradingSignal> aggressiveSignals = strategyService.executeStrategy("AggressiveSMA", marketData);
        List<TradingSignal> conservativeSignals = strategyService.executeStrategy("ConservativeSMA", marketData);

        // Then - Verify different behavior based on configuration
        // Aggressive strategy should be more sensitive to price changes
        assertThat(aggressiveSignals.size()).isGreaterThanOrEqualTo(conservativeSignals.size());
    }

    @Test
    void strategyRiskManagement_ShouldRespectPositionLimits() {
        // Given - Strategy with position limits
        StrategyConfig limitedConfig = StrategyConfig.builder("LimitedSMA")
            .parameter("shortPeriod", "5")
            .parameter("longPeriod", "20")
            .parameter("symbol", "AAPL")
            .parameter("maxPositionSize", "1000")
            .build();

        TradingStrategy limitedStrategy = new SimpleMovingAverageStrategy(limitedConfig, new HistoricalMarketData());
        strategyService.registerStrategy(limitedStrategy);

        // Create large existing position
        testDataFixtures.createPosition("AAPL", new BigDecimal("950"), new BigDecimal("150.00"));
        
        MarketData bullishData = mockMarketDataProvider.createBullishMarketData("AAPL");

        // When - Execute strategy
        List<TradingSignal> signals = strategyService.executeStrategy("LimitedSMA", bullishData);

        // Then - Verify position limits are respected
        for (TradingSignal signal : signals) {
            if (signal.getTradeType() == TradeType.BUY) {
                assertThat(signal.getQuantity()).isLessThanOrEqualTo(new BigDecimal("50")); // Max additional position
            }
        }
    }

    @Test
    void strategyPerformanceReset_ShouldClearMetrics() {
        // Given - Strategy with performance history
        strategyService.registerStrategy(testStrategy);
        
        // Generate some performance data
        MarketData data = mockMarketDataProvider.createBullishMarketData("AAPL");
        strategyService.executeStrategy("TestSMAStrategy", data);
        
        StrategyPerformance initialPerformance = strategyService.getStrategyPerformance("TestSMAStrategy");
        assertThat(initialPerformance.getTotalSignalsGenerated()).isGreaterThanOrEqualTo(0);

        // When - Reset performance
        strategyService.resetStrategyPerformance("TestSMAStrategy");

        // Then - Verify metrics are cleared
        StrategyPerformance resetPerformance = strategyService.getStrategyPerformance("TestSMAStrategy");
        assertThat(resetPerformance.getTotalSignalsGenerated()).isEqualTo(0);
        assertThat(resetPerformance.getTotalSignalsExecuted()).isEqualTo(0);
    }
}