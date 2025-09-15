package com.quanttrading.repository;

import com.quanttrading.model.Portfolio;
import com.quanttrading.model.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class PortfolioRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PortfolioRepository portfolioRepository;

    private Portfolio testPortfolio;
    private Position testPosition1;
    private Position testPosition2;

    @BeforeEach
    void setUp() {
        // Create test portfolio
        testPortfolio = new Portfolio("TEST_ACCOUNT_001", 
                                    new BigDecimal("10000.00"), 
                                    new BigDecimal("15000.00"));
        
        // Create test positions
        testPosition1 = new Position("AAPL", new BigDecimal("10"), new BigDecimal("150.00"));
        testPosition1.setCurrentPrice(new BigDecimal("155.00"));
        testPosition1.setPortfolio(testPortfolio);
        
        testPosition2 = new Position("GOOGL", new BigDecimal("5"), new BigDecimal("2500.00"));
        testPosition2.setCurrentPrice(new BigDecimal("2600.00"));
        testPosition2.setPortfolio(testPortfolio);
        
        testPortfolio.addPosition(testPosition1);
        testPortfolio.addPosition(testPosition2);
        
        // Persist test data
        entityManager.persistAndFlush(testPortfolio);
        entityManager.clear();
    }

    @Test
    void findByAccountId_ShouldReturnPortfolio_WhenAccountIdExists() {
        // When
        Optional<Portfolio> result = portfolioRepository.findByAccountId("TEST_ACCOUNT_001");
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getAccountId()).isEqualTo("TEST_ACCOUNT_001");
        assertThat(result.get().getCashBalance()).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(result.get().getPositions()).hasSize(2);
    }

    @Test
    void findByAccountId_ShouldReturnEmpty_WhenAccountIdDoesNotExist() {
        // When
        Optional<Portfolio> result = portfolioRepository.findByAccountId("NON_EXISTENT");
        
        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void calculateTotalPortfolioValue_ShouldReturnCorrectValue() {
        // When
        Optional<BigDecimal> result = portfolioRepository.calculateTotalPortfolioValue("TEST_ACCOUNT_001");
        
        // Then
        assertThat(result).isPresent();
        // Cash: 10000 + Positions: (10 * 155) + (5 * 2600) = 10000 + 1550 + 13000 = 24550
        assertThat(result.get()).isEqualByComparingTo(new BigDecimal("24550.00"));
    }

    @Test
    void calculateTotalUnrealizedPnL_ShouldReturnCorrectValue() {
        // When
        BigDecimal result = portfolioRepository.calculateTotalUnrealizedPnL("TEST_ACCOUNT_001");
        
        // Then
        // Position 1: (155 - 150) * 10 = 50
        // Position 2: (2600 - 2500) * 5 = 500
        // Total: 50 + 500 = 550
        assertThat(result).isEqualByComparingTo(new BigDecimal("550.00"));
    }

    @Test
    void getPortfolioMetrics_ShouldReturnCorrectMetrics() {
        // When
        List<Object[]> results = portfolioRepository.getPortfolioMetrics("TEST_ACCOUNT_001");
        
        // Then
        assertThat(results).isNotNull();
        assertThat(results).hasSize(1);
        
        Object[] result = results.get(0);
        assertThat(result).hasSize(3);
        
        BigDecimal cashBalance = (BigDecimal) result[0];
        BigDecimal positionsValue = (BigDecimal) result[1];
        BigDecimal totalUnrealizedPnL = (BigDecimal) result[2];
        
        assertThat(cashBalance).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(positionsValue).isEqualByComparingTo(new BigDecimal("14550.00")); // (10 * 155) + (5 * 2600)
        assertThat(totalUnrealizedPnL).isEqualByComparingTo(new BigDecimal("550.00"));
    }

    @Test
    void existsByAccountIdAndLastUpdatedAfter_ShouldReturnTrue_WhenPortfolioUpdatedAfterTimestamp() {
        // Given
        LocalDateTime pastTime = LocalDateTime.now().minusHours(1);
        
        // When
        boolean result = portfolioRepository.existsByAccountIdAndLastUpdatedAfter("TEST_ACCOUNT_001", pastTime);
        
        // Then
        assertThat(result).isTrue();
    }

    @Test
    void existsByAccountIdAndLastUpdatedAfter_ShouldReturnFalse_WhenPortfolioUpdatedBeforeTimestamp() {
        // Given
        LocalDateTime futureTime = LocalDateTime.now().plusHours(1);
        
        // When
        boolean result = portfolioRepository.existsByAccountIdAndLastUpdatedAfter("TEST_ACCOUNT_001", futureTime);
        
        // Then
        assertThat(result).isFalse();
    }

    @Test
    void save_ShouldPersistPortfolio() {
        // Given
        Portfolio newPortfolio = new Portfolio("TEST_ACCOUNT_002", 
                                             new BigDecimal("5000.00"), 
                                             new BigDecimal("5000.00"));
        
        // When
        Portfolio saved = portfolioRepository.save(newPortfolio);
        entityManager.flush();
        entityManager.clear();
        
        // Then
        assertThat(saved.getAccountId()).isEqualTo("TEST_ACCOUNT_002");
        
        Optional<Portfolio> retrieved = portfolioRepository.findByAccountId("TEST_ACCOUNT_002");
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getCashBalance()).isEqualByComparingTo(new BigDecimal("5000.00"));
    }

    @Test
    void delete_ShouldRemovePortfolio() {
        // When
        portfolioRepository.deleteById("TEST_ACCOUNT_001");
        entityManager.flush();
        entityManager.clear();
        
        // Then
        Optional<Portfolio> result = portfolioRepository.findByAccountId("TEST_ACCOUNT_001");
        assertThat(result).isEmpty();
    }
}