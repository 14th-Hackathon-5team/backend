package com.example.kbuddy.ai.dto;

import com.example.kbuddy.notification.entity.NotificationCategory;

import java.util.Map;

public record AiRecommendation(
        NotificationCategory category,
        String title,
        Integer priority,
        String reason,
        String summary,
        Map<String, Object> details,
        Map<String, Object> source
) {
}
