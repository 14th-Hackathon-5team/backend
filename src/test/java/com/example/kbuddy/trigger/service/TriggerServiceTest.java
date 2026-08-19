package com.example.kbuddy.trigger.service;

import com.example.kbuddy.ai.client.AiClientException;
import com.example.kbuddy.ai.dto.AiRecommendation;
import com.example.kbuddy.ai.dto.AiRecommendationPriority;
import com.example.kbuddy.ai.dto.AiRecommendationRequest;
import com.example.kbuddy.ai.dto.AiRecommendationResponse;
import com.example.kbuddy.ai.dto.AiRecommendationType;
import com.example.kbuddy.ai.dto.AiUser;
import com.example.kbuddy.ai.service.AiService;
import com.example.kbuddy.auth.oauth.AuthProvider;
import com.example.kbuddy.calendar.entity.CalendarEvent;
import com.example.kbuddy.calendar.entity.EventCategory;
import com.example.kbuddy.calendar.repository.CalendarEventRepository;
import com.example.kbuddy.calendar.service.CalendarEventService;
import com.example.kbuddy.notification.entity.Notification;
import com.example.kbuddy.notification.entity.NotificationCategory;
import com.example.kbuddy.notification.entity.NotificationTriggerType;
import com.example.kbuddy.notification.repository.NotificationRepository;
import com.example.kbuddy.notification.service.EmailNotificationService;
import com.example.kbuddy.notification.service.NotificationService;
import com.example.kbuddy.user.entity.AlarmSetting;
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

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TriggerServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 17);
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-08-17T00:00:00Z"), ZoneOffset.UTC);

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private EmailNotificationService emailNotificationService;

    @Mock
    private AiService aiService;

    @Mock
    private CalendarEventRepository calendarEventRepository;

    @Mock
    private CalendarEventService calendarEventService;

    private TriggerService triggerService;

    @BeforeEach
    void setUp() {
        triggerService = new TriggerService(
                notificationRepository, notificationService, emailNotificationService, aiService,
                calendarEventRepository, calendarEventService, FIXED_CLOCK);
    }

    /**
     * 다른 조건에 영향을 주지 않는 "중립" User. 각 테스트는 필요한 필드만 오버라이드한다.
     * hasAlienRegistration=true, stayExpirationDate=먼 미래로 두 트리거 모두 기본적으로 발생하지 않게 한다.
     */
    private User neutralUser(
            LocalDate entryDate,
            Boolean hasAlienRegistration,
            LocalDate stayExpirationDate,
            VisaType visaType,
            PartTimeStatus partTimeStatus,
            AlarmSetting alarmSetting
    ) {
        User user = new User(
                AuthProvider.GOOGLE,
                "google-1",
                "student@example.com",
                "김철수",
                "VN",
                2000,
                UserStatus.UNDERGRADUATE,
                "경북대학교",
                entryDate,
                visaType,
                hasAlienRegistration,
                stayExpirationDate,
                HousingType.DORMITORY,
                false,
                partTimeStatus,
                TopikLevel.LEVEL_3,
                TopikLevel.LEVEL_5,
                Language.KOREAN
        );
        user.updateAlarmSetting(alarmSetting);
        setUserId(user, 1L);
        return user;
    }

    private void setUserId(User user, Long id) {
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private CalendarEvent topikEvent(Long id, EventCategory category, LocalDate startDate, LocalDate endDate) {
        CalendarEvent event = new CalendarEvent(null, "TOPIK 일정", category, startDate, endDate, true, "설명", "https://www.topik.go.kr");
        try {
            var field = CalendarEvent.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(event, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return event;
    }

    private AiRecommendation recommendation(AiRecommendationType type, String title, AiRecommendationPriority priority, String category) {
        return new AiRecommendation(type, priority, title, "이유", Map.of("category", category, "k", "v"));
    }

    // ---------- 1, 2: BEFORE_ENTRY 판단 ----------

    @Test
    void entryDate가_미래이면_BEFORE_ENTRY_조건에_해당한다() {
        User user = neutralUser(TODAY.plusDays(5), true, TODAY.plusYears(1), VisaType.OTHER, PartTimeStatus.NOT_PLANNED, AlarmSetting.ALL);
        assertThat(triggerService.isBeforeEntry(user, TODAY)).isTrue();
    }

    @Test
    void entryDate가_과거이면_BEFORE_ENTRY_조건이_아니다() {
        User user = neutralUser(TODAY.minusDays(5), true, TODAY.plusYears(1), VisaType.OTHER, PartTimeStatus.NOT_PLANNED, AlarmSetting.ALL);
        assertThat(triggerService.isBeforeEntry(user, TODAY)).isFalse();
    }

    // ---------- 3, 4: ALIEN_REGISTRATION 판단 ----------

    @Test
    void hasAlienRegistration이_false이고_이미_입국했으면_ALIEN_REGISTRATION_후보다() {
        User user = neutralUser(TODAY.minusDays(10), false, TODAY.plusYears(1), VisaType.OTHER, PartTimeStatus.NOT_PLANNED, AlarmSetting.ALL);
        assertThat(triggerService.isAlienRegistrationCandidate(user, TODAY)).isTrue();
    }

    @Test
    void hasAlienRegistration이_true이면_ALIEN_REGISTRATION_후보가_아니다() {
        User user = neutralUser(TODAY.minusDays(10), true, TODAY.plusYears(1), VisaType.OTHER, PartTimeStatus.NOT_PLANNED, AlarmSetting.ALL);
        assertThat(triggerService.isAlienRegistrationCandidate(user, TODAY)).isFalse();
    }

    @Test
    void hasAlienRegistration이_false여도_아직_입국_전이면_ALIEN_REGISTRATION_후보가_아니다() {
        User user = neutralUser(TODAY.plusDays(10), false, TODAY.plusYears(1), VisaType.OTHER, PartTimeStatus.NOT_PLANNED, AlarmSetting.ALL);
        assertThat(triggerService.isAlienRegistrationCandidate(user, TODAY)).isFalse();
    }

    // ---------- 5, 6: VISA_EXPIRATION 판단 ----------

    @Test
    void stayExpirationDate가_임계값_30일_이내면_VISA_EXPIRATION_후보다() {
        User user = neutralUser(TODAY.minusYears(1), true, TODAY.plusDays(30), VisaType.OTHER, PartTimeStatus.NOT_PLANNED, AlarmSetting.ALL);
        assertThat(triggerService.isVisaExpirationCandidate(user, TODAY)).isTrue();
    }

    @Test
    void stayExpirationDate가_충분히_먼_미래면_VISA_EXPIRATION_후보가_아니다() {
        User user = neutralUser(TODAY.minusYears(1), true, TODAY.plusDays(31), VisaType.OTHER, PartTimeStatus.NOT_PLANNED, AlarmSetting.ALL);
        assertThat(triggerService.isVisaExpirationCandidate(user, TODAY)).isFalse();
    }

    @Test
    void stayExpirationDate가_이미_지났으면_VISA_EXPIRATION_후보가_아니다() {
        User user = neutralUser(TODAY.minusYears(1), true, TODAY.minusDays(1), VisaType.OTHER, PartTimeStatus.NOT_PLANNED, AlarmSetting.ALL);
        assertThat(triggerService.isVisaExpirationCandidate(user, TODAY)).isFalse();
    }

    // ---------- 7: VISA(D2/D4) 판단 ----------

    @Test
    void visaType이_D2이거나_D4이면_VISA_후보다() {
        User d2 = neutralUser(TODAY.minusYears(1), true, TODAY.plusYears(1), VisaType.D2, PartTimeStatus.NOT_PLANNED, AlarmSetting.ALL);
        User d4 = neutralUser(TODAY.minusYears(1), true, TODAY.plusYears(1), VisaType.D4, PartTimeStatus.NOT_PLANNED, AlarmSetting.ALL);
        User other = neutralUser(TODAY.minusYears(1), true, TODAY.plusYears(1), VisaType.H1, PartTimeStatus.NOT_PLANNED, AlarmSetting.ALL);

        assertThat(triggerService.isVisaCandidate(d2)).isTrue();
        assertThat(triggerService.isVisaCandidate(d4)).isTrue();
        assertThat(triggerService.isVisaCandidate(other)).isFalse();
    }

    // ---------- 8, 9: PART_TIME 판단 ----------

    @Test
    void partTimeStatus가_SEARCHING이거나_WORKING이면_PART_TIME_후보다() {
        User searching = neutralUser(TODAY.minusYears(1), true, TODAY.plusYears(1), VisaType.OTHER, PartTimeStatus.SEARCHING, AlarmSetting.ALL);
        User working = neutralUser(TODAY.minusYears(1), true, TODAY.plusYears(1), VisaType.OTHER, PartTimeStatus.WORKING, AlarmSetting.ALL);

        assertThat(triggerService.isPartTimeCandidate(searching)).isTrue();
        assertThat(triggerService.isPartTimeCandidate(working)).isTrue();
    }

    @Test
    void partTimeStatus가_NOT_PLANNED면_PART_TIME_후보가_아니다() {
        User user = neutralUser(TODAY.minusYears(1), true, TODAY.plusYears(1), VisaType.OTHER, PartTimeStatus.NOT_PLANNED, AlarmSetting.ALL);
        assertThat(triggerService.isPartTimeCandidate(user)).isFalse();
    }

    // ---------- 10~13, 14: AiUser 매핑 ----------

    @Test
    void toAiUser는_TOPIK_housingType_isParentSupported_userStatus_schoolName_nationality를_정확히_매핑한다() {
        User user = neutralUser(TODAY.minusYears(1), true, TODAY.plusYears(1), VisaType.OTHER, PartTimeStatus.NOT_PLANNED, AlarmSetting.ALL);

        AiUser aiUser = triggerService.toAiUser(user);

        assertThat(aiUser.currentTopikLevel()).isEqualTo(TopikLevel.LEVEL_3);
        assertThat(aiUser.targetTopikLevel()).isEqualTo(TopikLevel.LEVEL_5);
        assertThat(aiUser.housingType()).isEqualTo(HousingType.DORMITORY);
        assertThat(aiUser.isParentSupported()).isFalse();
        assertThat(aiUser.userStatus()).isEqualTo(UserStatus.UNDERGRADUATE);
        assertThat(aiUser.schoolName()).isEqualTo("경북대학교");
        // nationality는 어떤 변환도 없이 원본 ISO 코드값이 그대로 전달되어야 한다.
        assertThat(aiUser.nationality()).isEqualTo("VN");
        // User Entity에 아직 없는 필드라 매핑 데이터가 없으므로 항상 null이어야 한다.
        assertThat(aiUser.partTimeStartDate()).isNull();
        assertThat(aiUser.hasPartTimePermit()).isNull();
    }

    // ---------- 15, 16: AlarmSetting NONE/ALL ----------

    @Test
    void alarmSetting이_NONE이면_AiService를_호출하지_않는다() {
        User user = neutralUser(TODAY.minusDays(10), false, TODAY.plusYears(1), VisaType.OTHER, PartTimeStatus.NOT_PLANNED, AlarmSetting.NONE);

        triggerService.processUser(user);

        verifyNoInteractions(aiService, notificationService, emailNotificationService);
    }

    @Test
    void alarmSetting이_ALL이면_해당하는_Trigger에_대해_AiService를_호출한다() {
        User user = neutralUser(TODAY.minusDays(10), false, TODAY.plusYears(1), VisaType.OTHER, PartTimeStatus.NOT_PLANNED, AlarmSetting.ALL);
        when(notificationRepository.existsByUserAndTriggerTypeAndTriggerDate(any(), any(), any())).thenReturn(false);
        when(aiService.recommendations(any())).thenReturn(new AiRecommendationResponse(1L, "요약", java.util.List.of()));

        triggerService.processUser(user);

        verify(aiService).recommendations(any(AiRecommendationRequest.class));
    }

    // ---------- 17: ESSENTIAL_ONLY 정책 ----------

    @Test
    void ESSENTIAL_ONLY이고_최고_priority가_HIGH면_Notification을_생성한다() {
        User user = neutralUser(TODAY.minusDays(10), false, TODAY.plusYears(1), VisaType.OTHER, PartTimeStatus.NOT_PLANNED, AlarmSetting.ESSENTIAL_ONLY);
        when(notificationRepository.existsByUserAndTriggerTypeAndTriggerDate(any(), any(), any())).thenReturn(false);
        when(aiService.recommendations(any())).thenReturn(new AiRecommendationResponse(
                1L, "요약", java.util.List.of(recommendation(AiRecommendationType.LAW, "외국인등록 안내", AiRecommendationPriority.HIGH, "LEGAL"))));

        triggerService.processUser(user);

        verify(notificationService).create(eq(user), eq(NotificationCategory.LEGAL), any(), any(), any(), any(), any(), eq(5), eq(NotificationTriggerType.ALIEN_REGISTRATION), eq(TODAY));
    }

    @Test
    void ESSENTIAL_ONLY이고_최고_priority가_HIGH미만이면_Notification을_생성하지_않는다() {
        User user = neutralUser(TODAY.minusDays(10), false, TODAY.plusYears(1), VisaType.OTHER, PartTimeStatus.NOT_PLANNED, AlarmSetting.ESSENTIAL_ONLY);
        when(notificationRepository.existsByUserAndTriggerTypeAndTriggerDate(any(), any(), any())).thenReturn(false);
        when(aiService.recommendations(any())).thenReturn(new AiRecommendationResponse(
                1L, "요약", java.util.List.of(recommendation(AiRecommendationType.LAW, "외국인등록 안내", AiRecommendationPriority.MEDIUM, "LEGAL"))));

        triggerService.processUser(user);

        verify(notificationService, never()).create(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    // ---------- 18, 19: Recommendation 선택 ----------

    @Test
    void recommendations가_여러_개면_최고_priority_하나만_선택한다() {
        User user = neutralUser(TODAY.minusDays(10), false, TODAY.plusYears(1), VisaType.OTHER, PartTimeStatus.NOT_PLANNED, AlarmSetting.ALL);
        when(notificationRepository.existsByUserAndTriggerTypeAndTriggerDate(any(), any(), any())).thenReturn(false);
        when(aiService.recommendations(any())).thenReturn(new AiRecommendationResponse(1L, "요약", java.util.List.of(
                recommendation(AiRecommendationType.LAW, "중간순위", AiRecommendationPriority.MEDIUM, "LEGAL"),
                recommendation(AiRecommendationType.UNIVERSITY, "최고순위", AiRecommendationPriority.HIGH, "ADMISSION"),
                recommendation(AiRecommendationType.LAW, "최저순위", AiRecommendationPriority.LOW, "VISA")
        )));

        triggerService.processUser(user);

        ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).create(eq(user), eq(NotificationCategory.ADMISSION), titleCaptor.capture(), any(), any(), any(), any(), eq(5), any(), any());
        assertThat(titleCaptor.getValue()).isEqualTo("최고순위");
    }

    @Test
    void 동일_priority면_먼저_나온_항목을_선택한다() {
        User user = neutralUser(TODAY.minusDays(10), false, TODAY.plusYears(1), VisaType.OTHER, PartTimeStatus.NOT_PLANNED, AlarmSetting.ALL);
        when(notificationRepository.existsByUserAndTriggerTypeAndTriggerDate(any(), any(), any())).thenReturn(false);
        when(aiService.recommendations(any())).thenReturn(new AiRecommendationResponse(1L, "요약", java.util.List.of(
                recommendation(AiRecommendationType.LAW, "먼저", AiRecommendationPriority.HIGH, "LEGAL"),
                recommendation(AiRecommendationType.UNIVERSITY, "나중", AiRecommendationPriority.HIGH, "ADMISSION")
        )));

        triggerService.processUser(user);

        ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).create(eq(user), eq(NotificationCategory.LEGAL), titleCaptor.capture(), any(), any(), any(), any(), eq(5), any(), any());
        assertThat(titleCaptor.getValue()).isEqualTo("먼저");
    }

    @Test
    void recommendations가_빈_배열이면_Notification을_생성하지_않는다() {
        User user = neutralUser(TODAY.minusDays(10), false, TODAY.plusYears(1), VisaType.OTHER, PartTimeStatus.NOT_PLANNED, AlarmSetting.ALL);
        when(notificationRepository.existsByUserAndTriggerTypeAndTriggerDate(any(), any(), any())).thenReturn(false);
        when(aiService.recommendations(any())).thenReturn(new AiRecommendationResponse(1L, "요약", java.util.List.of()));

        triggerService.processUser(user);

        verify(notificationService, never()).create(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    // ---------- 21: 중복 방지 ----------

    @Test
    void 동일_User_triggerType_triggerDate_Notification이_이미_있으면_저장하지_않고_AI도_호출하지_않는다() {
        User user = neutralUser(TODAY.minusDays(10), false, TODAY.plusYears(1), VisaType.OTHER, PartTimeStatus.NOT_PLANNED, AlarmSetting.ALL);
        when(notificationRepository.existsByUserAndTriggerTypeAndTriggerDate(user, NotificationTriggerType.ALIEN_REGISTRATION, TODAY))
                .thenReturn(true);

        triggerService.processUser(user);

        verifyNoInteractions(aiService);
        verify(notificationService, never()).create(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    // ---------- 22: Notification 매핑 ----------

    @Test
    void 선택된_Recommendation의_필드와_Response_summary가_Notification_생성에_정확히_매핑된다() {
        User user = neutralUser(TODAY.minusDays(10), false, TODAY.plusYears(1), VisaType.OTHER, PartTimeStatus.NOT_PLANNED, AlarmSetting.ALL);
        when(notificationRepository.existsByUserAndTriggerTypeAndTriggerDate(any(), any(), any())).thenReturn(false);
        Map<String, Object> detail = Map.of("category", "LEGAL", "deadline", "2026-09-20");
        when(aiService.recommendations(any())).thenReturn(new AiRecommendationResponse(1L, "요약입니다", java.util.List.of(
                new AiRecommendation(AiRecommendationType.LAW, AiRecommendationPriority.HIGH, "외국인등록 안내", "이유입니다", detail)
        )));
        when(notificationService.create(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(org.mockito.Mockito.mock(Notification.class));

        triggerService.processUser(user);

        // AiRecommendation에는 더 이상 summary/source가 없으므로, summary는 Response 최상위 summary를 사용하고
        // source는 항상 null로 전달한다(Notification 엔티티의 source 컬럼은 nullable).
        verify(notificationService).create(
                user, NotificationCategory.LEGAL, "외국인등록 안내", "이유입니다", "요약입니다",
                detail, null, 5, NotificationTriggerType.ALIEN_REGISTRATION, TODAY);
    }

    // ---------- 24: NotificationCategory 매핑 (detail.category 기반, type 기반 임의 매핑 아님) ----------

    @Test
    void LAW_추천의_detail_category가_VISA면_NotificationCategory_VISA로_변환된다() {
        User user = neutralUser(TODAY.minusDays(10), false, TODAY.plusYears(1), VisaType.OTHER, PartTimeStatus.NOT_PLANNED, AlarmSetting.ALL);
        when(notificationRepository.existsByUserAndTriggerTypeAndTriggerDate(any(), any(), any())).thenReturn(false);
        when(aiService.recommendations(any())).thenReturn(new AiRecommendationResponse(1L, "요약", java.util.List.of(
                recommendation(AiRecommendationType.LAW, "체류기간 연장 신청을 확인하세요", AiRecommendationPriority.HIGH, "VISA"))));

        triggerService.processUser(user);

        verify(notificationService).create(eq(user), eq(NotificationCategory.VISA), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void UNIVERSITY_추천의_detail_category가_ADMISSION이면_NotificationCategory_ADMISSION으로_변환된다() {
        User user = neutralUser(TODAY.minusDays(10), false, TODAY.plusYears(1), VisaType.OTHER, PartTimeStatus.NOT_PLANNED, AlarmSetting.ALL);
        when(notificationRepository.existsByUserAndTriggerTypeAndTriggerDate(any(), any(), any())).thenReturn(false);
        when(aiService.recommendations(any())).thenReturn(new AiRecommendationResponse(1L, "요약", java.util.List.of(
                recommendation(AiRecommendationType.UNIVERSITY, "대학 지원 일정을 확인하세요", AiRecommendationPriority.HIGH, "ADMISSION"))));

        triggerService.processUser(user);

        verify(notificationService).create(eq(user), eq(NotificationCategory.ADMISSION), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void detail_category가_NotificationCategory에_없는_값이면_예외가_발생하고_Notification을_생성하지_않는다() {
        User user = neutralUser(TODAY.minusDays(10), false, TODAY.plusYears(1), VisaType.OTHER, PartTimeStatus.NOT_PLANNED, AlarmSetting.ALL);
        when(notificationRepository.existsByUserAndTriggerTypeAndTriggerDate(any(), any(), any())).thenReturn(false);
        when(aiService.recommendations(any())).thenReturn(new AiRecommendationResponse(1L, "요약", java.util.List.of(
                recommendation(AiRecommendationType.LAW, "제목", AiRecommendationPriority.HIGH, "UNKNOWN_CATEGORY"))));

        assertThatThrownBy(() -> triggerService.processUser(user))
                .isInstanceOf(IllegalArgumentException.class);

        verify(notificationService, never()).create(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void detail에_category가_없으면_예외가_발생하고_임의의_기본값으로_대체되지_않는다() {
        User user = neutralUser(TODAY.minusDays(10), false, TODAY.plusYears(1), VisaType.OTHER, PartTimeStatus.NOT_PLANNED, AlarmSetting.ALL);
        when(notificationRepository.existsByUserAndTriggerTypeAndTriggerDate(any(), any(), any())).thenReturn(false);
        when(aiService.recommendations(any())).thenReturn(new AiRecommendationResponse(1L, "요약", java.util.List.of(
                new AiRecommendation(AiRecommendationType.LAW, AiRecommendationPriority.HIGH, "제목", "이유", Map.of("k", "v")))));

        assertThatThrownBy(() -> triggerService.processUser(user))
                .isInstanceOf(IllegalStateException.class);

        verify(notificationService, never()).create(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    // ---------- 23: AI 오류가 전체를 중단시키지 않음 ----------

    @Test
    void AiClientException이_발생해도_processUser는_예외를_던지지_않는다() {
        User user = neutralUser(TODAY.minusDays(10), false, TODAY.plusYears(1), VisaType.OTHER, PartTimeStatus.NOT_PLANNED, AlarmSetting.ALL);
        when(notificationRepository.existsByUserAndTriggerTypeAndTriggerDate(any(), any(), any())).thenReturn(false);
        when(aiService.recommendations(any()))
                .thenThrow(new AiClientException(AiClientException.Reason.SERVER_UNAVAILABLE, "fail", new RuntimeException()));

        assertThatCode(() -> triggerService.processUser(user)).doesNotThrowAnyException();

        verify(notificationService, never()).create(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    // ---------- 이메일 발송 ----------

    @Test
    void Notification이_생성되면_생성된_Notification으로_이메일을_발송한다() {
        User user = neutralUser(TODAY.minusDays(10), false, TODAY.plusYears(1), VisaType.OTHER, PartTimeStatus.NOT_PLANNED, AlarmSetting.ALL);
        when(notificationRepository.existsByUserAndTriggerTypeAndTriggerDate(any(), any(), any())).thenReturn(false);
        when(aiService.recommendations(any())).thenReturn(new AiRecommendationResponse(1L, "요약", java.util.List.of(
                recommendation(AiRecommendationType.LAW, "외국인등록 안내", AiRecommendationPriority.HIGH, "LEGAL"))));
        Notification saved = org.mockito.Mockito.mock(Notification.class);
        when(notificationService.create(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(saved);

        triggerService.processUser(user);

        verify(emailNotificationService).send(user, saved);
    }

    @Test
    void Notification이_생성되지_않으면_이메일도_발송하지_않는다() {
        User user = neutralUser(TODAY.minusDays(10), false, TODAY.plusYears(1), VisaType.OTHER, PartTimeStatus.NOT_PLANNED, AlarmSetting.ALL);
        when(notificationRepository.existsByUserAndTriggerTypeAndTriggerDate(any(), any(), any())).thenReturn(false);
        when(aiService.recommendations(any())).thenReturn(new AiRecommendationResponse(1L, "요약", java.util.List.of()));

        triggerService.processUser(user);

        verifyNoInteractions(emailNotificationService);
    }

    // ---------- 체류만료 D-1 재알림 + 완료체크 skip ----------

    @Test
    void 체류만료가_D_1이고_캘린더에서_이미_완료체크했으면_AI를_호출하지_않고_Notification도_생성하지_않는다() {
        User user = neutralUser(TODAY.minusYears(1), true, TODAY.plusDays(1), VisaType.OTHER, PartTimeStatus.NOT_PLANNED, AlarmSetting.ALL);
        when(calendarEventService.isCompletedByUser(1L, CalendarEventService.VISA_EXPIRATION_REMINDER_EVENT_ID)).thenReturn(true);

        triggerService.processUser(user);

        verifyNoInteractions(aiService);
        verify(notificationService, never()).create(any(), any(), any(), any(), any(), any(), any(), any(), eq(NotificationTriggerType.VISA_EXPIRATION), any());
    }

    @Test
    void 체류만료가_D_1이고_아직_완료체크_안했으면_평소처럼_AI를_호출한다() {
        User user = neutralUser(TODAY.minusYears(1), true, TODAY.plusDays(1), VisaType.OTHER, PartTimeStatus.NOT_PLANNED, AlarmSetting.ALL);
        when(calendarEventService.isCompletedByUser(1L, CalendarEventService.VISA_EXPIRATION_REMINDER_EVENT_ID)).thenReturn(false);
        when(notificationRepository.existsByUserAndTriggerTypeAndTriggerDate(any(), any(), any())).thenReturn(false);
        when(aiService.recommendations(any())).thenReturn(new AiRecommendationResponse(1L, "요약", java.util.List.of()));

        triggerService.processUser(user);

        verify(aiService).recommendations(any(AiRecommendationRequest.class));
    }

    // ---------- TOPIK 접수/시험일 알림 ----------

    @Test
    void TOPIK_EXAM이_D_7이면_AI_호출_없이_고정_문구로_Notification을_생성하고_이메일을_발송한다() {
        User user = neutralUser(TODAY.minusYears(1), true, TODAY.plusYears(1), VisaType.OTHER, PartTimeStatus.NOT_PLANNED, AlarmSetting.ALL);
        CalendarEvent exam = topikEvent(100L, EventCategory.TOPIK_EXAM, TODAY.plusDays(7), null);
        when(calendarEventRepository.findByIsGlobalTrueAndCategory(EventCategory.TOPIK_APPLICATION)).thenReturn(java.util.List.of());
        when(calendarEventRepository.findByIsGlobalTrueAndCategory(EventCategory.TOPIK_EXAM)).thenReturn(java.util.List.of(exam));
        when(notificationRepository.existsByUserAndTriggerTypeAndTriggerDate(user, NotificationTriggerType.TOPIK_EXAM, TODAY)).thenReturn(false);
        Notification saved = org.mockito.Mockito.mock(Notification.class);
        when(notificationService.create(eq(user), eq(NotificationCategory.TOPIK), any(), any(), any(), any(), any(), eq(3), eq(NotificationTriggerType.TOPIK_EXAM), eq(TODAY)))
                .thenReturn(saved);

        triggerService.processUser(user);

        verifyNoInteractions(aiService);
        verify(emailNotificationService).send(user, saved);
    }

    @Test
    void TOPIK_APPLICATION이_D_1이고_완료체크_안했으면_Notification을_생성한다() {
        User user = neutralUser(TODAY.minusYears(1), true, TODAY.plusYears(1), VisaType.OTHER, PartTimeStatus.NOT_PLANNED, AlarmSetting.ALL);
        CalendarEvent application = topikEvent(200L, EventCategory.TOPIK_APPLICATION, TODAY.minusDays(3), TODAY.plusDays(1));
        when(calendarEventRepository.findByIsGlobalTrueAndCategory(EventCategory.TOPIK_APPLICATION)).thenReturn(java.util.List.of(application));
        when(calendarEventRepository.findByIsGlobalTrueAndCategory(EventCategory.TOPIK_EXAM)).thenReturn(java.util.List.of());
        when(calendarEventService.isCompletedByUser(1L, 200L)).thenReturn(false);
        when(notificationRepository.existsByUserAndTriggerTypeAndTriggerDate(user, NotificationTriggerType.TOPIK_APPLICATION, TODAY)).thenReturn(false);

        triggerService.processUser(user);

        verify(notificationService).create(eq(user), eq(NotificationCategory.TOPIK), any(), any(), any(), any(), any(), eq(5), eq(NotificationTriggerType.TOPIK_APPLICATION), eq(TODAY));
    }

    @Test
    void TOPIK_APPLICATION이_D_1이고_이미_완료체크했으면_Notification을_생성하지_않는다() {
        User user = neutralUser(TODAY.minusYears(1), true, TODAY.plusYears(1), VisaType.OTHER, PartTimeStatus.NOT_PLANNED, AlarmSetting.ALL);
        CalendarEvent application = topikEvent(200L, EventCategory.TOPIK_APPLICATION, TODAY.minusDays(3), TODAY.plusDays(1));
        when(calendarEventRepository.findByIsGlobalTrueAndCategory(EventCategory.TOPIK_APPLICATION)).thenReturn(java.util.List.of(application));
        when(calendarEventRepository.findByIsGlobalTrueAndCategory(EventCategory.TOPIK_EXAM)).thenReturn(java.util.List.of());
        when(calendarEventService.isCompletedByUser(1L, 200L)).thenReturn(true);

        triggerService.processUser(user);

        verify(notificationService, never()).create(any(), any(), any(), any(), any(), any(), any(), any(), eq(NotificationTriggerType.TOPIK_APPLICATION), any());
    }

    @Test
    void TOPIK_이벤트의_임박_기준이_아니면_Notification을_생성하지_않는다() {
        User user = neutralUser(TODAY.minusYears(1), true, TODAY.plusYears(1), VisaType.OTHER, PartTimeStatus.NOT_PLANNED, AlarmSetting.ALL);
        CalendarEvent exam = topikEvent(100L, EventCategory.TOPIK_EXAM, TODAY.plusDays(15), null);
        when(calendarEventRepository.findByIsGlobalTrueAndCategory(EventCategory.TOPIK_APPLICATION)).thenReturn(java.util.List.of());
        when(calendarEventRepository.findByIsGlobalTrueAndCategory(EventCategory.TOPIK_EXAM)).thenReturn(java.util.List.of(exam));

        triggerService.processUser(user);

        verify(notificationService, never()).create(any(), any(), any(), any(), any(), any(), any(), any(), eq(NotificationTriggerType.TOPIK_EXAM), any());
    }

    @Test
    void ESSENTIAL_ONLY_유저는_TOPIK_D_7_임박_알림을_받지_않는다() {
        User user = neutralUser(TODAY.minusYears(1), true, TODAY.plusYears(1), VisaType.OTHER, PartTimeStatus.NOT_PLANNED, AlarmSetting.ESSENTIAL_ONLY);
        CalendarEvent exam = topikEvent(100L, EventCategory.TOPIK_EXAM, TODAY.plusDays(7), null);
        when(calendarEventRepository.findByIsGlobalTrueAndCategory(EventCategory.TOPIK_APPLICATION)).thenReturn(java.util.List.of());
        when(calendarEventRepository.findByIsGlobalTrueAndCategory(EventCategory.TOPIK_EXAM)).thenReturn(java.util.List.of(exam));

        triggerService.processUser(user);

        verify(notificationService, never()).create(any(), any(), any(), any(), any(), any(), any(), any(), eq(NotificationTriggerType.TOPIK_EXAM), any());
    }
}
