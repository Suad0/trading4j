package com.quanttrading.integration.fixtures;

import com.quanttrading.dto.OrderRequest;
import com.quanttrading.model.MarketData;
import com.quanttrading.model.Portfolio;
import com.quanttrading.model.Position;
import com.quanttrading.model.Trade;
import com.quanttrading.model.TradeType;
import com.quanttrading.repository.PortfolioRepository;
import com.quanttrading.repository.PositionRepository;
import com.quanttrading.repository.TradeRepository;
import com.quanttrading.service.MarketDataService;
import com.quanttrading.service.PortfolioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Test data fixtures and helper utilities for integration tests.
 * Provides methods to create test portfolios, positions, trades, and market data.
 */
@Component
public class TestDataFixtures {

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private MarketDataService marketDataService;

    private static final String DEFAULT_ACCOUNT_ID = "test-account-default";
    private static final BigDecimal DEFAULT_CASH_BALANCE = new BigDecimal("100000.00");

    /**
     * Setup a basic test portfolio with default cash balance
     */
    public Portfolio setupTestPortfolio() {
        return setupTestPortfolio(DEFAULT_ACCOUNT_ID, DEFAULT_CASH_BALANCE);
    }

    /**
     * Setup a test portfolio with specified account ID and cash balance
     */
    public Portfolio setupTestPortfolio(String accountId, BigDecimal cashBalance) {
        Portfolio portfolio = new Portfolio();
        portfolio.setAccountId(accountId);
        portfolio.setCashBalance(cashBalance);
        portfolio.setTotalValue(cashBalance);
        portfolio.setLastUpdated(LocalDateTime.now());
        portfolio.setPositions(new ArrayList<>());
        
        return portfolioRepository.save(portfolio);
    }

    /**
     * Create a position for the default account
     */
    public Position createPosition(String symbol, BigDecimal quantity, BigDecimal averagePrice) {
        return createPositionForAccount(DEFAULT_ACCOUNT_ID, symbol, quantity, averagePrice);
    }

    /**
     * Create a position for a specific account
     */
    public Position createPositionForAccount(String accountId, String symbol, BigDecimal quantity, BigDecimal averagePrice) {
        Position position = new Position();
        position.setSymbol(symbol);
        position.setQuantity(quantity);
        position.setAveragePrice(averagePrice);
        position.setCurrentPrice(averagePrice); // Start with same price
        position.setUnrealizedPnL(BigDecimal.ZERO);
        position.setLastUpdated(LocalDateTime.now());
        
        // Set portfolio relationship
        Portfolio portfolio = portfolioRepository.findByAccountId(accountId).orElse(null);
        if (portfolio != null) {
            position.setPortfolio(portfolio);
        }
        
        return positionRepository.save(position);
    }

    /**
     * Create a position with trades (for P&L calculation)
     */
    public Position createPositionWithTrades(String symbol, BigDecimal quantity, 
                                           BigDecimal averagePrice, BigDecimal currentPrice) {
        Position position = createPosition(symbol, quantity, averagePrice);
        position.setCurrentPrice(currentPrice);
        
        // Calculate unrealized P&L
        BigDecimal unrealizedPnL = quantity.multiply(currentPrice.subtract(averagePrice));
        position.setUnrealizedPnL(unrealizedPnL);
        
        // Create corresponding trade
        createTrade(symbol, quantity, averagePrice, TradeType.BUY);
        
        return positionRepository.save(position);
    }

    /**
     * Create a trade record
     */
    public Trade createTrade(String symbol, BigDecimal quantity, BigDecimal price, TradeType tradeType) {
        Trade trade = new Trade();
        trade.setOrderId(UUID.randomUUID().toString());
        trade.setAccountId(DEFAULT_ACCOUNT_ID);
        trade.setSymbol(symbol);
        trade.setQuantity(quantity);
        trade.setPrice(price);
        trade.setType(tradeType);
        trade.setExecutedAt(LocalDateTime.now());
        trade.setStrategyName("TestStrategy");
        
        return tradeRepository.save(trade);
    }

    /**
     * Create a buy order request
     */
    public OrderRequest createBuyOrderRequest(String symbol, BigDecimal quantity, String orderType) {
        OrderRequest request = new OrderRequest();
        request.setSymbol(symbol);
        request.setQuantity(quantity);
        request.setType(TradeType.BUY);
        request.setOrderType(orderType);
        return request;
    }

    /**
     * Create a sell order request
     */
    public OrderRequest createSellOrderRequest(String symbol, BigDecimal quantity, String orderType) {
        OrderRequest request = new OrderRequest();
        request.setSymbol(symbol);
        request.setQuantity(quantity);
        request.setType(TradeType.SELL);
        request.setOrderType(orderType);
        return request;
    }

    /**
     * Create market data for testing
     */
    public MarketData createMarketData(String symbol, BigDecimal price, BigDecimal volume) {
        MarketData marketData = new MarketData();
        marketData.setSymbol(symbol);
        marketData.setPrice(price);
        marketData.setVolume(volume);
        marketData.setHigh(price.multiply(new BigDecimal("1.02")));
        marketData.setLow(price.multiply(new BigDecimal("0.98")));
        marketData.setOpen(price.multiply(new BigDecimal("0.99")));
        marketData.setTimestamp(LocalDateTime.now());
        return marketData;
    }

    /**
     * Update market price for a symbol (simulates price movement)
     */
    public void updateMarketPrice(String symbol, BigDecimal newPrice) {
        // Update positions with new current price
        List<Position> positions = positionRepository.findBySymbol(symbol);
        for (Position position : positions) {
            position.setCurrentPrice(newPrice);
            BigDecimal unrealizedPnL = position.getQuantity()
                .multiply(newPrice.subtract(position.getAveragePrice()));
            position.setUnrealizedPnL(unrealizedPnL);
            position.setLastUpdated(LocalDateTime.now());
            positionRepository.save(position);
        }
    }

