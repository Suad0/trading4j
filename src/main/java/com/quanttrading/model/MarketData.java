package com.quanttrading.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "market_data", indexes = {
    @Index(name = "idx_symbol_timestamp", columnList = "symbol, timestamp")
})
public class MarketData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Column(name = "symbol", nullable = false)
    private String symbol;
    
    @NotNull
    @PositiveOrZero
    @Column(name = "price", precision = 19, scale = 4)
    private BigDecimal price;
    
    @NotNull
    @PositiveOrZero
    @Column(name = "volume", precision = 19, scale = 0)
    private BigDecimal volume;
    
    @NotNull
    @PositiveOrZero
    @Column(name = "high", precision = 19, scale = 4)
    private BigDecimal high;
    
    @NotNull
    @PositiveOrZero
    @Column(name = "low", precision = 19, scale = 4)
    private BigDecimal low;
    
    @NotNull
    @PositiveOrZero
    @Column(name = "open", precision = 19, scale = 4)
    private BigDecimal open;
    
    @NotNull
    @Column(name = "timestamp")
    private LocalDateTime timestamp;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Constructors
    public MarketData() {
        this.createdAt = LocalDateTime.now();
    }
    
    public MarketData(String symbol, BigDecimal price, BigDecimal volume, 
                     BigDecimal high, BigDecimal low, BigDecimal open, LocalDateTime timestamp) {
        this.symbol = symbol;
        this.price = price;
        this.volume = volume;
        this.high = high;
        this.low = low;
        this.open = open;
        this.timestamp = timestamp;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
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
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    // Helper methods
    public BigDecimal getClose() {
        return price; // Current price is the close price
    }
    
    public boolean isValidOHLC() {
        return high.compareTo(low) >= 0 && 
               high.compareTo(open) >= 0 && 
               high.compareTo(price) >= 0 &&
               low.compareTo(open) <= 0 && 
               low.compareTo(price) <= 0;
    }
    
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}