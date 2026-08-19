package com.example.kbuddy.notification.service;

import com.example.kbuddy.notification.entity.Notification;
import com.example.kbuddy.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 생성된 Notification을 User의 가입 이메일로 발송한다.
 * 발송 실패가 Notification 생성(DB 저장)이나 다른 User 처리에 영향을 주면 안 되므로,
 * 예외를 던지지 않고 흡수한다 - 호출부({@link com.example.kbuddy.trigger.service.TriggerService})가
 * 이미 AI 호출 실패를 동일한 방식으로 흡수하고 있는 것과 같은 정책이다.
 */
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;

    public void send(User user, Notification notification) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("[kbuddy] " + notification.getTitle());
        message.setText(buildBody(user, notification));

        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.warn("Failed to send notification email. userId={}, notificationId={}, reason={}",
                    user.getId(), notification.getId(), e.getMessage());
        }
    }

    private String buildBody(User user, Notification notification) {
        return "%s님, 안녕하세요. kbuddy입니다.%n%n%s%n%n%s%n%n---%n이 메일은 kbuddy 알림 설정에 따라 자동으로 발송되었습니다."
                .formatted(user.getName(), notification.getReason(), notification.getSummary());
    }
}
