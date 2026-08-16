package com.example.kbuddy.notification.dto;

import com.example.kbuddy.notification.entity.Notification;
import com.example.kbuddy.notification.entity.NotificationCategory;
import com.example.kbuddy.notification.entity.NotificationTriggerType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "AI 맞춤 알림 응답")
public record NotificationResponse(
        @Schema(description = "알림 ID", example = "1")
        Long notificationId,

        @Schema(description = "알림 카테고리")
        NotificationCategory category,

        @Schema(description = "알림 제목", example = "체류기간 만료 30일 전입니다")
        String title,

        @Schema(description = "AI가 판단한 추천 이유", example = "현재 비자가 D-2입니다.")
        String reason,

        @Schema(description = "사용자에게 표시할 핵심 안내 내용")
        String summary,

        @Schema(description = "상세 정보(가변 JSON)")
        Map<String, Object> details,

        @Schema(description = "정보 출처(가변 JSON, 없으면 null)")
        Map<String, Object> source,

        @Schema(description = "중요도 (1~5, 높을수록 중요)", example = "5")
        Integer priority,

        @Schema(description = "알림이 발생한 트리거 종류")
        NotificationTriggerType triggerType,

        @Schema(description = "트리거가 발생한 기준 날짜", example = "2026-08-31")
        LocalDate triggerDate,

        @Schema(description = "읽음 여부", example = "false")
        Boolean isRead,

        @Schema(description = "읽은 시각(읽지 않았으면 null)")
        LocalDateTime readAt,

        @Schema(description = "알림 생성 시각")
        LocalDateTime createdAt
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getCategory(),
                notification.getTitle(),
                notification.getReason(),
                notification.getSummary(),
                notification.getDetails(),
                notification.getSource(),
                notification.getPriority(),
                notification.getTriggerType(),
                notification.getTriggerDate(),
                notification.getIsRead(),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }
}
