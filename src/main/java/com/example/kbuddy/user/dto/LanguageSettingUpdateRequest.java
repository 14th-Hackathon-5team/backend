package com.example.kbuddy.user.dto;

import com.example.kbuddy.user.entity.Language;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "앱 화면 표시 언어 변경 요청")
public record LanguageSettingUpdateRequest(
        @Schema(description = "변경할 앱 화면 표시 언어", example = "ENGLISH", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Language language
) {
}
