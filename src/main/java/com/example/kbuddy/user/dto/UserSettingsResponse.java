package com.example.kbuddy.user.dto;

import com.example.kbuddy.user.entity.AccountState;
import com.example.kbuddy.user.entity.AlarmSetting;
import com.example.kbuddy.user.entity.Language;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 앱 설정 정보 응답")
public record UserSettingsResponse(
        @Schema(description = "앱 화면 표시 언어", example = "KOREAN")
        Language language,

        @Schema(description = "푸시 알림 수신 설정", example = "ALL")
        AlarmSetting alarmSetting,

        @Schema(description = "계정 상태", example = "ACTIVE")
        AccountState accountState
) {
}
