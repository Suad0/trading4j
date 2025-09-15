package com.quanttrading.repository;

import com.quanttrading.model.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {
    
    /**
     * Find all positions for a specific portfolio
     */
    List<Position> findByPortfolioAccountId(String accountId);
    
    /**
     * Find position by portfolio and symbol
     */
    Optional<Position> findByPortfolioAccountIdAndSymbol(String accountId, String symbol);
    
    /**
     * Find all positions with non-zero quantities
     */
    @Query("SELECT p FROM Position p WHERE p.quantity != 0")
    List<Position> findActivePositions();
    
    /**
     * Find active positions for a specific portfolio
     */
    @Query("SELECT p FROM Position p WHERE p.portfolio.accountId = :accountId AND p.quantity != 0")
    List<Position> findActivePositionsByPortfolio(@Param("accountId") String accountId);
    
    /**
     * Calculate total market value for a portfolio
     */
    @Query("SELECT COALESCE(SUM(p.quantity * p.currentPrice), 0) " +
           "FROM Position p WHERE p.portfolio.accountId = :accountId")
    BigDecimal calculateTotalMarketValue(@Param("accountId") String accountId);
    
    /**
     * Calculate total unrealized P&L for a portfolio
     */
    @Query("SELECT COALESCE(SUM(p.unrealizedPnL), 0) " +
           "FROM Position p WHERE p.portfolio.accountId = :accountId")
    BigDecimal calculateTotalUnrealizedPnL(@Param("accountId") String accountId);
    
    /**
     * Find positions by symbol across all portfolios
     */
    List<Position> findBySymbol(String symbol);
    
    /**
     * Find positions that need price updates (current price is null or old)
     */
    @Query("SELECT p FROM Position p WHERE p.currentPrice IS NULL OR p.lastUpdated < :cutoffTime")
    List<Position> findPositionsNeedingPriceUpdate(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * Get top performing positions by unrealized P&L
     */
    @Query("SELECT p FROM Position p WHERE p.portfolio.accountId = :accountId " +
           "ORDER BY p.unrealizedPnL DESC")
    List<Position> findTopPerformingPositions(@Param("accountId") String accountId);
    
    /**
     * Get worst performing positions by unrealized P&L
     */
    @Query("SELECT p FROM Position p WHERE p.portfolio.accountId = :accountId " +
           "ORDER BY p.unrealizedPnL ASC")
    List<Position> findWorstPerformingPositions(@Param("accountId") String accountId);
    
    /**
     * Count positions for a portfolio
     */
    long countByPortfolioAccountId(String accountId);
    
    /**
     * Delete positions with zero quantity
     */
    void deleteByQuantity(BigDecimal quantity);
}