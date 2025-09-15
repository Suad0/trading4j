package com.quanttrading.service.impl;

import com.quanttrading.client.AlpacaApiClient;
import com.quanttrading.dto.AccountInfo;
import com.quanttrading.dto.PerformanceMetrics;
import com.quanttrading.exception.TradingSystemException;
import com.quanttrading.model.Portfolio;
import com.quanttrading.model.Position;
import com.quanttrading.repository.PortfolioRepository;
import com.quanttrading.repository.PositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceImplTest {
    
    @Mock
    private PortfolioRepository portfolioRepository;
    
    @Mock
    private PositionRepository positionRepository;
    
    @Mock
    private AlpacaApiClient alpacaApiClient;
    
    @InjectMocks
    private PortfolioServiceImpl portfolioService;
    
    private Portfolio testPortfolio;
    private Position testPosition1;
    private Position testPosition2;
    private final String TEST_ACCOUNT_ID = "test-account";
    
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(portfolioService, "defaultAccountId", TEST_ACCOUNT_ID);
        
        testPortfolio = new Portfolio(TEST_ACCOUNT_ID, new BigDecimal("10000.00"), new BigDecimal("15000.00"));
        
        testPosition1 = new Position("AAPL", new BigDecimal("10"), new BigDecimal("150.00"));
        testPosition1.setCurrentPrice(new BigDecimal("155.00"));
        testPosition1.setPortfolio(testPortfolio);
        
        testPosition2 = new Position("GOOGL", new BigDecimal("5"), new BigDecimal("2000.00"));
        testPosition2.setCurrentPrice(new BigDecimal("2100.00"));
        testPosition2.setPortfolio(testPortfolio);
        
        testPortfolio.setPositions(Arrays.asList(testPosition1, testPosition2));
    }
    
    @Test
    void getCurrentPortfolio_ShouldReturnPortfolio_WhenExists() {
        // Given
        when(portfolioRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.of(testPortfolio));
        
        // When
        Portfolio result = portfolioService.getCurrentPortfolio();
        
        // Then
        assertNotNull(result);
        assertEquals(TEST_ACCOUNT_ID, result.getAccountId());
        assertEquals(new BigDecimal("10000.00"), result.getCashBalance());
        verify(portfolioRepository).findByAccountId(TEST_ACCOUNT_ID);
    }
    
    @Test
    void getCurrentPortfolio_ShouldThrowException_WhenNotExists() {
        // Given
        when(portfolioRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(TradingSystemException.class, () -> portfolioService.getCurrentPortfolio());
        verify(portfolioRepository).findByAccountId(TEST_ACCOUNT_ID);
    }
    
    @Test
    void getPositions_ShouldReturnPositions_ForDefaultAccount() {
        // Given
        List<Position> expectedPositions = Arrays.asList(testPosition1, testPosition2);
        when(positionRepository.findByPortfolioAccountId(TEST_ACCOUNT_ID)).thenReturn(expectedPositions);
        
        // When
        List<Position> result = portfolioService.getPositions();
        
        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("AAPL", result.get(0).getSymbol());
        assertEquals("GOOGL", result.get(1).getSymbol());
        verify(positionRepository).findByPortfolioAccountId(TEST_ACCOUNT_ID);
    }
    
    @Test
    void getActivePositions_ShouldReturnActivePositions() {
        // Given
        List<Position> activePositions = Arrays.asList(testPosition1, testPosition2);
        when(positionRepository.findActivePositionsByPortfolio(TEST_ACCOUNT_ID)).thenReturn(activePositions);
        
        // When
        List<Position> result = portfolioService.getActivePositions(TEST_ACCOUNT_ID);
        
        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(positionRepository).findActivePositionsByPortfolio(TEST_ACCOUNT_ID);
    }
    
    @Test
    void calculatePerformance_ShouldReturnCorrectMetrics() {
        // Given
        when(portfolioRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.of(testPortfolio));
        when(positionRepository.calculateTotalMarketValue(TEST_ACCOUNT_ID)).thenReturn(new BigDecimal("12050.00"));
        when(positionRepository.calculateTotalUnrealizedPnL(TEST_ACCOUNT_ID)).thenReturn(new BigDecimal("550.00"));
        when(positionRepository.findActivePositionsByPortfolio(TEST_ACCOUNT_ID)).thenReturn(Arrays.asList(testPosition1, testPosition2));
        
        // When
        PerformanceMetrics result = portfolioService.calculatePerformance(TEST_ACCOUNT_ID);
        
        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("22050.00"), result.getTotalValue()); // 10000 + 12050
        assertEquals(new BigDecimal("10000.00"), result.getCashBalance());
        assertEquals(new BigDecimal("12050.00"), result.getPositionsValue());
        assertEquals(new BigDecimal("550.00"), result.getTotalUnrealizedPnL());
        assertEquals(2, result.getPositionCount());
        
        verify(portfolioRepository).findByAccountId(TEST_ACCOUNT_ID);
        verify(positionRepository).calculateTotalMarketValue(TEST_ACCOUNT_ID);
        verify(positionRepository).calculateTotalUnrealizedPnL(TEST_ACCOUNT_ID);
    }
    
    @Test
    void calculatePerformance_ShouldThrowException_WhenPortfolioNotFound() {
        // Given
        when(portfolioRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(TradingSystemException.class, () -> portfolioService.calculatePerformance(TEST_ACCOUNT_ID));
        verify(portfolioRepository).findByAccountId(TEST_ACCOUNT_ID);
    }
    
    @Test
    void updatePosition_ShouldCreateNewPosition_WhenNotExists() {
        // Given
        when(portfolioRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.of(testPortfolio));
        when(positionRepository.findByPortfolioAccountIdAndSymbol(TEST_ACCOUNT_ID, "MSFT")).thenReturn(Optional.empty());
        when(positionRepository.calculateTotalMarketValue(TEST_ACCOUNT_ID)).thenReturn(new BigDecimal("5000.00"));
        
        // When
        portfolioService.updatePosition(TEST_ACCOUNT_ID, "MSFT", new BigDecimal("20"), new BigDecimal("250.00"));
        
        // Then
        verify(portfolioRepository).findByAccountId(TEST_ACCOUNT_ID);
        verify(positionRepository).findByPortfolioAccountIdAndSymbol(TEST_ACCOUNT_ID, "MSFT");
        verify(positionRepository).save(any(Position.class));
        verify(portfolioRepository).save(testPortfolio);
    }
    
    @Test
    void updatePosition_ShouldUpdateExistingPosition_WhenExists() {
        // Given
        Position existingPosition = new Position("AAPL", new BigDecimal("10"), new BigDecimal("150.00"));
        when(portfolioRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.of(testPortfolio));
        when(positionRepository.findByPortfolioAccountIdAndSymbol(TEST_ACCOUNT_ID, "AAPL")).thenReturn(Optional.of(existingPosition));
        when(positionRepository.calculateTotalMarketValue(TEST_ACCOUNT_ID)).thenReturn(new BigDecimal("5000.00"));
        
        // When
        portfolioService.updatePosition(TEST_ACCOUNT_ID, "AAPL", new BigDecimal("5"), new BigDecimal("160.00"));
        
        // Then
        verify(portfolioRepository).findByAccountId(TEST_ACCOUNT_ID);
        verify(positionRepository).findByPortfolioAccountIdAndSymbol(TEST_ACCOUNT_ID, "AAPL");
        verify(positionRepository).save(existingPosition);
        
        // Verify position was updated correctly
        assertEquals(new BigDecimal("15"), existingPosition.getQuantity()); // 10 + 5
        // New average price should be (10*150 + 5*160) / 15 = 153.33
        assertEquals(0, new BigDecimal("153.3333").compareTo(existingPosition.getAveragePrice()));
    }
    
    @Test
    void updatePosition_ShouldDeletePosition_WhenQuantityBecomesZero() {
        // Given
        Position existingPosition = new Position("AAPL", new BigDecimal("10"), new BigDecimal("150.00"));
        when(portfolioRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.of(testPortfolio));
        when(positionRepository.findByPortfolioAccountIdAndSymbol(TEST_ACCOUNT_ID, "AAPL")).thenReturn(Optional.of(existingPosition));
        when(positionRepository.calculateTotalMarketValue(TEST_ACCOUNT_ID)).thenReturn(new BigDecimal("5000.00"));
        
        // When
        portfolioService.updatePosition(TEST_ACCOUNT_ID, "AAPL", new BigDecimal("-10"), new BigDecimal("160.00"));
        
        // Then
        verify(portfolioRepository).findByAccountId(TEST_ACCOUNT_ID);
        verify(positionRepository).findByPortfolioAccountIdAndSymbol(TEST_ACCOUNT_ID, "AAPL");
        verify(positionRepository).delete(existingPosition);
        verify(portfolioRepository).save(testPortfolio);
    }
    
    @Test
    void synchronizePortfolio_ShouldUpdatePortfolio_WithBrokerData() {
        // Given
        AccountInfo accountInfo = new AccountInfo();
        accountInfo.setCash(new BigDecimal("15000.00"));
        
        com.quanttrading.model.Position brokerPosition = new com.quanttrading.model.Position("AAPL", new BigDecimal("10"), new BigDecimal("150.00"));
        brokerPosition.setCurrentPrice(new BigDecimal("155.00"));
        
        when(alpacaApiClient.getAccountInfo()).thenReturn(accountInfo);
        when(alpacaApiClient.getPositions()).thenReturn(Arrays.asList(brokerPosition));
        when(portfolioRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.of(testPortfolio));
        when(portfolioRepository.save(any(Portfolio.class))).thenReturn(testPortfolio);
        
        // When
        portfolioService.synchronizePortfolio(TEST_ACCOUNT_ID);
        
        // Then
        verify(alpacaApiClient).getAccountInfo();
        verify(alpacaApiClient).getPositions();
        verify(portfolioRepository).save(any(Portfolio.class));
    }
    
    @Test
    void synchronizePortfolio_ShouldThrowException_WhenApiFails() {
        // Given
        when(alpacaApiClient.getAccountInfo()).thenThrow(new RuntimeException("API Error"));
        
        // When & Then
        assertThrows(TradingSystemException.class, () -> portfolioService.synchronizePortfolio(TEST_ACCOUNT_ID));
        verify(alpacaApiClient).getAccountInfo();
    }
    
    @Test
    void getPosition_ShouldReturnPosition_WhenExists() {
        // Given
        when(positionRepository.findByPortfolioAccountIdAndSymbol(TEST_ACCOUNT_ID, "AAPL")).thenReturn(Optional.of(testPosition1));
        
        // When
        Optional<Position> result = portfolioService.getPosition(TEST_ACCOUNT_ID, "AAPL");
        
        // Then
        assertTrue(result.isPresent());
        assertEquals("AAPL", result.get().getSymbol());
        assertEquals(new BigDecimal("10"), result.get().getQuantity());
        verify(positionRepository).findByPortfolioAccountIdAndSymbol(TEST_ACCOUNT_ID, "AAPL");
    }
    
    @Test
    void getPosition_ShouldReturnEmpty_WhenNotExists() {
        // Given
        when(positionRepository.findByPortfolioAccountIdAndSymbol(TEST_ACCOUNT_ID, "MSFT")).thenReturn(Optional.empty());
        
        // When
        Optional<Position> result = portfolioService.getPosition(TEST_ACCOUNT_ID, "MSFT");
        
        // Then
        assertFalse(result.isPresent());
        verify(positionRepository).findByPortfolioAccountIdAndSymbol(TEST_ACCOUNT_ID, "MSFT");
    }
    
    @Test
    void savePortfolio_ShouldSaveAndReturnPortfolio() {
        // Given
        when(portfolioRepository.save(testPortfolio)).thenReturn(testPortfolio);
        
        // When
        Portfolio result = portfolioService.savePortfolio(testPortfolio);
        
        // Then
        assertNotNull(result);
        assertEquals(TEST_ACCOUNT_ID, result.getAccountId());
        verify(portfolioRepository).save(testPortfolio);
    }
}