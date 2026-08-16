package com.example.kbuddy.ai.dto;

import com.example.kbuddy.notification.entity.NotificationCategory;

import java.util.List;
import java.util.Map;

public record AiChatResponse(
        String answer,
        NotificationCategory category,
        List<String> suggestedQuestions,
        List<Map<String, Object>> sources
) {
}
