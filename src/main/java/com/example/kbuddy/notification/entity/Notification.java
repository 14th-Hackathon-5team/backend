package com.example.kbuddy.notification.entity;

import com.example.kbuddy.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "notifications")
public class Notification {

    private static final int MIN_PRIORITY = 1;
    private static final int MAX_PRIORITY = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 20, nullable = false)
    private NotificationCategory category;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "reason", columnDefinition = "TEXT", nullable = false)
    private String reason;

    @Column(name = "summary", columnDefinition = "TEXT", nullable = false)
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "json", nullable = false)
    private Map<String, Object> details;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source", columnDefinition = "json")
    private Map<String, Object> source;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", length = 30, nullable = false)
    private NotificationTriggerType triggerType;

    @Column(name = "trigger_date", nullable = false)
    private LocalDate triggerDate;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Notification(
            User user,
            NotificationCategory category,
            String title,
            String reason,
            String summary,
            Map<String, Object> details,
            Map<String, Object> source,
            Integer priority,
            NotificationTriggerType triggerType,
            LocalDate triggerDate
    ) {
        validatePriority(priority);
        this.user = user;
        this.category = category;
        this.title = title;
        this.reason = reason;
        this.summary = summary;
        this.details = details;
        this.source = source;
        this.priority = priority;
        this.triggerType = triggerType;
        this.triggerDate = triggerDate;
        this.isRead = false;
    }

    private static void validatePriority(Integer priority) {
        if (priority == null || priority < MIN_PRIORITY || priority > MAX_PRIORITY) {
            throw new IllegalArgumentException("priority는 1~5 사이여야 합니다: " + priority);
        }
    }

    public void markAsRead() {
        if (this.isRead) {
            return;
        }
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.isRead == null) {
            this.isRead = false;
        }
    }
}
