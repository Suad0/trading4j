package com.quanttrading.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for account information
 */
public class AccountInfo {
    
    private String accountId;
    private BigDecimal cash;
    private BigDecimal portfolioValue;
    private BigDecimal buyingPower;
    private BigDecimal dayTradingBuyingPower;
    private String status;
    private boolean patternDayTrader;
    private boolean tradingBlocked;
    private boolean accountBlocked;
    private int dayTradeCount;
    private LocalDateTime lastUpdated;
    
    // Constructors
    public AccountInfo() {}
    
    public AccountInfo(String accountId, BigDecimal cash, BigDecimal portfolioValue, BigDecimal buyingPower) {
        this.accountId = accountId;
        this.cash = cash;
        this.portfolioValue = portfolioValue;
        this.buyingPower = buyingPower;
        this.lastUpdated = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getAccountId() {
        return accountId;
    }
    
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
    
    public BigDecimal getCash() {
        return cash;
    }
    
    public void setCash(BigDecimal cash) {
        this.cash = cash;
    }
    
    public BigDecimal getPortfolioValue() {
        return portfolioValue;
    }
    
    public void setPortfolioValue(BigDecimal portfolioValue) {
        this.portfolioValue = portfolioValue;
    }
    
    public BigDecimal getBuyingPower() {
        return buyingPower;
    }
    
    public void setBuyingPower(BigDecimal buyingPower) {
        this.buyingPower = buyingPower;
    }
    
    public BigDecimal getDayTradingBuyingPower() {
        return dayTradingBuyingPower;
    }
    
    public void setDayTradingBuyingPower(BigDecimal dayTradingBuyingPower) {
        this.dayTradingBuyingPower = dayTradingBuyingPower;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public boolean isPatternDayTrader() {
        return patternDayTrader;
    }
    
    public void setPatternDayTrader(boolean patternDayTrader) {
        this.patternDayTrader = patternDayTrader;
    }
    
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    public boolean isTradingBlocked() {
        return tradingBlocked;
    }
    
    public void setTradingBlocked(boolean tradingBlocked) {
        this.tradingBlocked = tradingBlocked;
    }
    
    public boolean isAccountBlocked() {
        return accountBlocked;
    }
    
    public void setAccountBlocked(boolean accountBlocked) {
        this.accountBlocked = accountBlocked;
    }
    
    public int getDayTradeCount() {
        return dayTradeCount;
    }
    
    public void setDayTradeCount(int dayTradeCount) {
        this.dayTradeCount = dayTradeCount;
    }
}