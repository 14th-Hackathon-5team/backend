package com.example.kbuddy.user.service;

import com.example.kbuddy.auth.jwt.JwtProvider;
import com.example.kbuddy.auth.oauth.AuthProvider;
import com.example.kbuddy.calendar.repository.CalendarEventRepository;
import com.example.kbuddy.global.exception.BusinessException;
import com.example.kbuddy.global.exception.ErrorCode;
import com.example.kbuddy.user.dto.UserSettingsResponse;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSettingsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private CalendarEventRepository calendarEventRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, jwtProvider, calendarEventRepository);
    }

    @Test
    void 설정_정보를_조회할_수_있다() {
        User user = baselineUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserSettingsResponse response = userService.getSettings(1L);

        assertThat(response.language()).isEqualTo(Language.KOREAN);
        assertThat(response.alarmSetting()).isEqualTo(AlarmSetting.ALL);
        assertThat(response.accountState()).isEqualTo(AccountState.ACTIVE);
    }

    @Test
    void 언어_설정을_변경하면_User_language가_변경된다() {
        User user = baselineUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserSettingsResponse response = userService.updateLanguage(1L, Language.ENGLISH);

        assertThat(response.language()).isEqualTo(Language.ENGLISH);
        assertThat(user.getLanguage()).isEqualTo(Language.ENGLISH);
    }

    @Test
    void 알림_설정을_변경할_수_있다() {
        User user = baselineUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserSettingsResponse response = userService.updateAlarmSetting(1L, AlarmSetting.ESSENTAL_ONLY);

        assertThat(response.alarmSetting()).isEqualTo(AlarmSetting.ESSENTAL_ONLY);
        assertThat(user.getAlarmSetting()).isEqualTo(AlarmSetting.ESSENTAL_ONLY);
    }

    @Test
    void 존재하지_않는_사용자의_설정_조회는_USER_NOT_FOUND_예외가_발생한다() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getSettings(999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    private User baselineUser() {
        return new User(
                AuthProvider.GOOGLE,
                "109876543210987654321",
                "test@gmail.com",
                "김철수",
                "중국",
                2000,
                UserStatus.UNDERGRADUATE,
                "서울대학교",
                LocalDate.of(2022, 3, 1),
                VisaType.D2,
                true,
                LocalDate.of(2027, 3, 1),
                HousingType.DORMITORY,
                false,
                PartTimeStatus.SEARCHING,
                TopikLevel.LEVEL_3,
                TopikLevel.LEVEL_5,
                Language.KOREAN
        );
    }
}
