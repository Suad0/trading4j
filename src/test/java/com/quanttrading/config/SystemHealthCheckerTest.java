package com.quanttrading.config;

import com.quanttrading.client.AlpacaApiClient;
import com.quanttrading.client.YFinanceApiClient;
import com.quanttrading.dto.AccountInfo;
import com.quanttrading.model.MarketData;
import com.quanttrading.exception.TradingSystemException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemHealthCheckerTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private AlpacaApiClient alpacaApiClient;

    @Mock
    private YFinanceApiClient yFinanceApiClient;

    @Mock
    private Connection connection;

    @Mock
    private Statement statement;

    @Mock
    private ResultSet resultSet;

    private SystemHealthChecker healthChecker;

    @BeforeEach
    void setUp() {
        healthChecker = new SystemHealthChecker(dataSource, alpacaApiClient, yFinanceApiClient);
    }

    @Test
    void checkDatabaseHealth_Success() throws SQLException {
        // Given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(true);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery("SELECT 1")).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(1);

        // When & Then
        assertDoesNotThrow(() -> healthChecker.checkDatabaseHealth());

        verify(dataSource).getConnection();
        verify(connection).isValid(5);
        verify(statement).executeQuery("SELECT 1");
    }

    @Test
    void checkDatabaseHealth_ConnectionInvalid() throws SQLException {
        // Given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(false);

        // When & Then
        TradingSystemException exception = assertThrows(TradingSystemException.class, () -> {
            healthChecker.checkDatabaseHealth();
        });

        assertEquals("Database connection is not valid", exception.getMessage());
    }

    @Test
    void checkDatabaseHealth_QueryFailed() throws SQLException {
        // Given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(true);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery("SELECT 1")).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(0); // Wrong result

        // When & Then
        TradingSystemException exception = assertThrows(TradingSystemException.class, () -> {
            healthChecker.checkDatabaseHealth();
        });

        assertEquals("Database query test failed", exception.getMessage());
    }

    @Test
    void checkDatabaseHealth_SQLException() throws SQLException {
        // Given
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        // When & Then
        TradingSystemException exception = assertThrows(TradingSystemException.class, () -> {
            healthChecker.checkDatabaseHealth();
        });

        assertEquals("Database health check failed", exception.getMessage());
        assertTrue(exception.getCause() instanceof SQLException);
    }

    @Test
    void checkExternalApiHealth_Success() {
        // Given
        AccountInfo accountInfo = new AccountInfo();
        accountInfo.setCash(BigDecimal.valueOf(10000));
        when(alpacaApiClient.getAccountInfo()).thenReturn(accountInfo);

        MarketData marketData = new MarketData();
        marketData.setSymbol("AAPL");
        marketData.setPrice(BigDecimal.valueOf(150.00));
        when(yFinanceApiClient.getCurrentQuote("AAPL")).thenReturn(marketData);

        // When & Then
        assertDoesNotThrow(() -> healthChecker.checkExternalApiHealth());

        verify(alpacaApiClient).getAccountInfo();
        verify(yFinanceApiClient).getCurrentQuote("AAPL");
    }

    @Test
    void checkExternalApiHealth_AlpacaFailure() {
        // Given
        when(alpacaApiClient.getAccountInfo()).thenThrow(new RuntimeException("Alpaca API error"));

        // When & Then
        TradingSystemException exception = assertThrows(TradingSystemException.class, () -> {
            healthChecker.checkExternalApiHealth();
        });

        assertEquals("External API health check failed", exception.getMessage());
    }

    @Test
    void checkExternalApiHealth_YFinanceFailure() {
        // Given
        AccountInfo accountInfo = new AccountInfo();
        accountInfo.setCash(BigDecimal.valueOf(10000));
        when(alpacaApiClient.getAccountInfo()).thenReturn(accountInfo);
        when(yFinanceApiClient.getCurrentQuote("AAPL")).thenThrow(new RuntimeException("yFinance API error"));

        // When & Then
        TradingSystemException exception = assertThrows(TradingSystemException.class, () -> {
            healthChecker.checkExternalApiHealth();
        });

        assertEquals("External API health check failed", exception.getMessage());
    }

    @Test
    void checkSystemReadiness_Success() {
        // When & Then
        assertDoesNotThrow(() -> healthChecker.checkSystemReadiness());
    }

    @Test
    void checkSystemReadiness_CreatesDataDirectory() {
        // Given - data directory doesn't exist
        java.io.File dataDir = new java.io.File("./data");
        if (dataDir.exists()) {
            dataDir.delete();
        }

        // When
        assertDoesNotThrow(() -> healthChecker.checkSystemReadiness());

        // Then - directory should be created
        assertTrue(dataDir.exists());
        assertTrue(dataDir.isDirectory());
    }
}