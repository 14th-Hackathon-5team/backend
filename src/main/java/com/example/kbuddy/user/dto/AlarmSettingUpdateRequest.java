package com.example.kbuddy.user.dto;

import com.example.kbuddy.user.entity.AlarmSetting;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "푸시 알림 수신 설정 변경 요청")
public record AlarmSettingUpdateRequest(
        @Schema(description = "변경할 푸시 알림 수신 설정", example = "ESSENTAL_ONLY", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        AlarmSetting alarmSetting
) {
}
