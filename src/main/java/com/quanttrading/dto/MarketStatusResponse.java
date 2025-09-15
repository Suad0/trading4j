package com.quanttrading.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for market status information
 */
public class MarketStatusResponse {
    
    private boolean isOpen;
    private String status;
    private LocalDateTime timestamp;
    private String timezone;
    private List<String> supportedSymbols;
    
    // Constructors
    public MarketStatusResponse() {
        this.timestamp = LocalDateTime.now();
        this.timezone = "America/New_York"; // Default to NYSE timezone
    }
    
    public MarketStatusResponse(boolean isOpen, String status) {
        this.isOpen = isOpen;
        this.status = status;
        this.timestamp = LocalDateTime.now();
        this.timezone = "America/New_York";
    }
    
    public MarketStatusResponse(boolean isOpen, String status, List<String> supportedSymbols) {
        this.isOpen = isOpen;
        this.status = status;
        this.supportedSymbols = supportedSymbols;
        this.timestamp = LocalDateTime.now();
        this.timezone = "America/New_York";
    }
    
    // Getters and Setters
    public boolean isOpen() {
        return isOpen;
    }
    
    public void setOpen(boolean open) {
        isOpen = open;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getTimezone() {
        return timezone;
    }
    
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
    
    public List<String> getSupportedSymbols() {
        return supportedSymbols;
    }
    
    public void setSupportedSymbols(List<String> supportedSymbols) {
        this.supportedSymbols = supportedSymbols;
    }
}