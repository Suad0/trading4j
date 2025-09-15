package com.quanttrading.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@ConfigurationProperties(prefix = "security")
@Validated
public record SecurityProperties(
    BasicAuth basicAuth
) {
    public record BasicAuth(
        @NotBlank String username,
        @NotBlank String password
    ) {}
}