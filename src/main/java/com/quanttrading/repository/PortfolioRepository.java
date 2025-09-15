package com.quanttrading.repository;

import com.quanttrading.model.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, String> {
    
    /**
     * Find portfolio by account ID
     */
    Optional<Portfolio> findByAccountId(String accountId);
    
    /**
     * Calculate total portfolio value including positions
     */
    @Query("SELECT p.cashBalance + COALESCE(SUM(pos.quantity * pos.currentPrice), 0) " +
           "FROM Portfolio p LEFT JOIN p.positions pos " +
           "WHERE p.accountId = :accountId " +
           "GROUP BY p.accountId, p.cashBalance")
    Optional<BigDecimal> calculateTotalPortfolioValue(@Param("accountId") String accountId);
    
    /**
     * Calculate total unrealized P&L for a portfolio
     */
    @Query("SELECT COALESCE(SUM(pos.unrealizedPnL), 0) " +
           "FROM Portfolio p LEFT JOIN p.positions pos " +
           "WHERE p.accountId = :accountId")
    BigDecimal calculateTotalUnrealizedPnL(@Param("accountId") String accountId);
    
    /**
     * Get portfolio performance metrics
     */
    @Query("SELECT p.cashBalance, " +
           "COALESCE(SUM(pos.quantity * pos.currentPrice), 0) as positionsValue, " +
           "COALESCE(SUM(pos.unrealizedPnL), 0) as totalUnrealizedPnL " +
           "FROM Portfolio p LEFT JOIN p.positions pos " +
           "WHERE p.accountId = :accountId " +
           "GROUP BY p.accountId, p.cashBalance")
    List<Object[]> getPortfolioMetrics(@Param("accountId") String accountId);
    
    /**
     * Check if portfolio was updated after a specific time
     */
    boolean existsByAccountIdAndLastUpdatedAfter(String accountId, LocalDateTime timestamp);
}