package com.example.kbuddy.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiClientConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

    @Bean
    public RestClient aiRecommendationRestClient(AiProperties aiProperties) {
        return buildRestClient(aiProperties, aiProperties.recommendationTimeout());
    }

    @Bean
    public RestClient aiChatRestClient(AiProperties aiProperties) {
        return buildRestClient(aiProperties, aiProperties.chatTimeout());
    }

    @Bean
    public RestClient aiNewsRestClient(AiProperties aiProperties) {
        return buildRestClient(aiProperties, aiProperties.newsTimeout());
    }

    private RestClient buildRestClient(AiProperties aiProperties, Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .baseUrl(normalizeBaseUrl(aiProperties.baseUrl()))
                .requestFactory(requestFactory)
                .build();
    }

    static String normalizeBaseUrl(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
