package com.example.kbuddy.calendar.repository;

import com.example.kbuddy.calendar.entity.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

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
}