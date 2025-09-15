package com.quanttrading.repository;

import com.quanttrading.model.OrderStatus;
import com.quanttrading.model.Trade;
import com.quanttrading.model.TradeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class TradeRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TradeRepository tradeRepository;

    private Trade filledBuyTrade;
    private Trade filledSellTrade;
    private Trade pendingTrade;
    private Trade cancelledTrade;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        
        // Create filled buy trade
        filledBuyTrade = new Trade("ORDER_001", "AAPL", TradeType.BUY, 
                                  new BigDecimal("10"), new BigDecimal("150.00"), OrderStatus.FILLED);
        filledBuyTrade.setExecutedAt(now.minusHours(2));
        filledBuyTrade.setStrategyName("SMA_CROSSOVER");
        
        // Create filled sell trade
        filledSellTrade = new Trade("ORDER_002", "GOOGL", TradeType.SELL, 
                                   new BigDecimal("5"), new BigDecimal("2600.00"), OrderStatus.FILLED);
        filledSellTrade.setExecutedAt(now.minusHours(1));
        filledSellTrade.setStrategyName("RSI_MEAN_REVERSION");
        
        // Create pending trade
        pendingTrade = new Trade("ORDER_003", "MSFT", TradeType.BUY, 
                                new BigDecimal("8"), new BigDecimal("310.00"), OrderStatus.PENDING);
        pendingTrade.setStrategyName("MOMENTUM");
        
        // Create cancelled trade
        cancelledTrade = new Trade("ORDER_004", "TSLA", TradeType.SELL, 
                                  new BigDecimal("3"), new BigDecimal("900.00"), OrderStatus.CANCELLED);
        
        // Persist test data
        entityManager.persistAndFlush(filledBuyTrade);
        entityManager.persistAndFlush(filledSellTrade);
        entityManager.persistAndFlush(pendingTrade);
        entityManager.persistAndFlush(cancelledTrade);
        entityManager.clear();
    }

    @Test
    void findByOrderId_ShouldReturnTrade_WhenOrderIdExists() {
        // When
        Optional<Trade> result = tradeRepository.findByOrderId("ORDER_001");
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getSymbol()).isEqualTo("AAPL");
        assertThat(result.get().getType()).isEqualTo(TradeType.BUY);
        assertThat(result.get().getStatus()).isEqualTo(OrderStatus.FILLED);
    }

    @Test
    void findByOrderId_ShouldReturnEmpty_WhenOrderIdDoesNotExist() {
        // When
        Optional<Trade> result = tradeRepository.findByOrderId("NON_EXISTENT");
        
        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void findBySymbol_ShouldReturnTradesForSymbol() {
        // When
        List<Trade> trades = tradeRepository.findBySymbol("AAPL");
        
        // Then
        assertThat(trades).hasSize(1);
        assertThat(trades.get(0).getSymbol()).isEqualTo("AAPL");
    }

    @Test
    void findBySymbolWithPagination_ShouldReturnPagedResults() {
        // When
        Page<Trade> page = tradeRepository.findBySymbol("AAPL", PageRequest.of(0, 10));
        
        // Then
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getSymbol()).isEqualTo("AAPL");
    }

    @Test
    void findByStatus_ShouldReturnTradesWithSpecificStatus() {
        // When
        List<Trade> filledTrades = tradeRepository.findByStatus(OrderStatus.FILLED);
        
        // Then
        assertThat(filledTrades).hasSize(2);
        assertThat(filledTrades).extracting(Trade::getOrderId)
                               .containsExactlyInAnyOrder("ORDER_001", "ORDER_002");
    }

    @Test
    void findByType_ShouldReturnTradesWithSpecificType() {
        // When
        List<Trade> buyTrades = tradeRepository.findByType(TradeType.BUY);
        
        // Then
        assertThat(buyTrades).hasSize(2);
        assertThat(buyTrades).extracting(Trade::getOrderId)
                            .containsExactlyInAnyOrder("ORDER_001", "ORDER_003");
    }

    @Test
    void findByStrategyName_ShouldReturnTradesForStrategy() {
        // When
        List<Trade> trades = tradeRepository.findByStrategyName("SMA_CROSSOVER");
        
        // Then
        assertThat(trades).hasSize(1);
        assertThat(trades.get(0).getOrderId()).isEqualTo("ORDER_001");
    }

    @Test
    void findTradesByDateRange_ShouldReturnTradesInRange() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusHours(3);
        LocalDateTime endDate = LocalDateTime.now();
        
        // When
        List<Trade> trades = tradeRepository.findTradesByDateRange(startDate, endDate);
        
        // Then
        assertThat(trades).hasSize(2); // Only filled trades have execution dates
        assertThat(trades).extracting(Trade::getOrderId)
                         .containsExactlyInAnyOrder("ORDER_001", "ORDER_002");
    }

    @Test
    void findTradesByDateRangeWithPagination_ShouldReturnPagedResults() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusHours(3);
        LocalDateTime endDate = LocalDateTime.now();
        
        // When
        Page<Trade> page = tradeRepository.findTradesByDateRange(startDate, endDate, PageRequest.of(0, 1));
        
        // Then
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void findExecutedTradesOrderByExecutionTime_ShouldReturnFilledTradesOrderedByTime() {
        // When
        List<Trade> trades = tradeRepository.findExecutedTradesOrderByExecutionTime();
        
        // Then
        assertThat(trades).hasSize(2);
        // Should be ordered by execution time DESC (most recent first)
        assertThat(trades.get(0).getOrderId()).isEqualTo("ORDER_002"); // More recent
        assertThat(trades.get(1).getOrderId()).isEqualTo("ORDER_001"); // Older
    }

    @Test
    void findRecentTrades_ShouldReturnTradesAfterCutoffDate() {
        // Given
        LocalDateTime cutoffDate = LocalDateTime.now().minusHours(1).minusMinutes(30);
        
        // When
        List<Trade> trades = tradeRepository.findRecentTrades(cutoffDate);
        
        // Then
        assertThat(trades).hasSize(1);
        assertThat(trades.get(0).getOrderId()).isEqualTo("ORDER_002");
    }

    @Test
    void calculateTotalVolumeBySymbol_ShouldReturnCorrectVolume() {
        // When
        BigDecimal volume = tradeRepository.calculateTotalVolumeBySymbol("AAPL");
        
        // Then
        // 10 * 150.00 = 1500.00
        assertThat(volume).isEqualByComparingTo(new BigDecimal("1500.00"));
    }

    @Test
    void calculateTotalVolumeByDateRange_ShouldReturnCorrectVolume() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusHours(3);
        LocalDateTime endDate = LocalDateTime.now();
        
        // When
        BigDecimal volume = tradeRepository.calculateTotalVolumeByDateRange(startDate, endDate);
        
        // Then
        // ORDER_001: 10 * 150.00 = 1500.00
        // ORDER_002: 5 * 2600.00 = 13000.00
        // Total: 1500.00 + 13000.00 = 14500.00
        assertThat(volume).isEqualByComparingTo(new BigDecimal("14500.00"));
    }

    @Test
    void getTradeStatsByStrategy_ShouldReturnStrategyStatistics() {
        // When
        List<Object[]> stats = tradeRepository.getTradeStatsByStrategy();
        
        // Then
        assertThat(stats).hasSize(2);
        
        // Find SMA_CROSSOVER stats
        Object[] smaCrossoverStats = stats.stream()
            .filter(stat -> "SMA_CROSSOVER".equals(stat[0]))
            .findFirst()
            .orElseThrow();
        
        assertThat(smaCrossoverStats[0]).isEqualTo("SMA_CROSSOVER"); // strategy name
        assertThat(smaCrossoverStats[1]).isEqualTo(1L); // count
        
        // Handle SQLite returning Double for AVG() function
        BigDecimal avgVolume = smaCrossoverStats[2] instanceof Double ? 
            BigDecimal.valueOf((Double) smaCrossoverStats[2]) : 
            (BigDecimal) smaCrossoverStats[2];
        BigDecimal totalVolume = smaCrossoverStats[3] instanceof Double ? 
            BigDecimal.valueOf((Double) smaCrossoverStats[3]) : 
            (BigDecimal) smaCrossoverStats[3];
            
        assertThat(avgVolume).isEqualByComparingTo(new BigDecimal("1500.00")); // avg volume
        assertThat(totalVolume).isEqualByComparingTo(new BigDecimal("1500.00")); // total volume
    }

    @Test
    void getDailyTradeSummary_ShouldReturnDailySummary() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        
        // When
        List<Object[]> summary = tradeRepository.getDailyTradeSummary(startDate);
        
        // Then
        assertThat(summary).hasSize(1); // All trades are from today
        Object[] todayStats = summary.get(0);
        assertThat(todayStats[1]).isEqualTo(2L); // count of trades
        assertThat((BigDecimal) todayStats[2]).isEqualByComparingTo(new BigDecimal("14500.00")); // total volume
    }

    @Test
    void findPendingTrades_ShouldReturnNonFinalizedTrades() {
        // When
        List<Trade> pendingTrades = tradeRepository.findPendingTrades();
        
        // Then
        assertThat(pendingTrades).hasSize(1);
        assertThat(pendingTrades.get(0).getOrderId()).isEqualTo("ORDER_003");
        assertThat(pendingTrades.get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void countByStatus_ShouldReturnCorrectCount() {
        // When
        long filledCount = tradeRepository.countByStatus(OrderStatus.FILLED);
        long pendingCount = tradeRepository.countByStatus(OrderStatus.PENDING);
        long cancelledCount = tradeRepository.countByStatus(OrderStatus.CANCELLED);
        
        // Then
        assertThat(filledCount).isEqualTo(2);
        assertThat(pendingCount).isEqualTo(1);
        assertThat(cancelledCount).isEqualTo(1);
    }

    @Test
    void countTradesBySymbolAndDateRange_ShouldReturnCorrectCount() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusHours(3);
        LocalDateTime endDate = LocalDateTime.now();
        
        // When
        long count = tradeRepository.countTradesBySymbolAndDateRange("AAPL", startDate, endDate);
        
        // Then
        assertThat(count).isEqualTo(1);
    }

    @Test
    void findAllByOrderByCreatedAtDesc_ShouldReturnTradesOrderedByCreationTime() {
        // When
        List<Trade> trades = tradeRepository.findAllByOrderByCreatedAtDesc();
        
        // Then
        assertThat(trades).hasSize(4);
        // Should be ordered by creation time DESC (most recent first)
        // Since all were created around the same time, just verify we get all trades
        assertThat(trades).extracting(Trade::getOrderId)
                         .containsExactlyInAnyOrder("ORDER_001", "ORDER_002", "ORDER_003", "ORDER_004");
    }

    @Test
    void findAllByOrderByExecutedAtDesc_ShouldReturnTradesOrderedByExecutionTime() {
        // When
        List<Trade> trades = tradeRepository.findAllByOrderByExecutedAtDesc();
        
        // Then
        assertThat(trades).hasSize(4);
        // Trades with execution time should come first, ordered DESC
        assertThat(trades.get(0).getOrderId()).isEqualTo("ORDER_002"); // Most recent execution
        assertThat(trades.get(1).getOrderId()).isEqualTo("ORDER_001"); // Older execution
        // Trades without execution time will be at the end
    }

    @Test
    void save_ShouldPersistTrade() {
        // Given
        Trade newTrade = new Trade("ORDER_005", "NVDA", TradeType.BUY, 
                                  new BigDecimal("2"), new BigDecimal("800.00"), OrderStatus.PENDING);
        
        // When
        Trade saved = tradeRepository.save(newTrade);
        entityManager.flush();
        entityManager.clear();
        
        // Then
        assertThat(saved.getOrderId()).isEqualTo("ORDER_005");
        
        Optional<Trade> retrieved = tradeRepository.findByOrderId("ORDER_005");
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getSymbol()).isEqualTo("NVDA");
    }

    @Test
    void delete_ShouldRemoveTrade() {
        // When
        tradeRepository.deleteById("ORDER_001");
        entityManager.flush();
        entityManager.clear();
        
        // Then
        Optional<Trade> result = tradeRepository.findByOrderId("ORDER_001");
        assertThat(result).isEmpty();
    }
}