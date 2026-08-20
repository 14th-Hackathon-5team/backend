package com.example.kbuddy.job.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "seoul.job")
public record SeoulJobProperties(
        String baseUrl,
        String apiKey,
        Duration timeout
) {
}
