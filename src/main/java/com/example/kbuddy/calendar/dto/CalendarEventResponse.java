package com.example.kbuddy.calendar.dto;

import com.example.kbuddy.calendar.entity.CalendarEvent;
import com.example.kbuddy.calendar.entity.EventCategory;

import java.time.LocalDate;

public record CalendarEventResponse(
        Long eventId,
        String title,
        EventCategory category,
        LocalDate startDate,
        LocalDate endDate,
        Boolean isGlobal,
        Boolean completed
) {

    public static CalendarEventResponse from(CalendarEvent event, boolean completed) {
        return new CalendarEventResponse(
                event.getId(),
                event.getTitle(),
                event.getCategory(),
                event.getStartDate(),
                event.getEndDate(),
                event.getIsGlobal(),
                completed
        );
    }
}