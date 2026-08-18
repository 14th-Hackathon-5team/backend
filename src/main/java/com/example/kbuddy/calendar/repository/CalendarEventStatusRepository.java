package com.example.kbuddy.calendar.repository;

import com.example.kbuddy.calendar.entity.CalendarEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CalendarEventStatusRepository extends JpaRepository<CalendarEventStatus, Long> {

    Optional<CalendarEventStatus> findByUserIdAndEventId(Long userId, Long eventId);

    List<CalendarEventStatus> findByUserIdAndEventIdIn(Long userId, List<Long> eventIds);

    List<CalendarEventStatus> findByUserIdAndHiddenAtIsNotNull(Long userId);

    @Modifying
    @Query("delete from CalendarEventStatus s where s.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
