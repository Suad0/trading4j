package com.quanttrading.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import jakarta.persistence.PersistenceContext;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@SpringJUnitConfig
@Transactional
class EntityRelationshipTest {

    @PersistenceContext
    private EntityManager entityManager;

    private Portfolio portfolio;
    private Position position1;
    private Position position2;
    private Trade trade1;
    private Trade trade2;
    private MarketData marketData1;
    private MarketData marketData2;

    @BeforeEach
    void setUp() {
        // Create portfolio
        portfolio = new Portfolio("TEST_ACCOUNT", 
                                 new BigDecimal("10000.00"), 
                                 new BigDecimal("15000.00"));

        // Create positions
        position1 = new Position("AAPL", new BigDecimal("10"), new BigDecimal("150.00"));
        position1.setCurrentPrice(new BigDecimal("155.00"));
        
        position2 = new Position("GOOGL", new BigDecimal("5"), new BigDecimal("2500.00"));
        position2.setCurrentPrice(new BigDecimal("2550.00"));

        // Create trades
        trade1 = new Trade("ORDER_001", "AAPL", TradeType.BUY, 
                          new BigDecimal("10"), new BigDecimal("150.00"), OrderStatus.FILLED);
        trade1.setExecutedAt(LocalDateTime.now().minusDays(1));
        trade1.setStrategyName("MANUAL");

        trade2 = new Trade("ORDER_002", "GOOGL", TradeType.BUY, 
                          new BigDecimal("5"), new BigDecimal("2500.00"), OrderStatus.FILLED);
        trade2.setExecutedAt(LocalDateTime.now().minusHours(2));
        trade2.setStrategyName("SMA_CROSSOVER");

        // Create market data
        marketData1 = new MarketData("AAPL", 
                                    new BigDecimal("155.00"), 
                                    new BigDecimal("1000000"), 
                                    new BigDecimal("157.00"), 
                                    new BigDecimal("153.00"), 
                                    new BigDecimal("154.00"), 
                                    LocalDateTime.now().minusMinutes(5));

        marketData2 = new MarketData("GOOGL", 
                                    new BigDecimal("2550.00"), 
                                    new BigDecimal("500000"), 
                                    new BigDecimal("2560.00"), 
                                    new BigDecimal("2540.00"), 
                                    new BigDecimal("2545.00"), 
                                    LocalDateTime.now().minusMinutes(3));
    }

    @Test
    void testPortfolioPositionRelationship() {
        // Establish relationships
        portfolio.addPosition(position1);
        portfolio.addPosition(position2);

        // Persist entities
        entityManager.persist(portfolio);
        entityManager.flush();

        // Clear persistence context to ensure fresh load
        entityManager.clear();

        // Verify relationship from portfolio side
        Portfolio foundPortfolio = entityManager.find(Portfolio.class, "TEST_ACCOUNT");
        assertNotNull(foundPortfolio);
        assertEquals(2, foundPortfolio.getPositions().size());

        // Verify relationship from position side
        List<Position> positions = entityManager.createQuery(
                "SELECT p FROM Position p WHERE p.portfolio.accountId = :accountId", Position.class)
                .setParameter("accountId", "TEST_ACCOUNT")
                .getResultList();

        assertEquals(2, positions.size());
        positions.forEach(pos -> {
            assertNotNull(pos.getPortfolio());
            assertEquals("TEST_ACCOUNT", pos.getPortfolio().getAccountId());
        });
    }

    @Test
    void testCascadeOperations() {
        // Add positions to portfolio
        portfolio.addPosition(position1);
        portfolio.addPosition(position2);

        // Persist only portfolio - positions should be cascaded
        entityManager.persist(portfolio);
        entityManager.flush();
        entityManager.clear();

        // Verify cascade persist worked
        Portfolio foundPortfolio = entityManager.find(Portfolio.class, "TEST_ACCOUNT");
        assertEquals(2, foundPortfolio.getPositions().size());

        // Test cascade delete
        entityManager.remove(foundPortfolio);
        entityManager.flush();
        entityManager.clear();

        // Verify positions were also deleted
        List<Position> remainingPositions = entityManager.createQuery(
                "SELECT p FROM Position p WHERE p.portfolio.accountId = :accountId", Position.class)
                .setParameter("accountId", "TEST_ACCOUNT")
                .getResultList();

        assertTrue(remainingPositions.isEmpty());
    }

    @Test
    void testPositionUnrealizedPnLCalculation() {
        portfolio.addPosition(position1);
        entityManager.persist(portfolio);
        entityManager.flush();

        // Verify unrealized P&L calculation
        // (155.00 - 150.00) * 10 = 50.00
        assertEquals(new BigDecimal("50.00"), position1.getUnrealizedPnL());

        // Update current price and verify recalculation
        position1.setCurrentPrice(new BigDecimal("160.00"));
        entityManager.merge(position1);
        entityManager.flush();

        // (160.00 - 150.00) * 10 = 100.00
        assertEquals(new BigDecimal("100.00"), position1.getUnrealizedPnL());
    }

