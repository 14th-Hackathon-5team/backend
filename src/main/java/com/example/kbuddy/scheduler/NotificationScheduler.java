package com.example.kbuddy.scheduler;

import com.example.kbuddy.trigger.service.TriggerService;
import com.example.kbuddy.user.entity.AccountState;
import com.example.kbuddy.user.entity.AlarmSetting;
import com.example.kbuddy.user.entity.User;
import com.example.kbuddy.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 매일 한 번 활성 사용자를 대상으로 {@link TriggerService}를 실행해
 * AI 맞춤 알림(Notification) 생성 여부를 판단한다.
 * Scheduler 자체는 비즈니스 로직을 갖지 않고 대상 User를 추려 TriggerService에 위임만 한다.
 */
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);

    private final UserRepository userRepository;
    private final TriggerService triggerService;

    @Scheduled(cron = "${scheduler.notification.cron:0 0 9 * * *}", zone = "Asia/Seoul")
    public void run() {
        List<User> targets = userRepository.findAll().stream()
                .filter(user -> user.getAccountState() == AccountState.ACTIVE)
                .filter(user -> user.getAlarmSetting() != AlarmSetting.NONE)
                .toList();

        for (User user : targets) {
            try {
                triggerService.processUser(user);
            } catch (Exception e) {
                // 한 User의 실패가 나머지 User 처리를 막지 않도록 여기서 흡수한다.
                log.warn("Trigger processing failed for user. userId={}, exceptionType={}",
                        user.getId(), e.getClass().getSimpleName());
            }
        }
    }
}
