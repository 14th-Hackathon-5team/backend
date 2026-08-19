package com.example.kbuddy.calendar.repository;

import com.example.kbuddy.calendar.entity.CalendarEvent;
import com.example.kbuddy.calendar.entity.EventCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    @Query("""
            select e
            from CalendarEvent e
            where (e.isGlobal = true or e.user.id = :userId)
              and e.startDate <= :endDate
              and coalesce(e.endDate, e.startDate) >= :startDate
            order by e.startDate asc, e.id asc
            """)
    List<CalendarEvent> findVisibleEventsBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            select e
            from CalendarEvent e
            where (e.isGlobal = true or e.user.id = :userId)
              and e.id = :eventId
            """)
    Optional<CalendarEvent> findVisibleEventById(
            @Param("userId") Long userId,
            @Param("eventId") Long eventId
    );

    List<CalendarEvent> findByIsGlobalTrueAndCategory(EventCategory category);

    @Modifying
    @Query("""
            delete from CalendarEvent e
            where e.user.id = :userId
            """)
    void deleteByUserId(@Param("userId") Long userId);
}