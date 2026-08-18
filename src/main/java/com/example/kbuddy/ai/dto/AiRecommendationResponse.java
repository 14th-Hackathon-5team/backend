package com.example.kbuddy.ai.dto;

import java.util.List;

public record AiRecommendationResponse(
        Long userId,
        String summary,
        List<AiRecommendation> recommendations
) {
}
