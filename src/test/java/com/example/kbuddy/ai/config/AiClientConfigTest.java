package com.example.kbuddy.ai.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiClientConfigTest {

    @Test
    void baseUrl에_trailing_slash가_있으면_제거된다() {
        assertThat(AiClientConfig.normalizeBaseUrl("http://localhost:8000/")).isEqualTo("http://localhost:8000");
    }

    @Test
    void baseUrl에_trailing_slash가_없으면_그대로_유지된다() {
        assertThat(AiClientConfig.normalizeBaseUrl("http://localhost:8000")).isEqualTo("http://localhost:8000");
    }
}
