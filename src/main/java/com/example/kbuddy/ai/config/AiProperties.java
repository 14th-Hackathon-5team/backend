package com.example.kbuddy.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "ai.fastapi")
public record AiProperties(
        String baseUrl,
        String internalApiKey,
        Duration recommendationTimeout,
        Duration chatTimeout
) {
}
