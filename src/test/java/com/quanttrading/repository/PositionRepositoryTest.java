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
class PositionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PositionRepository positionRepository;

    private Portfolio testPortfolio;
    private Position activePosition1;
    private Position activePosition2;
    private Position zeroPosition;

    @BeforeEach
    void setUp() {
        // Create test portfolio
        testPortfolio = new Portfolio("TEST_ACCOUNT_001", 
                                    new BigDecimal("10000.00"), 
                                    new BigDecimal("15000.00"));
        
        // Create active positions
        activePosition1 = new Position("AAPL", new BigDecimal("10"), new BigDecimal("150.00"));
        activePosition1.setCurrentPrice(new BigDecimal("155.00"));
        activePosition1.setPortfolio(testPortfolio);
        
        activePosition2 = new Position("GOOGL", new BigDecimal("5"), new BigDecimal("2500.00"));
        activePosition2.setCurrentPrice(new BigDecimal("2600.00"));
        activePosition2.setPortfolio(testPortfolio);
        
        // Create zero quantity position
        zeroPosition = new Position("MSFT", BigDecimal.ZERO, new BigDecimal("300.00"));
        zeroPosition.setCurrentPrice(new BigDecimal("310.00"));
        zeroPosition.setPortfolio(testPortfolio);
        
        testPortfolio.addPosition(activePosition1);
        testPortfolio.addPosition(activePosition2);
        testPortfolio.addPosition(zeroPosition);
        
        // Persist test data
        entityManager.persistAndFlush(testPortfolio);
        entityManager.clear();
    }

    @Test
    void findByPortfolioAccountId_ShouldReturnAllPositions() {
        // When
        List<Position> positions = positionRepository.findByPortfolioAccountId("TEST_ACCOUNT_001");
        
        // Then
        assertThat(positions).hasSize(3);
        assertThat(positions).extracting(Position::getSymbol)
                            .containsExactlyInAnyOrder("AAPL", "GOOGL", "MSFT");
    }

    @Test
    void findByPortfolioAccountIdAndSymbol_ShouldReturnPosition_WhenExists() {
        // When
        Optional<Position> result = positionRepository.findByPortfolioAccountIdAndSymbol("TEST_ACCOUNT_001", "AAPL");
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getSymbol()).isEqualTo("AAPL");
        assertThat(result.get().getQuantity()).isEqualByComparingTo(new BigDecimal("10"));
    }

    @Test
    void findByPortfolioAccountIdAndSymbol_ShouldReturnEmpty_WhenNotExists() {
        // When
        Optional<Position> result = positionRepository.findByPortfolioAccountIdAndSymbol("TEST_ACCOUNT_001", "TSLA");
        
        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void findActivePositions_ShouldReturnOnlyNonZeroPositions() {
        // When
        List<Position> activePositions = positionRepository.findActivePositions();
        
        // Then
        assertThat(activePositions).hasSize(2);
        assertThat(activePositions).extracting(Position::getSymbol)
                                  .containsExactlyInAnyOrder("AAPL", "GOOGL");
        assertThat(activePositions).allMatch(pos -> pos.getQuantity().compareTo(BigDecimal.ZERO) != 0);
    }

    @Test
    void findActivePositionsByPortfolio_ShouldReturnOnlyNonZeroPositionsForPortfolio() {
        // When
        List<Position> activePositions = positionRepository.findActivePositionsByPortfolio("TEST_ACCOUNT_001");
        
        // Then
        assertThat(activePositions).hasSize(2);
        assertThat(activePositions).extracting(Position::getSymbol)
                                  .containsExactlyInAnyOrder("AAPL", "GOOGL");
    }

    @Test
    void calculateTotalMarketValue_ShouldReturnCorrectValue() {
        // When
        BigDecimal totalValue = positionRepository.calculateTotalMarketValue("TEST_ACCOUNT_001");
        
        // Then
        // AAPL: 10 * 155 = 1550
        // GOOGL: 5 * 2600 = 13000
        // MSFT: 0 * 310 = 0
        // Total: 1550 + 13000 + 0 = 14550
        assertThat(totalValue).isEqualByComparingTo(new BigDecimal("14550.00"));
    }

    @Test
    void calculateTotalUnrealizedPnL_ShouldReturnCorrectValue() {
        // When
        BigDecimal totalPnL = positionRepository.calculateTotalUnrealizedPnL("TEST_ACCOUNT_001");
        
        // Then
        // AAPL: (155 - 150) * 10 = 50
        // GOOGL: (2600 - 2500) * 5 = 500
        // MSFT: (310 - 300) * 0 = 0
        // Total: 50 + 500 + 0 = 550
        assertThat(totalPnL).isEqualByComparingTo(new BigDecimal("550.00"));
    }

    @Test
    void findBySymbol_ShouldReturnPositionsForSymbol() {
        // When
        List<Position> positions = positionRepository.findBySymbol("AAPL");
        
        // Then
        assertThat(positions).hasSize(1);
        assertThat(positions.get(0).getSymbol()).isEqualTo("AAPL");
    }

    @Test
    void findPositionsNeedingPriceUpdate_ShouldReturnPositionsWithNullOrOldPrices() {
        // Given - Create a position with null current price
        Position positionWithNullPrice = new Position("NVDA", new BigDecimal("2"), new BigDecimal("800.00"));
        positionWithNullPrice.setPortfolio(testPortfolio);
        entityManager.persistAndFlush(positionWithNullPrice);
        
        LocalDateTime cutoffTime = LocalDateTime.now().plusMinutes(1);
        
        // When
        List<Position> positions = positionRepository.findPositionsNeedingPriceUpdate(cutoffTime);
        
        // Then
        assertThat(positions).hasSizeGreaterThanOrEqualTo(1);
        assertThat(positions).anyMatch(pos -> pos.getSymbol().equals("NVDA"));
    }

    @Test
    void findTopPerformingPositions_ShouldReturnPositionsOrderedByPnLDesc() {
        // When
        List<Position> topPerformers = positionRepository.findTopPerformingPositions("TEST_ACCOUNT_001");
        
        // Then
        assertThat(topPerformers).hasSize(3);
        // GOOGL should be first (500 PnL), then AAPL (50 PnL), then MSFT (0 PnL)
        assertThat(topPerformers.get(0).getSymbol()).isEqualTo("GOOGL");
        assertThat(topPerformers.get(1).getSymbol()).isEqualTo("AAPL");
        assertThat(topPerformers.get(2).getSymbol()).isEqualTo("MSFT");
    }

    @Test
    void findWorstPerformingPositions_ShouldReturnPositionsOrderedByPnLAsc() {
        // When
        List<Position> worstPerformers = positionRepository.findWorstPerformingPositions("TEST_ACCOUNT_001");
        
        // Then
        assertThat(worstPerformers).hasSize(3);
        // MSFT should be first (0 PnL), then AAPL (50 PnL), then GOOGL (500 PnL)
        assertThat(worstPerformers.get(0).getSymbol()).isEqualTo("MSFT");
        assertThat(worstPerformers.get(1).getSymbol()).isEqualTo("AAPL");
        assertThat(worstPerformers.get(2).getSymbol()).isEqualTo("GOOGL");
    }

    @Test
    void countByPortfolioAccountId_ShouldReturnCorrectCount() {
        // When
        long count = positionRepository.countByPortfolioAccountId("TEST_ACCOUNT_001");
        
        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    void save_ShouldPersistPosition() {
        // Given
        Position newPosition = new Position("TSLA", new BigDecimal("3"), new BigDecimal("900.00"));
        newPosition.setCurrentPrice(new BigDecimal("950.00"));
        newPosition.setPortfolio(testPortfolio);
        
        // When
        Position saved = positionRepository.save(newPosition);
        entityManager.flush();
        entityManager.clear();
        
        // Then
        assertThat(saved.getId()).isNotNull();
        
        Optional<Position> retrieved = positionRepository.findByPortfolioAccountIdAndSymbol("TEST_ACCOUNT_001", "TSLA");
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getQuantity()).isEqualByComparingTo(new BigDecimal("3"));
    }

    @Test
    void delete_ShouldRemovePosition() {
        // Given
        Position position = positionRepository.findByPortfolioAccountIdAndSymbol("TEST_ACCOUNT_001", "AAPL").orElseThrow();
        
        // When
        positionRepository.delete(position);
        entityManager.flush();
        entityManager.clear();
        
        // Then
        Optional<Position> result = positionRepository.findByPortfolioAccountIdAndSymbol("TEST_ACCOUNT_001", "AAPL");
        assertThat(result).isEmpty();
    }
}