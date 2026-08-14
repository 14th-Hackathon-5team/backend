package com.example.kbuddy.user.controller;

import com.example.kbuddy.global.response.ApiResponse;
import com.example.kbuddy.user.dto.AlarmSettingUpdateRequest;
import com.example.kbuddy.user.dto.LanguageSettingUpdateRequest;
import com.example.kbuddy.user.dto.UserSettingsResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "설정", description = "앱 표시 언어와 푸시 알림 설정을 관리하는 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/settings/me")
public class UserSettingsController {

    private final UserService userService;

    @Operation(
            summary = "설정 정보 조회",
            description = "현재 로그인한 사용자의 앱 화면 표시 언어, 알림 수신 설정, 계정 상태를 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공 (code: USER_SETTINGS_FETCHED)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 사용자 (code: USER_NOT_FOUND)"
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<ApiResponse<UserSettingsResponse>> getSettings(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        UserSettingsResponse response = userService.getSettings(userId);
        return ResponseEntity.ok(
                ApiResponse.success("USER_SETTINGS_FETCHED", "설정 정보를 조회했습니다.", response)
        );
    }

    @Operation(
            summary = "언어 설정 변경",
            description = "현재 로그인한 사용자의 앱 화면 표시 언어를 변경합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/language")
    public ResponseEntity<ApiResponse<UserSettingsResponse>> updateLanguage(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody LanguageSettingUpdateRequest request
    ) {
        Long userId = Long.valueOf(jwt.getSubject());
        UserSettingsResponse response = userService.updateLanguage(userId, request.language());
        return ResponseEntity.ok(
                ApiResponse.success("USER_LANGUAGE_SETTING_UPDATED", "언어 설정을 변경했습니다.", response)
        );
    }

    @Operation(
            summary = "알림 설정 변경",
            description = "현재 로그인한 사용자의 푸시 알림 수신 범위를 변경합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/alarm")
    public ResponseEntity<ApiResponse<UserSettingsResponse>> updateAlarmSetting(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AlarmSettingUpdateRequest request
    ) {
        Long userId = Long.valueOf(jwt.getSubject());
        UserSettingsResponse response = userService.updateAlarmSetting(userId, request.alarmSetting());
        return ResponseEntity.ok(
                ApiResponse.success("USER_ALARM_SETTING_UPDATED", "알림 설정을 변경했습니다.", response)
        );
    }
}
