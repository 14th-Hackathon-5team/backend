package com.example.kbuddy.ai.dto;

import java.util.Map;

public record AiRecommendation(
        AiRecommendationType type,
        AiRecommendationPriority priority,
        String title,
        String reason,
        Map<String, Object> detail
) {
}
