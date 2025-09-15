package com.quanttrading.repository;

import com.quanttrading.model.OrderStatus;
import com.quanttrading.model.Trade;
import com.quanttrading.model.TradeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TradeRepository extends JpaRepository<Trade, String> {
    
    /**
     * Find trade by order ID
     */
    Optional<Trade> findByOrderId(String orderId);
    
    /**
     * Find all trades for a specific symbol
     */
    List<Trade> findBySymbol(String symbol);
    
    /**
     * Find trades by symbol with pagination
     */
    Page<Trade> findBySymbol(String symbol, Pageable pageable);
    
    /**
     * Find trades by status
     */
    List<Trade> findByStatus(OrderStatus status);
    
    /**
     * Find trades by type (BUY/SELL)
     */
    List<Trade> findByType(TradeType type);
    
    /**
     * Find trades by strategy name
     */
    List<Trade> findByStrategyName(String strategyName);
    
    /**
     * Find trades within a date range
     */
    @Query("SELECT t FROM Trade t WHERE t.executedAt BETWEEN :startDate AND :endDate")
    List<Trade> findTradesByDateRange(@Param("startDate") LocalDateTime startDate, 
                                     @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find trades within a date range with pagination
     */
    @Query("SELECT t FROM Trade t WHERE t.executedAt BETWEEN :startDate AND :endDate")
    Page<Trade> findTradesByDateRange(@Param("startDate") LocalDateTime startDate, 
                                     @Param("endDate") LocalDateTime endDate, 
                                     Pageable pageable);
    
    /**
     * Find executed trades (filled status) ordered by execution time
     */
    @Query("SELECT t FROM Trade t WHERE t.status = 'FILLED' ORDER BY t.executedAt DESC")
    List<Trade> findExecutedTradesOrderByExecutionTime();
    
    /**
     * Find recent trades (last N days)
     */
    @Query("SELECT t FROM Trade t WHERE t.executedAt >= :cutoffDate ORDER BY t.executedAt DESC")
    List<Trade> findRecentTrades(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Calculate total trade volume for a symbol
     */
    @Query("SELECT COALESCE(SUM(t.quantity * t.price), 0) " +
           "FROM Trade t WHERE t.symbol = :symbol AND t.status = 'FILLED'")
    BigDecimal calculateTotalVolumeBySymbol(@Param("symbol") String symbol);
    
    /**
     * Calculate total trade volume for a date range
     */
    @Query("SELECT COALESCE(SUM(t.quantity * t.price), 0) " +
           "FROM Trade t WHERE t.executedAt BETWEEN :startDate AND :endDate AND t.status = 'FILLED'")
    BigDecimal calculateTotalVolumeByDateRange(@Param("startDate") LocalDateTime startDate, 
                                              @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get trade statistics by strategy
     */
    @Query("SELECT t.strategyName, COUNT(t), AVG(t.quantity * t.price), SUM(t.quantity * t.price) " +
           "FROM Trade t WHERE t.status = 'FILLED' AND t.strategyName IS NOT NULL " +
           "GROUP BY t.strategyName")
    List<Object[]> getTradeStatsByStrategy();
    
    /**
     * Get daily trade summary
     */
    @Query("SELECT DATE(t.executedAt), COUNT(t), SUM(t.quantity * t.price) " +
           "FROM Trade t WHERE t.status = 'FILLED' AND t.executedAt >= :startDate " +
           "GROUP BY DATE(t.executedAt) ORDER BY DATE(t.executedAt)")
    List<Object[]> getDailyTradeSummary(@Param("startDate") LocalDateTime startDate);
    
    /**
     * Find pending trades (not filled or cancelled)
     */
    @Query("SELECT t FROM Trade t WHERE t.status NOT IN ('FILLED', 'CANCELLED', 'REJECTED')")
    List<Trade> findPendingTrades();
    
    /**
     * Count trades by status
     */
    long countByStatus(OrderStatus status);
    
    /**
     * Count trades by symbol and date range
     */
    @Query("SELECT COUNT(t) FROM Trade t WHERE t.symbol = :symbol " +
           "AND t.executedAt BETWEEN :startDate AND :endDate")
    long countTradesBySymbolAndDateRange(@Param("symbol") String symbol,
                                        @Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find trades ordered by creation time (most recent first)
     */
    List<Trade> findAllByOrderByCreatedAtDesc();
    
    /**
     * Find trades ordered by execution time (most recent first)
     */
    List<Trade> findAllByOrderByExecutedAtDesc();
    
    /**
     * Find trades by account ID ordered by execution time
     */
    List<Trade> findByAccountIdOrderByExecutedAtDesc(String accountId);
    
    /**
     * Find trades by symbol ordered by execution time
     */
    List<Trade> findBySymbolOrderByExecutedAtDesc(String symbol);
    
    /**
     * Find top N trades ordered by execution time
     */
    @Query(value = "SELECT * FROM trades ORDER BY executed_at DESC LIMIT :limit", nativeQuery = true)
    List<Trade> findTopNByOrderByExecutedAtDesc(@Param("limit") int limit);
}