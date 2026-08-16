package com.example.kbuddy.ai.service;

import com.example.kbuddy.ai.client.AiClient;
import com.example.kbuddy.ai.dto.AiChatRequest;
import com.example.kbuddy.ai.dto.AiChatResponse;
import com.example.kbuddy.ai.dto.AiRecommendationRequest;
import com.example.kbuddy.ai.dto.AiRecommendationResponse;
import com.example.kbuddy.ai.dto.AiTrigger;
import com.example.kbuddy.ai.dto.AiUser;
import com.example.kbuddy.notification.entity.NotificationCategory;
import com.example.kbuddy.user.entity.HousingType;
import com.example.kbuddy.user.entity.Language;
import com.example.kbuddy.user.entity.PartTimeStatus;
import com.example.kbuddy.user.entity.TopikLevel;
import com.example.kbuddy.user.entity.UserStatus;
import com.example.kbuddy.user.entity.VisaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiServiceTest {

    @Mock
    private AiClient aiClient;

    private AiService aiService;

    @BeforeEach
    void setUp() {
        aiService = new AiService(aiClient);
    }

    private AiUser sampleUser() {
        return new AiUser(
                1L,
                "Vietnam",
                2003,
                UserStatus.UNDERGRADUATE,
                "Kyungpook National University",
                LocalDate.of(2026, 2, 20),
                VisaType.D2,
                true,
                LocalDate.of(2027, 2, 19),
                HousingType.DORMITORY,
                true,
                PartTimeStatus.NOT_PLANNED,
                TopikLevel.LEVEL_3,
                TopikLevel.LEVEL_5,
                Language.KOREAN
        );
    }

    @Test
    void recommendations를_호출하면_AiClient의_recommendations에_그대로_위임한다() {
        AiRecommendationRequest request = new AiRecommendationRequest(sampleUser(), new AiTrigger("VISA_EXPIRATION", 30));
        AiRecommendationResponse expected = new AiRecommendationResponse(1L, List.of());
        when(aiClient.recommendations(request)).thenReturn(expected);

        AiRecommendationResponse result = aiService.recommendations(request);

        assertThat(result).isSameAs(expected);
        verify(aiClient).recommendations(request);
    }

    @Test
    void chat을_호출하면_AiClient의_chat에_그대로_위임한다() {
        AiChatRequest request = new AiChatRequest(sampleUser(), "질문");
        AiChatResponse expected = new AiChatResponse("답변", NotificationCategory.SUPPORT, List.of(), List.of());
        when(aiClient.chat(request)).thenReturn(expected);

        AiChatResponse result = aiService.chat(request);

        assertThat(result).isSameAs(expected);
        verify(aiClient).chat(request);
    }
}
