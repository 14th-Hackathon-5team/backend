package com.example.kbuddy.notification.service;

import com.example.kbuddy.auth.oauth.AuthProvider;
import com.example.kbuddy.notification.entity.Notification;
import com.example.kbuddy.notification.entity.NotificationCategory;
import com.example.kbuddy.notification.entity.NotificationTriggerType;
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
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailNotificationService emailNotificationService;

    @BeforeEach
    void setUp() {
        emailNotificationService = new EmailNotificationService(mailSender);
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

    private Notification createNotification(User user) {
        return new Notification(
                user, NotificationCategory.VISA, "체류기간 만료 30일 전입니다", "현재 비자가 D-2입니다.",
                "체류기간 만료 전에 연장 절차를 준비하세요.", Map.of("visaType", "D2"), null, 5,
                NotificationTriggerType.VISA_EXPIRATION, LocalDate.of(2026, 8, 31));
    }

    @Test
    void send를_호출하면_User의_이메일로_제목과_내용을_담아_발송한다() {
        User user = createUser();
        Notification notification = createNotification(user);

        emailNotificationService.send(user, notification);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getTo()).containsExactly("student@example.com");
        assertThat(message.getSubject()).isEqualTo("[kbuddy] 체류기간 만료 30일 전입니다");
        assertThat(message.getText())
                .contains("김철수")
                .contains("현재 비자가 D-2입니다.")
                .contains("체류기간 만료 전에 연장 절차를 준비하세요.");
    }

    @Test
    void 발송이_실패해도_예외를_던지지_않는다() {
        User user = createUser();
        Notification notification = createNotification(user);
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> emailNotificationService.send(user, notification)).doesNotThrowAnyException();
    }
}
