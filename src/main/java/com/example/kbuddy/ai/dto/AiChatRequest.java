package com.example.kbuddy.ai.dto;

public record AiChatRequest(
        AiUser user,
        String message
) {
}
