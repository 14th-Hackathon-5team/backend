package com.example.kbuddy.ai.controller;

import com.example.kbuddy.ai.dto.AiChatApiRequest;
import com.example.kbuddy.ai.dto.AiChatApiResponse;
import com.example.kbuddy.ai.dto.AiChatRequest;
import com.example.kbuddy.ai.dto.AiChatResponse;
import com.example.kbuddy.ai.dto.AiUser;
import com.example.kbuddy.ai.service.AiService;
import com.example.kbuddy.ai.service.AiUserMapper;
import com.example.kbuddy.global.response.ApiResponse;
import com.example.kbuddy.user.dto.UserResponse;
import com.example.kbuddy.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI", description = "현재 로그인한 사용자 정보를 기반으로 AI와 대화하는 API")
@RestController
@RequiredArgsConstructor
public class AiController {

    private final UserService userService;
    private final AiUserMapper aiUserMapper;
    private final AiService aiService;

    @Operation(
            summary = "AI Chat",
            description = "현재 로그인한 사용자의 프로필 정보를 Backend가 구성하여 FastAPI Chat API에 전달합니다. "
                    + "요청 본문에는 message만 포함하며, 사용자 정보를 직접 보낼 필요가 없습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "AI 응답 성공 (code: AI_CHAT_RESPONDED)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 값 검증 실패(code: INVALID_REQUEST, 예: message가 비어 있음)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 요청(토큰 없음/만료/서명 오류 등)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 사용자 (code: USER_NOT_FOUND)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "503",
                    description = "AI 서비스 통신 실패(FastAPI timeout/네트워크 오류/5xx/응답 형식 오류) (code: AI_SERVICE_UNAVAILABLE)"
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/ai/chat")
    public ResponseEntity<ApiResponse<AiChatApiResponse>> chat(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AiChatApiRequest request
    ) {
        Long userId = Long.valueOf(jwt.getSubject());
        UserResponse currentUser = userService.getMyInfo(userId);
        AiUser aiUser = aiUserMapper.toAiUser(currentUser);

        AiChatResponse chatResponse = aiService.chat(new AiChatRequest(aiUser, request.message()));

        return ResponseEntity.ok(
                ApiResponse.success("AI_CHAT_RESPONDED", "AI 답변을 받았습니다.", AiChatApiResponse.from(chatResponse))
        );
    }
}