    /**
     * Setup trading account with sufficient funds
     */
    public void setupTradingAccount() {
        setupTestPortfolio(DEFAULT_ACCOUNT_ID, new BigDecimal("500000.00"));
    }

    /**
     * Create portfolio with historical performance data
     */
    public Portfolio createPortfolioWithHistory() {
        Portfolio portfolio = setupTestPortfolio();
        
        // Create positions with various P&L scenarios
        createPositionWithTrades("AAPL", new BigDecimal("100"), new BigDecimal("140.00"), new BigDecimal("155.00"));
        createPositionWithTrades("GOOGL", new BigDecimal("25"), new BigDecimal("2100.00"), new BigDecimal("1950.00"));
        createPositionWithTrades("MSFT", new BigDecimal("75"), new BigDecimal("290.00"), new BigDecimal("310.00"));
        createPositionWithTrades("TSLA", new BigDecimal("50"), new BigDecimal("800.00"), new BigDecimal("750.00"));
        
        // Create historical trades
        createHistoricalTrades();
        
        return portfolio;
    }

    /**
     * Create historical trades for performance calculation
     */
    public void createHistoricalTrades() {
        LocalDateTime baseTime = LocalDateTime.now().minusDays(30);
        
        // Create a series of trades over the past month
        for (int i = 0; i < 20; i++) {
            LocalDateTime tradeTime = baseTime.plusDays(i);
            
            Trade trade = new Trade();
            trade.setOrderId(UUID.randomUUID().toString());
            trade.setAccountId(DEFAULT_ACCOUNT_ID);
            trade.setSymbol(i % 2 == 0 ? "AAPL" : "GOOGL");
            trade.setQuantity(new BigDecimal(10 + (i % 5) * 5));
            trade.setPrice(new BigDecimal(150 + i * 2));
            trade.setType(i % 3 == 0 ? TradeType.SELL : TradeType.BUY);
            trade.setExecutedAt(tradeTime);
            trade.setStrategyName("HistoricalTestStrategy");
            
            tradeRepository.save(trade);
        }
    }

    /**
     * Create test data for multiple symbols
     */
    public void setupMultiSymbolPortfolio() {
        setupTestPortfolio();
        
        // Create diversified portfolio
        createPosition("AAPL", new BigDecimal("100"), new BigDecimal("150.00"));
        createPosition("GOOGL", new BigDecimal("50"), new BigDecimal("2000.00"));
        createPosition("MSFT", new BigDecimal("75"), new BigDecimal("300.00"));
        createPosition("TSLA", new BigDecimal("25"), new BigDecimal("800.00"));
        createPosition("AMZN", new BigDecimal("30"), new BigDecimal("3000.00"));
    }

    /**
     * Create test data for performance testing
     */
    public void setupLargePortfolio() {
        setupTestPortfolio(DEFAULT_ACCOUNT_ID, new BigDecimal("1000000.00"));
        
        // Create many positions
        String[] symbols = {"AAPL", "GOOGL", "MSFT", "TSLA", "AMZN", "META", "NVDA", "NFLX", "CRM", "ADBE"};
        for (int i = 0; i < symbols.length; i++) {
            BigDecimal quantity = new BigDecimal(100 + i * 50);
            BigDecimal price = new BigDecimal(100 + i * 100);
            createPosition(symbols[i], quantity, price);
        }
    }

    /**
     * Clean up test data
     */
    public void cleanupTestData() {
        tradeRepository.deleteAll();
        positionRepository.deleteAll();
        portfolioRepository.deleteAll();
    }

    /**
     * Create test data for error scenarios
     */
    public void setupErrorScenarios() {
        // Portfolio with very low cash balance
        setupTestPortfolio(DEFAULT_ACCOUNT_ID, new BigDecimal("100.00"));
        
        // Position with zero quantity (should be filtered out)
        createPosition("ZERO_QTY", BigDecimal.ZERO, new BigDecimal("100.00"));
        
        // Position with negative unrealized P&L
        createPositionWithTrades("LOSING_STOCK", new BigDecimal("100"), 
            new BigDecimal("200.00"), new BigDecimal("150.00"));
    }

    /**
     * Get default account ID for tests
     */
    public String getDefaultAccountId() {
        return DEFAULT_ACCOUNT_ID;
    }

    /**
     * Get default cash balance for tests
     */
    public BigDecimal getDefaultCashBalance() {
        return DEFAULT_CASH_BALANCE;
    }

    /**
     * Create order request with limit price
     */
    public OrderRequest createLimitOrderRequest(String symbol, BigDecimal quantity, 
                                              TradeType tradeType, BigDecimal limitPrice) {
        OrderRequest request = new OrderRequest();
        request.setSymbol(symbol);
        request.setQuantity(quantity);
        request.setType(tradeType);
        request.setOrderType("limit");
        request.setLimitPrice(limitPrice);
        return request;
    }

    /**
     * Create batch of test trades for volume testing
     */
    public List<Trade> createBatchTrades(int count) {
        List<Trade> trades = new ArrayList<>();
        String[] symbols = {"AAPL", "GOOGL", "MSFT", "TSLA"};
        
        for (int i = 0; i < count; i++) {
            String symbol = symbols[i % symbols.length];
            BigDecimal quantity = new BigDecimal(10 + (i % 10));
            BigDecimal price = new BigDecimal(100 + i);
            TradeType type = i % 2 == 0 ? TradeType.BUY : TradeType.SELL;
            
            Trade trade = createTrade(symbol, quantity, price, type);
            trades.add(trade);
        }
        
        return trades;
    }
}