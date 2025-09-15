package com.quanttrading.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@ConfigurationProperties(prefix = "market-data")
@Validated
public record MarketDataConfig(
    @NotNull @Min(1) Integer cacheDuration,
    @NotNull @Min(100) Integer rateLimitDelay
) {
}