package com.example.kbuddy.notification.controller;

import com.example.kbuddy.global.response.ApiResponse;
import com.example.kbuddy.notification.dto.NotificationResponse;
import com.example.kbuddy.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "알림", description = "AI 맞춤 알림을 조회하고 읽음 처리하는 API")
@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(
            summary = "내 알림 목록 조회",
            description = "현재 로그인한 사용자의 알림 목록을 최신순(createdAt DESC)으로 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공 (code: NOTIFICATION_LIST_FETCHED)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 요청(토큰 없음/만료/서명 오류 등)"
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.valueOf(jwt.getSubject());
        List<NotificationResponse> response = notificationService.getMyNotifications(userId);
        return ResponseEntity.ok(
                ApiResponse.success("NOTIFICATION_LIST_FETCHED", "알림 목록을 조회했습니다.", response)
        );
    }

    @Operation(
            summary = "알림 읽음 처리",
            description = "현재 로그인한 사용자 소유의 알림을 읽음 처리합니다. 다른 사용자의 알림이거나 존재하지 않는 알림은 "
                    + "동일하게 NOTIFICATION_NOT_FOUND로 처리됩니다. 이미 읽은 알림을 다시 요청해도 성공하며 기존 읽은 시각이 유지됩니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "읽음 처리 성공 (code: NOTIFICATION_READ)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 요청(토큰 없음/만료/서명 오류 등)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않거나 다른 사용자 소유의 알림 (code: NOTIFICATION_NOT_FOUND)"
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/notifications/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long notificationId
    ) {
        Long userId = Long.valueOf(jwt.getSubject());
        NotificationResponse response = notificationService.markAsRead(userId, notificationId);
        return ResponseEntity.ok(
                ApiResponse.success("NOTIFICATION_READ", "알림을 읽음 처리했습니다.", response)
        );
    }
}
