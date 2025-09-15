package com.quanttrading.strategy;

import com.quanttrading.model.MarketData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class HistoricalMarketDataTest {

    private HistoricalMarketData historicalData;

    @BeforeEach
    void setUp() {
        historicalData = new HistoricalMarketData(5); // Small size for testing
    }

    @Test
    void testAddMarketData() {
        MarketData data = createMarketData("AAPL", new BigDecimal("150.00"), LocalDateTime.now());
        
        historicalData.addMarketData(data);
        
        assertEquals(1, historicalData.getDataCount("AAPL"));
        assertEquals(data, historicalData.getLatestData("AAPL"));
    }

    @Test
    void testAddMarketDataWithNullData() {
        historicalData.addMarketData(null);
        assertEquals(0, historicalData.getDataCount("AAPL"));
    }

    @Test
    void testAddMarketDataWithNullSymbol() {
        MarketData data = createMarketData(null, new BigDecimal("150.00"), LocalDateTime.now());
        
        historicalData.addMarketData(data);
        
        assertTrue(historicalData.getSymbols().isEmpty());
    }

    @Test
    void testMaxHistorySize() {
        LocalDateTime baseTime = LocalDateTime.now();
        
        // Add 7 data points (more than max size of 5)
        for (int i = 0; i < 7; i++) {
            MarketData data = createMarketData("AAPL", 
                    new BigDecimal("150.00").add(BigDecimal.valueOf(i)), 
                    baseTime.plusMinutes(i));
            historicalData.addMarketData(data);
        }
        
        // Should only keep the last 5 data points
        assertEquals(5, historicalData.getDataCount("AAPL"));
        
        List<MarketData> data = historicalData.getHistoricalData("AAPL");
        assertEquals(new BigDecimal("152.00"), data.get(0).getPrice()); // Should start from index 2
        assertEquals(new BigDecimal("156.00"), data.get(4).getPrice()); // Should end at index 6
    }

    @Test
    void testDataSorting() {
        LocalDateTime baseTime = LocalDateTime.now();
        
        // Add data in random order
        historicalData.addMarketData(createMarketData("AAPL", new BigDecimal("152.00"), baseTime.plusMinutes(2)));
        historicalData.addMarketData(createMarketData("AAPL", new BigDecimal("150.00"), baseTime));
        historicalData.addMarketData(createMarketData("AAPL", new BigDecimal("151.00"), baseTime.plusMinutes(1)));
        
        List<MarketData> data = historicalData.getHistoricalData("AAPL");
        
        // Should be sorted by timestamp
        assertEquals(new BigDecimal("150.00"), data.get(0).getPrice());
        assertEquals(new BigDecimal("151.00"), data.get(1).getPrice());
        assertEquals(new BigDecimal("152.00"), data.get(2).getPrice());
    }

    @Test
    void testGetHistoricalData() {
        MarketData data1 = createMarketData("AAPL", new BigDecimal("150.00"), LocalDateTime.now());
        MarketData data2 = createMarketData("AAPL", new BigDecimal("151.00"), LocalDateTime.now().plusMinutes(1));
        
        historicalData.addMarketData(data1);
        historicalData.addMarketData(data2);
        
        List<MarketData> retrieved = historicalData.getHistoricalData("AAPL");
        
        assertEquals(2, retrieved.size());
        assertEquals(data1, retrieved.get(0));
        assertEquals(data2, retrieved.get(1));
        
        // Should return a copy, not the original list
        retrieved.clear();
        assertEquals(2, historicalData.getDataCount("AAPL"));
    }

    @Test
    void testGetHistoricalDataForNonExistentSymbol() {
        List<MarketData> data = historicalData.getHistoricalData("NONEXISTENT");
        assertTrue(data.isEmpty());
    }

    @Test
    void testGetLatestData() {
        MarketData data1 = createMarketData("AAPL", new BigDecimal("150.00"), LocalDateTime.now());
        MarketData data2 = createMarketData("AAPL", new BigDecimal("151.00"), LocalDateTime.now().plusMinutes(1));
        
        historicalData.addMarketData(data1);
        historicalData.addMarketData(data2);
        
        assertEquals(data2, historicalData.getLatestData("AAPL"));
    }

    @Test
    void testGetLatestDataForNonExistentSymbol() {
        assertNull(historicalData.getLatestData("NONEXISTENT"));
    }

    @Test
    void testGetPrices() {
        historicalData.addMarketData(createMarketData("AAPL", new BigDecimal("150.00"), LocalDateTime.now()));
        historicalData.addMarketData(createMarketData("AAPL", new BigDecimal("151.00"), LocalDateTime.now().plusMinutes(1)));
        historicalData.addMarketData(createMarketData("AAPL", new BigDecimal("152.00"), LocalDateTime.now().plusMinutes(2)));
        
        List<BigDecimal> prices = historicalData.getPrices("AAPL");
        
        assertEquals(3, prices.size());
        assertEquals(new BigDecimal("150.00"), prices.get(0));
        assertEquals(new BigDecimal("151.00"), prices.get(1));
        assertEquals(new BigDecimal("152.00"), prices.get(2));
    }

    @Test
    void testGetLastPrices() {
        for (int i = 0; i < 5; i++) {
            historicalData.addMarketData(createMarketData("AAPL", 
                    new BigDecimal("150.00").add(BigDecimal.valueOf(i)), 
                    LocalDateTime.now().plusMinutes(i)));
        }
        
        List<BigDecimal> lastThree = historicalData.getLastPrices("AAPL", 3);
        
        assertEquals(3, lastThree.size());
        assertEquals(new BigDecimal("152.00"), lastThree.get(0));
        assertEquals(new BigDecimal("153.00"), lastThree.get(1));
        assertEquals(new BigDecimal("154.00"), lastThree.get(2));
    }

    @Test
    void testGetLastPricesWhenRequestingMoreThanAvailable() {
        historicalData.addMarketData(createMarketData("AAPL", new BigDecimal("150.00"), LocalDateTime.now()));
        historicalData.addMarketData(createMarketData("AAPL", new BigDecimal("151.00"), LocalDateTime.now().plusMinutes(1)));
        
        List<BigDecimal> prices = historicalData.getLastPrices("AAPL", 5);
        
        assertEquals(2, prices.size());
        assertEquals(new BigDecimal("150.00"), prices.get(0));
        assertEquals(new BigDecimal("151.00"), prices.get(1));
    }

    @Test
    void testHasEnoughData() {
        assertFalse(historicalData.hasEnoughData("AAPL", 1));
        
        historicalData.addMarketData(createMarketData("AAPL", new BigDecimal("150.00"), LocalDateTime.now()));
        
        assertTrue(historicalData.hasEnoughData("AAPL", 1));
        assertFalse(historicalData.hasEnoughData("AAPL", 2));
        
        historicalData.addMarketData(createMarketData("AAPL", new BigDecimal("151.00"), LocalDateTime.now().plusMinutes(1)));
        
        assertTrue(historicalData.hasEnoughData("AAPL", 2));
    }

    @Test
    void testGetDataCount() {
        assertEquals(0, historicalData.getDataCount("AAPL"));
        
        historicalData.addMarketData(createMarketData("AAPL", new BigDecimal("150.00"), LocalDateTime.now()));
        assertEquals(1, historicalData.getDataCount("AAPL"));
        
        historicalData.addMarketData(createMarketData("AAPL", new BigDecimal("151.00"), LocalDateTime.now().plusMinutes(1)));
        assertEquals(2, historicalData.getDataCount("AAPL"));
    }

    @Test
    void testClear() {
        historicalData.addMarketData(createMarketData("AAPL", new BigDecimal("150.00"), LocalDateTime.now()));
        historicalData.addMarketData(createMarketData("TSLA", new BigDecimal("200.00"), LocalDateTime.now()));
        
        assertEquals(2, historicalData.getSymbols().size());
        
        historicalData.clear();
        
        assertTrue(historicalData.getSymbols().isEmpty());
        assertEquals(0, historicalData.getDataCount("AAPL"));
        assertEquals(0, historicalData.getDataCount("TSLA"));
    }

    @Test
    void testClearSpecificSymbol() {
        historicalData.addMarketData(createMarketData("AAPL", new BigDecimal("150.00"), LocalDateTime.now()));
        historicalData.addMarketData(createMarketData("TSLA", new BigDecimal("200.00"), LocalDateTime.now()));
        
        historicalData.clear("AAPL");
        
        assertEquals(0, historicalData.getDataCount("AAPL"));
        assertEquals(1, historicalData.getDataCount("TSLA"));
        assertEquals(Set.of("TSLA"), historicalData.getSymbols());
    }

    @Test
    void testGetSymbols() {
        assertTrue(historicalData.getSymbols().isEmpty());
        
        historicalData.addMarketData(createMarketData("AAPL", new BigDecimal("150.00"), LocalDateTime.now()));
        historicalData.addMarketData(createMarketData("TSLA", new BigDecimal("200.00"), LocalDateTime.now()));
        
        Set<String> symbols = historicalData.getSymbols();
        assertEquals(2, symbols.size());
        assertTrue(symbols.contains("AAPL"));
        assertTrue(symbols.contains("TSLA"));
    }

    @Test
    void testMultipleSymbols() {
        historicalData.addMarketData(createMarketData("AAPL", new BigDecimal("150.00"), LocalDateTime.now()));
        historicalData.addMarketData(createMarketData("TSLA", new BigDecimal("200.00"), LocalDateTime.now()));
        historicalData.addMarketData(createMarketData("AAPL", new BigDecimal("151.00"), LocalDateTime.now().plusMinutes(1)));
        
        assertEquals(2, historicalData.getDataCount("AAPL"));
        assertEquals(1, historicalData.getDataCount("TSLA"));
        
        assertEquals(new BigDecimal("151.00"), historicalData.getLatestData("AAPL").getPrice());
        assertEquals(new BigDecimal("200.00"), historicalData.getLatestData("TSLA").getPrice());
    }

    private MarketData createMarketData(String symbol, BigDecimal price, LocalDateTime timestamp) {
        MarketData data = new MarketData();
        data.setSymbol(symbol);
        data.setPrice(price);
        data.setTimestamp(timestamp);
        return data;
    }
}