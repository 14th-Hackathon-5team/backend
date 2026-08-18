package com.example.kbuddy.calendar.entity;

import com.example.kbuddy.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "calendar_event_statuses",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_calendar_event_statuses_user_event", columnNames = {"user_id", "event_id"})
        }
)
public class CalendarEventStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "status_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "completed", nullable = false)
    private Boolean completed;

    @Column(name = "hidden_at")
    private LocalDateTime hiddenAt;

    public CalendarEventStatus(User user, Long eventId) {
        this.user = user;
        this.eventId = eventId;
        this.completed = false;
        this.hiddenAt = null;
    }

    public void toggleCompleted() {
        this.completed = !this.completed;
    }

    public void hide() {
        this.hiddenAt = LocalDateTime.now();
    }

    public void restore() {
        this.hiddenAt = null;
    }

    public boolean isHidden() {
        return this.hiddenAt != null;
    }
}
