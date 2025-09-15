package com.quanttrading.service.impl;

import com.quanttrading.client.AlpacaApiClient;
import com.quanttrading.dto.PerformanceMetrics;
import com.quanttrading.exception.TradingSystemException;
import com.quanttrading.model.Portfolio;
import com.quanttrading.model.Position;
import com.quanttrading.repository.PortfolioRepository;
import com.quanttrading.repository.PositionRepository;
import com.quanttrading.service.PortfolioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of PortfolioService for portfolio management operations
 */
@Service
@Transactional
public class PortfolioServiceImpl implements PortfolioService {
    
    private static final Logger logger = LoggerFactory.getLogger(PortfolioServiceImpl.class);
    
    @Value("${trading.default-account-id:default}")
    private String defaultAccountId;
    
    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;
    private final AlpacaApiClient alpacaApiClient;
    
    @Autowired
    public PortfolioServiceImpl(PortfolioRepository portfolioRepository,
                               PositionRepository positionRepository,
                               AlpacaApiClient alpacaApiClient) {
        this.portfolioRepository = portfolioRepository;
        this.positionRepository = positionRepository;
        this.alpacaApiClient = alpacaApiClient;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Portfolio getCurrentPortfolio() {
        return getPortfolio(defaultAccountId)
                .orElseThrow(() -> new TradingSystemException("Default portfolio not found"));
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Portfolio> getPortfolio(String accountId) {
        logger.debug("Retrieving portfolio for account: {}", accountId);
        return portfolioRepository.findByAccountId(accountId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Position> getPositions() {
        return getPositions(defaultAccountId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Position> getPositions(String accountId) {
        logger.debug("Retrieving positions for account: {}", accountId);
        return positionRepository.findByPortfolioAccountId(accountId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Position> getActivePositions() {
        return getActivePositions(defaultAccountId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Position> getActivePositions(String accountId) {
        logger.debug("Retrieving active positions for account: {}", accountId);
        return positionRepository.findActivePositionsByPortfolio(accountId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PerformanceMetrics calculatePerformance() {
        return calculatePerformance(defaultAccountId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PerformanceMetrics calculatePerformance(String accountId) {
        logger.debug("Calculating performance metrics for account: {}", accountId);
        
        Portfolio portfolio = getPortfolio(accountId)
                .orElseThrow(() -> new TradingSystemException("Portfolio not found for account: " + accountId));
        
        BigDecimal cashBalance = portfolio.getCashBalance();
        BigDecimal positionsValue = positionRepository.calculateTotalMarketValue(accountId);
        BigDecimal totalUnrealizedPnL = positionRepository.calculateTotalUnrealizedPnL(accountId);
        BigDecimal totalValue = cashBalance.add(positionsValue);
        
        PerformanceMetrics metrics = new PerformanceMetrics(totalValue, cashBalance, positionsValue, totalUnrealizedPnL);
        
        // Calculate returns if we have historical data
        calculateReturns(metrics, portfolio, totalValue);
        
        // Set position count
        metrics.setPositionCount(getActivePositions(accountId).size());
        
        logger.debug("Performance metrics calculated - Total Value: {}, P&L: {}", totalValue, totalUnrealizedPnL);
        
        return metrics;
    }
    
    @Override
    public void updatePosition(String symbol, BigDecimal quantity, BigDecimal price) {
        updatePosition(defaultAccountId, symbol, quantity, price);
    }
    
    @Override
    public void updatePosition(String accountId, String symbol, BigDecimal quantity, BigDecimal price) {
        logger.debug("Updating position for account: {}, symbol: {}, quantity: {}, price: {}", 
                    accountId, symbol, quantity, price);
        
        Portfolio portfolio = getPortfolio(accountId)
                .orElseThrow(() -> new TradingSystemException("Portfolio not found for account: " + accountId));
        
        Optional<Position> existingPosition = positionRepository.findByPortfolioAccountIdAndSymbol(accountId, symbol);
        
        if (existingPosition.isPresent()) {
            updateExistingPosition(existingPosition.get(), quantity, price);
        } else {
            createNewPosition(portfolio, symbol, quantity, price);
        }
        
        // Update portfolio total value
        updatePortfolioValue(portfolio);
    }
    
    @Override
    public void updateCurrentPrices(String accountId) {
        logger.debug("Updating current prices for account: {}", accountId);
        
        List<Position> positions = getActivePositions(accountId);
        
        for (Position position : positions) {
            try {
                // This would typically call market data service to get current price
                // For now, we'll leave the current price as is
                logger.debug("Would update price for symbol: {}", position.getSymbol());
            } catch (Exception e) {
                logger.warn("Failed to update price for symbol: {}", position.getSymbol(), e);
            }
        }
    }
    
    @Override
    public void synchronizePortfolio(String accountId) {
        logger.debug("Synchronizing portfolio with external broker for account: {}", accountId);
        
        try {
            // Get account info from Alpaca
            var accountInfo = alpacaApiClient.getAccountInfo();
            var brokerPositions = alpacaApiClient.getPositions();
            
            // Update or create portfolio
            Portfolio portfolio = getPortfolio(accountId).orElse(new Portfolio());
            portfolio.setAccountId(accountId);
            portfolio.setCashBalance(accountInfo.getCash());
            
            // Clear existing positions and add broker positions
            portfolio.setPositions(new ArrayList<>());
            
            for (var brokerPosition : brokerPositions) {
                Position position = new Position(
                    brokerPosition.getSymbol(),
                    brokerPosition.getQuantity(),
                    brokerPosition.getAveragePrice()
                );
                position.setCurrentPrice(brokerPosition.getCurrentPrice());
                portfolio.addPosition(position);
            }
            
            savePortfolio(portfolio);
            
            logger.info("Portfolio synchronized successfully for account: {}", accountId);
            
        } catch (Exception e) {
            logger.error("Failed to synchronize portfolio for account: {}", accountId, e);
            throw new TradingSystemException("Portfolio synchronization failed", e);
        }
    }
    
    @Override
    public Portfolio savePortfolio(Portfolio portfolio) {
        logger.debug("Saving portfolio for account: {}", portfolio.getAccountId());
        return portfolioRepository.save(portfolio);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Position> getPosition(String symbol) {
        return getPosition(defaultAccountId, symbol);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Position> getPosition(String accountId, String symbol) {
        logger.debug("Retrieving position for account: {}, symbol: {}", accountId, symbol);
        return positionRepository.findByPortfolioAccountIdAndSymbol(accountId, symbol);
    }
    
    // Private helper methods
    
    private void updateExistingPosition(Position position, BigDecimal quantity, BigDecimal price) {
        BigDecimal currentQuantity = position.getQuantity();
        BigDecimal currentAvgPrice = position.getAveragePrice();
        
        BigDecimal newQuantity = currentQuantity.add(quantity);
        
        if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
            // Position closed
            positionRepository.delete(position);
            logger.debug("Position closed for symbol: {}", position.getSymbol());
        } else {
            // Calculate new average price
            BigDecimal totalCost = currentQuantity.multiply(currentAvgPrice).add(quantity.multiply(price));
            BigDecimal newAvgPrice = totalCost.divide(newQuantity, 4, RoundingMode.HALF_UP);
            
            position.setQuantity(newQuantity);
            position.setAveragePrice(newAvgPrice);
            
            positionRepository.save(position);
            logger.debug("Position updated for symbol: {}, new quantity: {}, new avg price: {}", 
                        position.getSymbol(), newQuantity, newAvgPrice);
        }
    }
    
    private void createNewPosition(Portfolio portfolio, String symbol, BigDecimal quantity, BigDecimal price) {
        Position newPosition = new Position(symbol, quantity, price);
        newPosition.setPortfolio(portfolio);
        
        positionRepository.save(newPosition);
        logger.debug("New position created for symbol: {}, quantity: {}, price: {}", symbol, quantity, price);
    }
    
    private void updatePortfolioValue(Portfolio portfolio) {
        BigDecimal positionsValue = positionRepository.calculateTotalMarketValue(portfolio.getAccountId());
        BigDecimal totalValue = portfolio.getCashBalance().add(positionsValue);
        
        portfolio.setTotalValue(totalValue);
        portfolioRepository.save(portfolio);
    }
    
    private void calculateReturns(PerformanceMetrics metrics, Portfolio portfolio, BigDecimal currentValue) {
        // For now, we'll set returns to zero since we don't have historical data
        // In a real implementation, this would calculate based on previous day's value
        metrics.setDailyPnL(BigDecimal.ZERO);
        metrics.setDailyReturn(BigDecimal.ZERO);
        metrics.setTotalReturn(BigDecimal.ZERO);
        
        // TODO: Implement historical performance calculation
        logger.debug("Return calculations not yet implemented - using zero values");
    }
}