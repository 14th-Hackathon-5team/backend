package com.example.kbuddy.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원가입 성공 응답")
public record UserSignupResponse(
        @Schema(description = "회원가입 완료 후 발급된 ACCESS_TOKEN")
        String accessToken
) {
}
