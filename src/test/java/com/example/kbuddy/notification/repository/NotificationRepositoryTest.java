package com.example.kbuddy.notification.repository;

import com.example.kbuddy.notification.entity.NotificationTriggerType;
import com.example.kbuddy.user.entity.User;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class NotificationRepositoryTest {

    @Test
    void existsByUserAndTriggerTypeAndTriggerDate_메서드가_User_TriggerType_LocalDate_파라미터로_존재한다() {
        assertThatCode(() ->
                NotificationRepository.class.getMethod(
                        "existsByUserAndTriggerTypeAndTriggerDate",
                        User.class,
                        NotificationTriggerType.class,
                        LocalDate.class
                )
        ).doesNotThrowAnyException();
    }

    @Test
    void existsByUserAndTriggerTypeAndTriggerDate_메서드는_boolean을_반환한다() throws NoSuchMethodException {
        Method method = NotificationRepository.class.getMethod(
                "existsByUserAndTriggerTypeAndTriggerDate",
                User.class,
                NotificationTriggerType.class,
                LocalDate.class
        );

        assertThat(method.getReturnType()).isEqualTo(boolean.class);
    }

    @Test
    void findAllByUserIdOrderByCreatedAtDescIdDesc_메서드가_Long_파라미터로_존재하고_List를_반환한다() throws NoSuchMethodException {
        Method method = NotificationRepository.class.getMethod(
                "findAllByUserIdOrderByCreatedAtDescIdDesc", Long.class);

        assertThat(method.getReturnType()).isEqualTo(List.class);
    }

    @Test
    void findByIdAndUserId_메서드가_Long_Long_파라미터로_존재하고_Optional을_반환한다() throws NoSuchMethodException {
        Method method = NotificationRepository.class.getMethod(
                "findByIdAndUserId", Long.class, Long.class);

        assertThat(method.getReturnType()).isEqualTo(Optional.class);
    }
}
