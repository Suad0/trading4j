package com.quanttrading.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for current market data
 */
public class MarketDataResponse {
    
    private String symbol;
    private BigDecimal price;
    private BigDecimal volume;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal open;
    private BigDecimal change;
    private BigDecimal changePercent;
    private LocalDateTime timestamp;
    private boolean isMarketOpen;
    
    // Constructors
    public MarketDataResponse() {}
    
    public MarketDataResponse(String symbol, BigDecimal price, BigDecimal volume,
                            BigDecimal high, BigDecimal low, BigDecimal open,
                            LocalDateTime timestamp) {
        this.symbol = symbol;
        this.price = price;
        this.volume = volume;
        this.high = high;
        this.low = low;
        this.open = open;
        this.timestamp = timestamp;
        
        // Calculate change and change percentage
        if (open != null && price != null) {
            this.change = price.subtract(open);
            if (open.compareTo(BigDecimal.ZERO) > 0) {
                this.changePercent = change.divide(open, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            }
        }
    }
    
    // Getters and Setters
    public String getSymbol() {
        return symbol;
    }
    
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    public BigDecimal getVolume() {
        return volume;
    }
    
    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }
    
    public BigDecimal getHigh() {
        return high;
    }
    
    public void setHigh(BigDecimal high) {
        this.high = high;
    }
    
    public BigDecimal getLow() {
        return low;
    }
    
    public void setLow(BigDecimal low) {
        this.low = low;
    }
    
    public BigDecimal getOpen() {
        return open;
    }
    
    public void setOpen(BigDecimal open) {
        this.open = open;
        // Recalculate change when open price is set
        if (this.price != null) {
            this.change = this.price.subtract(open);
            if (open.compareTo(BigDecimal.ZERO) > 0) {
                this.changePercent = change.divide(open, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            }
        }
    }
    
    public BigDecimal getChange() {
        return change;
    }
    
    public void setChange(BigDecimal change) {
        this.change = change;
    }
    
    public BigDecimal getChangePercent() {
        return changePercent;
    }
    
    public void setChangePercent(BigDecimal changePercent) {
        this.changePercent = changePercent;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public boolean isMarketOpen() {
        return isMarketOpen;
    }
    
    public void setMarketOpen(boolean marketOpen) {
        isMarketOpen = marketOpen;
    }
}