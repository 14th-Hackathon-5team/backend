package com.example.kbuddy.auth.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String issuer,
        String privateKey,
        String publicKey,
        Duration signupTokenExpiration,
        Duration accessTokenExpiration
) {
}
