package com.example.kbuddy.trigger.service;

import com.example.kbuddy.ai.client.AiClientException;
import com.example.kbuddy.ai.dto.AiRecommendation;
import com.example.kbuddy.ai.dto.AiRecommendationRequest;
import com.example.kbuddy.ai.dto.AiRecommendationResponse;
import com.example.kbuddy.ai.dto.AiTrigger;
import com.example.kbuddy.ai.dto.AiUser;
import com.example.kbuddy.ai.service.AiService;
import com.example.kbuddy.notification.entity.NotificationTriggerType;
import com.example.kbuddy.notification.repository.NotificationRepository;
import com.example.kbuddy.notification.service.NotificationService;
import com.example.kbuddy.user.entity.AlarmSetting;
import com.example.kbuddy.user.entity.PartTimeStatus;
import com.example.kbuddy.user.entity.User;
import com.example.kbuddy.user.entity.VisaType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 한 User에 대해 어떤 Trigger가 발생했는지 판단하고, FastAPI Recommendation을 호출해
 * Notification 생성까지 이어주는 orchestration 서비스.
 *
 * 현재 실제로 AI 호출 + Notification 저장까지 연결되는 Trigger는
 * {@link NotificationTriggerType#VISA_EXPIRATION}과 {@link NotificationTriggerType#ALIEN_REGISTRATION}
 * 두 가지뿐이다(STEP A에서 정의된 값만 존재하므로).
 *
 * BEFORE_ENTRY / VISA(D2,D4) / PART_TIME 등 나머지 조건은 아직 NotificationTriggerType이
 * 없어 판단 로직만 준비해두고(추후 확장 대비), 실제 AI 호출/Notification 생성으로는
 * 연결하지 않는다. 이 조건에 해당하는 사용자 프로필 정보는 {@link #toAiUser(User)}를 통해
 * 어떤 AI 호출에서든 컨텍스트로 항상 함께 전달된다.
 */
@Service
@RequiredArgsConstructor
public class TriggerService {

    private static final Logger log = LoggerFactory.getLogger(TriggerService.class);

    /**
     * 체류기간 만료 "임박" 기준(일). 정확한 정책이 확정되지 않아, Calendar 도메인의
     * 기존 D-30 체류만료 알림(CalendarEventService.VISA_EXPIRATION_REMINDER_DAYS_BEFORE)과
     * 동일한 값을 재사용해 가장 보수적이고 일관된 기준을 적용한다.
     */
    private static final int VISA_EXPIRATION_THRESHOLD_DAYS = 30;

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final AiService aiService;
    private final Clock clock;

    @Transactional
    public void processUser(User user) {
        if (user.getAlarmSetting() == AlarmSetting.NONE) {
            return;
        }

        LocalDate today = LocalDate.now(clock);

        if (isAlienRegistrationCandidate(user, today)) {
            evaluateTrigger(user, NotificationTriggerType.ALIEN_REGISTRATION, null, today);
        }
        if (isVisaExpirationCandidate(user, today)) {
            int daysRemaining = (int) ChronoUnit.DAYS.between(today, user.getStayExpirationDate());
            evaluateTrigger(user, NotificationTriggerType.VISA_EXPIRATION, daysRemaining, today);
        }
    }

    private void evaluateTrigger(User user, NotificationTriggerType triggerType, Integer daysRemaining, LocalDate triggerDate) {
        if (notificationRepository.existsByUserAndTriggerTypeAndTriggerDate(user, triggerType, triggerDate)) {
            return;
        }

        AiRecommendationRequest request = new AiRecommendationRequest(
                toAiUser(user), new AiTrigger(triggerType.name(), daysRemaining));

        AiRecommendationResponse response;
        try {
            response = aiService.recommendations(request);
        } catch (AiClientException e) {
            log.warn("AI recommendation call failed. userId={}, triggerType={}, reason={}",
                    user.getId(), triggerType, e.getReason());
            return;
        }

        AiRecommendation selected = selectRecommendation(response);
        if (selected == null) {
            return;
        }
        if (!isAllowedByAlarmSetting(user.getAlarmSetting(), selected.priority())) {
            return;
        }

        notificationService.create(
                user,
                selected.category(),
                selected.title(),
                selected.reason(),
                selected.summary(),
                selected.details(),
                selected.source(),
                selected.priority(),
                triggerType,
                triggerDate
        );
    }

    /**
     * priority가 가장 높은 Recommendation 하나만 선택한다(MVP 정책).
     * priority가 없거나 1~5 범위를 벗어난 Recommendation은 후보에서 제외한다.
     * 동일 priority면 응답 배열에서 먼저 나온 항목을 우선한다(결정론적 선택).
     */
    private AiRecommendation selectRecommendation(AiRecommendationResponse response) {
        if (response == null || response.recommendations() == null) {
            return null;
        }

        AiRecommendation best = null;
        for (AiRecommendation candidate : response.recommendations()) {
            if (!isValidPriority(candidate)) {
                continue;
            }
            if (best == null || candidate.priority() > best.priority()) {
                best = candidate;
            }
        }
        return best;
    }

    private boolean isValidPriority(AiRecommendation candidate) {
        return candidate != null
                && candidate.priority() != null
                && candidate.priority() >= 1
                && candidate.priority() <= 5;
    }

    private boolean isAllowedByAlarmSetting(AlarmSetting alarmSetting, int priority) {
        return switch (alarmSetting) {
            case ALL -> true;
            case ESSENTIAL_ONLY -> priority == 5;
            case NONE -> false;
        };
    }

    boolean isAlienRegistrationCandidate(User user, LocalDate today) {
        if (Boolean.TRUE.equals(user.getHasAlienRegistration())) {
            return false;
        }
        // 입국 전 사용자에게 외국인등록 안내는 너무 이른 알림이므로 제외한다.
        return user.getEntryDate() != null && !user.getEntryDate().isAfter(today);
    }

    boolean isVisaExpirationCandidate(User user, LocalDate today) {
        LocalDate expiration = user.getStayExpirationDate();
        if (expiration == null || expiration.isBefore(today)) {
            return false;
        }
        long daysRemaining = ChronoUnit.DAYS.between(today, expiration);
        return daysRemaining <= VISA_EXPIRATION_THRESHOLD_DAYS;
    }

    boolean isBeforeEntry(User user, LocalDate today) {
        return user.getEntryDate() != null && user.getEntryDate().isAfter(today);
    }

    boolean isVisaCandidate(User user) {
        return user.getVisaType() == VisaType.D2 || user.getVisaType() == VisaType.D4;
    }

    boolean isPartTimeCandidate(User user) {
        return user.getPartTimeStatus() == PartTimeStatus.SEARCHING
                || user.getPartTimeStatus() == PartTimeStatus.WORKING;
    }

    AiUser toAiUser(User user) {
        return new AiUser(
                user.getId(),
                user.getNationality(),
                user.getBirthYear(),
                user.getUserStatus(),
                user.getSchoolName(),
                user.getEntryDate(),
                user.getVisaType(),
                user.getHasAlienRegistration(),
                user.getStayExpirationDate(),
                user.getHousingType(),
                user.getIsParentSupported(),
                user.getPartTimeStatus(),
                user.getCurrentTopikLevel(),
                user.getTargetTopikLevel(),
                user.getLanguage()
        );
    }
}
