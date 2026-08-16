package com.example.kbuddy.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(
        description = "AI Chat 요청. 사용자 정보는 인증된 로그인 사용자 기준으로 Backend가 채우므로 message만 전달합니다.",
        example = """
                {
                  "message": "D-2 비자인데 아르바이트를 시작하려면 어떻게 해야 해?"
                }
                """
)
public record AiChatApiRequest(
        @Schema(description = "AI에게 보낼 질문 메시지", example = "D-2 비자인데 아르바이트를 시작하려면 어떻게 해야 해?")
        @NotBlank
        String message
) {
}
