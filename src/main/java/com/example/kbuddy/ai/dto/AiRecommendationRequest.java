package com.example.kbuddy.ai.dto;

public record AiRecommendationRequest(
        AiUser user,
        AiTrigger trigger
) {
}
