package com.quanttrading.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

@ConfigurationProperties(prefix = "trading")
@Validated
public record TradingConfig(
    @NotNull @Positive BigDecimal maxPositionSize,
    @NotNull @DecimalMin("0.001") @DecimalMax("0.1") BigDecimal riskPerTrade,
    @NotNull Boolean enableAutoTrading,
    @NotEmpty List<String> allowedSymbols
) {
}