    @Test
    void testTradeIndependentPersistence() {
        // Trades should persist independently without relationships
        entityManager.persist(trade1);
        entityManager.persist(trade2);
        entityManager.flush();
        entityManager.clear();

        // Verify trades were persisted
        Trade foundTrade1 = entityManager.find(Trade.class, "ORDER_001");
        Trade foundTrade2 = entityManager.find(Trade.class, "ORDER_002");

        assertNotNull(foundTrade1);
        assertNotNull(foundTrade2);
        assertEquals("AAPL", foundTrade1.getSymbol());
        assertEquals("GOOGL", foundTrade2.getSymbol());
        assertEquals("MANUAL", foundTrade1.getStrategyName());
        assertEquals("SMA_CROSSOVER", foundTrade2.getStrategyName());
    }

    @Test
    void testMarketDataIndependentPersistence() {
        // Market data should persist independently
        entityManager.persist(marketData1);
        entityManager.persist(marketData2);
        entityManager.flush();
        entityManager.clear();

        // Verify market data was persisted
        List<MarketData> appleData = entityManager.createQuery(
                "SELECT m FROM MarketData m WHERE m.symbol = :symbol ORDER BY m.timestamp DESC", MarketData.class)
                .setParameter("symbol", "AAPL")
                .getResultList();

        List<MarketData> googleData = entityManager.createQuery(
                "SELECT m FROM MarketData m WHERE m.symbol = :symbol ORDER BY m.timestamp DESC", MarketData.class)
                .setParameter("symbol", "GOOGL")
                .getResultList();

        assertEquals(1, appleData.size());
        assertEquals(1, googleData.size());
        assertEquals(new BigDecimal("155.00"), appleData.get(0).getPrice());
        assertEquals(new BigDecimal("2550.00"), googleData.get(0).getPrice());
    }

    @Test
    void testCompleteWorkflow() {
        // Simulate a complete trading workflow
        
        // 1. Create portfolio
        entityManager.persist(portfolio);
        
        // 2. Execute trades
        entityManager.persist(trade1);
        entityManager.persist(trade2);
        
        // 3. Update portfolio with positions based on trades
        portfolio.addPosition(position1);
        portfolio.addPosition(position2);
        
        // 4. Store market data
        entityManager.persist(marketData1);
        entityManager.persist(marketData2);
        
        entityManager.flush();
        entityManager.clear();

        // Verify complete state
        Portfolio foundPortfolio = entityManager.find(Portfolio.class, "TEST_ACCOUNT");
        assertNotNull(foundPortfolio);
        assertEquals(2, foundPortfolio.getPositions().size());

        List<Trade> allTrades = entityManager.createQuery(
                "SELECT t FROM Trade t ORDER BY t.createdAt", Trade.class)
                .getResultList();
        assertEquals(2, allTrades.size());

        List<MarketData> allMarketData = entityManager.createQuery(
                "SELECT m FROM MarketData m ORDER BY m.timestamp DESC", MarketData.class)
                .getResultList();
        assertEquals(2, allMarketData.size());

        // Verify portfolio calculations
        BigDecimal totalUnrealizedPnL = foundPortfolio.getPositions().stream()
                .map(Position::getUnrealizedPnL)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // AAPL: (155-150)*10 = 50, GOOGL: (2550-2500)*5 = 250, Total = 300
        assertEquals(new BigDecimal("300.00"), totalUnrealizedPnL);
    }

    @Test
    void testLazyLoadingBehavior() {
        portfolio.addPosition(position1);
        entityManager.persist(portfolio);
        entityManager.flush();
        entityManager.clear();

        // Load portfolio without initializing positions
        Portfolio foundPortfolio = entityManager.find(Portfolio.class, "TEST_ACCOUNT");
        assertNotNull(foundPortfolio);

        // Positions should be lazily loaded
        assertEquals(1, foundPortfolio.getPositions().size());
        Position foundPosition = foundPortfolio.getPositions().get(0);
        assertEquals("AAPL", foundPosition.getSymbol());
    }

    @Test
    void testQueryPerformanceWithIndexes() {
        // Create multiple market data entries for testing indexes
        for (int i = 0; i < 10; i++) {
            MarketData data = new MarketData("TEST" + i, 
                                           new BigDecimal("100.00"), 
                                           new BigDecimal("1000"), 
                                           new BigDecimal("101.00"), 
                                           new BigDecimal("99.00"), 
                                           new BigDecimal("100.50"), 
                                           LocalDateTime.now().minusMinutes(i));
            entityManager.persist(data);
        }
        entityManager.flush();

        // Test symbol-based query (should use index)
        List<MarketData> symbolResults = entityManager.createQuery(
                "SELECT m FROM MarketData m WHERE m.symbol = :symbol", MarketData.class)
                .setParameter("symbol", "TEST5")
                .getResultList();

        assertEquals(1, symbolResults.size());

        // Test timestamp-based query (should use index)
        List<MarketData> timeResults = entityManager.createQuery(
                "SELECT m FROM MarketData m WHERE m.timestamp > :time ORDER BY m.timestamp DESC", MarketData.class)
                .setParameter("time", LocalDateTime.now().minusMinutes(5))
                .getResultList();

        assertEquals(5, timeResults.size());
    }
}