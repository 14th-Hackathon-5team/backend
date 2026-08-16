package com.example.kbuddy.ai.dto;

import com.example.kbuddy.notification.entity.NotificationCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "AI Chat 응답")
public record AiChatApiResponse(
        @Schema(description = "AI 답변 내용")
        String answer,

        @Schema(description = "답변과 관련된 카테고리")
        NotificationCategory category,

        @Schema(description = "추가로 물어볼 만한 질문 목록")
        List<String> suggestedQuestions,

        @Schema(description = "답변의 근거가 된 출처 목록(가변 JSON, 없으면 빈 배열)")
        List<Map<String, Object>> sources
) {

    public static AiChatApiResponse from(AiChatResponse response) {
        return new AiChatApiResponse(
                response.answer(),
                response.category(),
                response.suggestedQuestions(),
                response.sources()
        );
    }
}
