package com.example.kbuddy.notification.service;

import com.example.kbuddy.auth.oauth.AuthProvider;
import com.example.kbuddy.global.exception.BusinessException;
import com.example.kbuddy.global.exception.ErrorCode;
import com.example.kbuddy.notification.entity.Notification;
import com.example.kbuddy.notification.entity.NotificationCategory;
import com.example.kbuddy.notification.entity.NotificationTriggerType;
import com.example.kbuddy.notification.repository.NotificationRepository;
import com.example.kbuddy.user.entity.HousingType;
import com.example.kbuddy.user.entity.Language;
import com.example.kbuddy.user.entity.PartTimeStatus;
import com.example.kbuddy.user.entity.TopikLevel;
import com.example.kbuddy.user.entity.User;
import com.example.kbuddy.user.entity.UserStatus;
import com.example.kbuddy.user.entity.VisaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository);
    }

    private User createUser() {
        return new User(
                AuthProvider.GOOGLE,
                "google-1",
                "student@example.com",
                "김철수",
                "CN",
                2000,
                UserStatus.UNDERGRADUATE,
                "서울대학교",
                LocalDate.of(2022, 3, 1),
                VisaType.D2,
                true,
                LocalDate.of(2026, 9, 30),
                HousingType.DORMITORY,
                false,
                PartTimeStatus.SEARCHING,
                TopikLevel.LEVEL_3,
                TopikLevel.LEVEL_5,
                Language.KOREAN
        );
    }

    @Test
    void create를_호출하면_Notification을_생성해서_저장하고_반환한다() {
        User user = createUser();
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Notification result = notificationService.create(
                user,
                NotificationCategory.VISA,
                "체류기간 만료 30일 전입니다",
                "현재 비자가 D-2입니다.",
                "체류기간 만료 전에 연장 절차를 준비하세요.",
                Map.of("visaType", "D2"),
                Map.of("dataset", "law"),
                5,
                NotificationTriggerType.VISA_EXPIRATION,
                LocalDate.of(2026, 8, 31)
        );

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();

        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getCategory()).isEqualTo(NotificationCategory.VISA);
        assertThat(saved.getTitle()).isEqualTo("체류기간 만료 30일 전입니다");
        assertThat(saved.getPriority()).isEqualTo(5);
        assertThat(saved.getTriggerType()).isEqualTo(NotificationTriggerType.VISA_EXPIRATION);
        assertThat(saved.getTriggerDate()).isEqualTo(LocalDate.of(2026, 8, 31));
        assertThat(saved.getIsRead()).isFalse();
        assertThat(result).isSameAs(saved);
    }

    // ---------- STEP D: getMyNotifications(Long userId) ----------

    @Test
    void getMyNotifications를_호출하면_해당_User의_Notification을_최신순으로_반환한다() {
        User user = createUser();
        Notification notification = new Notification(
                user, NotificationCategory.VISA, "제목", "이유", "요약",
                Map.of("k", "v"), null, 4, NotificationTriggerType.VISA_EXPIRATION, LocalDate.of(2026, 8, 17));
        when(notificationRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(1L))
                .thenReturn(List.of(notification));

        List<com.example.kbuddy.notification.dto.NotificationResponse> result = notificationService.getMyNotifications(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).category()).isEqualTo(NotificationCategory.VISA);
        assertThat(result.get(0).title()).isEqualTo("제목");
        assertThat(result.get(0).priority()).isEqualTo(4);
    }

    @Test
    void getMyNotifications는_Repository의_findAllByUserIdOrderByCreatedAtDescIdDesc를_호출한다() {
        when(notificationRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(1L)).thenReturn(List.of());

        notificationService.getMyNotifications(1L);

        verify(notificationRepository).findAllByUserIdOrderByCreatedAtDescIdDesc(1L);
    }

    // ---------- STEP D: markAsRead(Long userId, Long notificationId) — ownership 검증 ----------

    @Test
    void userId와_notificationId로_markAsRead를_호출하면_소유자의_Notification만_읽음_처리한다() {
        User user = createUser();
        Notification notification = new Notification(
                user, NotificationCategory.LEGAL, "제목", "이유", "요약",
                Map.of("k", "v"), null, 3, NotificationTriggerType.ALIEN_REGISTRATION, LocalDate.of(2026, 8, 17));
        when(notificationRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(notification));

        com.example.kbuddy.notification.dto.NotificationResponse response = notificationService.markAsRead(1L, 10L);

        assertThat(response.isRead()).isTrue();
        assertThat(response.readAt()).isNotNull();
    }

    @Test
    void 다른_User의_Notification을_userId_기준으로_조회하면_존재하지_않아_NOTIFICATION_NOT_FOUND가_발생한다() {
        // Repository가 (notificationId, userId) 복합 조건으로 조회하므로,
        // 다른 User 소유의 Notification은 findByIdAndUserId에서 애초에 Optional.empty()가 된다.
        when(notificationRepository.findByIdAndUserId(10L, 999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(999L, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @Test
    void 존재하지_않는_notificationId를_userId_기준으로_읽음_처리하면_NOTIFICATION_NOT_FOUND가_발생한다() {
        when(notificationRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @Test
    void 이미_읽은_Notification을_userId_기준으로_다시_읽음_처리해도_기존_readAt이_유지된다() {
        User user = createUser();
        Notification notification = new Notification(
                user, NotificationCategory.LEGAL, "제목", "이유", "요약",
                Map.of("k", "v"), null, 3, NotificationTriggerType.ALIEN_REGISTRATION, LocalDate.of(2026, 8, 17));
        notification.markAsRead();
        var firstReadAt = notification.getReadAt();
        when(notificationRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(notification));

        com.example.kbuddy.notification.dto.NotificationResponse response = notificationService.markAsRead(1L, 10L);

        assertThat(response.isRead()).isTrue();
        assertThat(response.readAt()).isEqualTo(firstReadAt);
    }
}
