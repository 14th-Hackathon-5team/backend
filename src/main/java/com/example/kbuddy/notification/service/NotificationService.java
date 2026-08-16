package com.example.kbuddy.notification.service;

import com.example.kbuddy.global.exception.BusinessException;
import com.example.kbuddy.global.exception.ErrorCode;
import com.example.kbuddy.notification.dto.NotificationResponse;
import com.example.kbuddy.notification.entity.Notification;
import com.example.kbuddy.notification.entity.NotificationCategory;
import com.example.kbuddy.notification.entity.NotificationTriggerType;
import com.example.kbuddy.notification.repository.NotificationRepository;
import com.example.kbuddy.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public Notification create(
            User user,
            NotificationCategory category,
            String title,
            String reason,
            String summary,
            Map<String, Object> details,
            Map<String, Object> source,
            Integer priority,
            NotificationTriggerType triggerType,
            LocalDate triggerDate
    ) {
        Notification notification = new Notification(
                user,
                category,
                title,
                reason,
                summary,
                details,
                source,
                priority,
                triggerType,
                triggerDate
        );

        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(Long userId) {
        return notificationRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(userId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    /**
     * 현재 로그인한 User(userId) 소유의 Notification만 읽음 처리한다.
     * 조회 조건에 userId를 함께 걸어 다른 User의 Notification을 조작할 수 없도록 한다.
     * 다른 User의 Notification이거나 존재하지 않으면 동일하게 NOTIFICATION_NOT_FOUND로 처리해
     * 존재 여부를 외부에 노출하지 않는다.
     */
    @Transactional
    public NotificationResponse markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        notification.markAsRead();
        return NotificationResponse.from(notification);
    }
}
