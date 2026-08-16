package com.example.kbuddy.notification.repository;

import com.example.kbuddy.notification.entity.Notification;
import com.example.kbuddy.notification.entity.NotificationTriggerType;
import com.example.kbuddy.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByUserAndTriggerTypeAndTriggerDate(User user, NotificationTriggerType triggerType, LocalDate triggerDate);

    List<Notification> findAllByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    Optional<Notification> findByIdAndUserId(Long id, Long userId);
}
