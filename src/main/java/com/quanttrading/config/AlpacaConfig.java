package com.quanttrading.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@ConfigurationProperties(prefix = "alpaca")
@Validated
public record AlpacaConfig(
    @NotBlank String baseUrl,
    @NotBlank String apiKey,
    @NotBlank String secretKey,
    @NotNull Boolean paperTrading
) {
}