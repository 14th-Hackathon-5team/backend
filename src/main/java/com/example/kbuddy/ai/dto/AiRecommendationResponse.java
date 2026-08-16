package com.example.kbuddy.ai.dto;

import java.util.List;

public record AiRecommendationResponse(
        Long userId,
        List<AiRecommendation> recommendations
) {
}
