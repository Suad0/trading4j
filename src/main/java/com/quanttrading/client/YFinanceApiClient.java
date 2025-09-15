package com.quanttrading.client;

import com.quanttrading.model.MarketData;
import com.quanttrading.dto.HistoricalData;

import java.time.LocalDate;
import java.util.List;

/**
 * Interface for yFinance API client operations
 */
public interface YFinanceApiClient {
    
    /**
     * Get current market data for a symbol
     * @param symbol Stock symbol (e.g., "AAPL")
     * @return Current market data
     */
    MarketData getCurrentQuote(String symbol);
    
    /**
     * Get historical market data for a symbol
     * @param symbol Stock symbol (e.g., "AAPL")
     * @param from Start date
     * @param to End date
     * @return List of historical data points
     */
    List<HistoricalData> getHistoricalData(String symbol, LocalDate from, LocalDate to);
    
    /**
     * Get historical market data for a symbol with period
     * @param symbol Stock symbol (e.g., "AAPL")
     * @param period Period string (e.g., "1d", "5d", "1mo", "3mo", "6mo", "1y", "2y", "5y", "10y", "ytd", "max")
     * @return List of historical data points
     */
    List<HistoricalData> getHistoricalData(String symbol, String period);
}