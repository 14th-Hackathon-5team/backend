package com.example.kbuddy.ai.service;

import com.example.kbuddy.ai.client.AiClient;
import com.example.kbuddy.ai.dto.AiChatRequest;
import com.example.kbuddy.ai.dto.AiChatResponse;
import com.example.kbuddy.ai.dto.AiRecommendationRequest;
import com.example.kbuddy.ai.dto.AiRecommendationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiService {

    private final AiClient aiClient;

    public AiRecommendationResponse recommendations(AiRecommendationRequest request) {
        return aiClient.recommendations(request);
    }

    public AiChatResponse chat(AiChatRequest request) {
        return aiClient.chat(request);
    }
}
