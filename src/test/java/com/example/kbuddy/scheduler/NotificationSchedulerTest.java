package com.example.kbuddy.scheduler;

import com.example.kbuddy.auth.oauth.AuthProvider;
import com.example.kbuddy.trigger.service.TriggerService;
import com.example.kbuddy.user.entity.AccountState;
import com.example.kbuddy.user.entity.AlarmSetting;
import com.example.kbuddy.user.entity.HousingType;
import com.example.kbuddy.user.entity.Language;
import com.example.kbuddy.user.entity.PartTimeStatus;
import com.example.kbuddy.user.entity.TopikLevel;
import com.example.kbuddy.user.entity.User;
import com.example.kbuddy.user.entity.UserStatus;
import com.example.kbuddy.user.entity.VisaType;
import com.example.kbuddy.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationSchedulerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TriggerService triggerService;

    private NotificationScheduler notificationScheduler;

    @BeforeEach
    void setUp() {
        notificationScheduler = new NotificationScheduler(userRepository, triggerService);
    }

    private User user(Long id, AccountState accountState, AlarmSetting alarmSetting) {
        User user = new User(
                AuthProvider.GOOGLE,
                "google-" + id,
                "student" + id + "@example.com",
                "사용자" + id,
                "VN",
                2000,
                UserStatus.UNDERGRADUATE,
                "경북대학교",
                LocalDate.of(2020, 1, 1),
                VisaType.D2,
                true,
                LocalDate.of(2030, 1, 1),
                HousingType.DORMITORY,
                false,
                PartTimeStatus.NOT_PLANNED,
                TopikLevel.LEVEL_3,
                TopikLevel.LEVEL_5,
                Language.KOREAN
        );
        user.updateAlarmSetting(alarmSetting);
        setAccountState(user, accountState);
        setId(user, id);
        return user;
    }

    private void setAccountState(User user, AccountState accountState) {
        try {
            var field = User.class.getDeclaredField("accountState");
            field.setAccessible(true);
            field.set(user, accountState);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private void setId(User user, Long id) {
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    // ---------- 24: Scheduler가 TriggerService를 호출하는지 ----------

    @Test
    void run을_호출하면_활성_사용자에_대해_TriggerService_processUser를_호출한다() {
        User active = user(1L, AccountState.ACTIVE, AlarmSetting.ALL);
        when(userRepository.findAll()).thenReturn(List.of(active));

        notificationScheduler.run();

        verify(triggerService, times(1)).processUser(active);
    }

    // ---------- 25: 비활성(DELETED) User 제외 ----------

    @Test
    void accountState가_DELETED인_User는_TriggerService_대상에서_제외된다() {
        User active = user(1L, AccountState.ACTIVE, AlarmSetting.ALL);
        User deleted = user(2L, AccountState.DELETED, AlarmSetting.ALL);
        when(userRepository.findAll()).thenReturn(List.of(active, deleted));

        notificationScheduler.run();

        verify(triggerService, times(1)).processUser(active);
        verify(triggerService, never()).processUser(deleted);
    }

    // ---------- 26: alarmSetting NONE User 제외 ----------

    @Test
    void alarmSetting이_NONE인_User는_TriggerService_대상에서_제외된다() {
        User active = user(1L, AccountState.ACTIVE, AlarmSetting.ALL);
        User noneAlarm = user(2L, AccountState.ACTIVE, AlarmSetting.NONE);
        when(userRepository.findAll()).thenReturn(List.of(active, noneAlarm));

        notificationScheduler.run();

        verify(triggerService, times(1)).processUser(active);
        verify(triggerService, never()).processUser(noneAlarm);
    }

    // ---------- 한 User의 예외가 전체 Scheduler를 중단시키지 않는지 ----------

    @Test
    void 한_User_처리_중_예외가_발생해도_나머지_User_처리는_계속된다() {
        User first = user(1L, AccountState.ACTIVE, AlarmSetting.ALL);
        User second = user(2L, AccountState.ACTIVE, AlarmSetting.ALL);
        User third = user(3L, AccountState.ACTIVE, AlarmSetting.ALL);
        when(userRepository.findAll()).thenReturn(List.of(first, second, third));
        doThrow(new RuntimeException("boom")).when(triggerService).processUser(second);

        assertThatCode(() -> notificationScheduler.run()).doesNotThrowAnyException();

        verify(triggerService).processUser(first);
        verify(triggerService).processUser(second);
        verify(triggerService).processUser(third);
    }
}
