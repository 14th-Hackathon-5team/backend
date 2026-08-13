package com.example.kbuddy.calendar.dto;

import com.example.kbuddy.calendar.entity.CalendarEvent;
import com.example.kbuddy.calendar.entity.EventCategory;

import java.time.LocalDate;

public record CalendarEventDetailResponse(
        Long eventId,
        String title,
        EventCategory category,
        LocalDate startDate,
        LocalDate endDate,
        Boolean isGlobal,
        String description,
        String relatedLink
) {

    public static CalendarEventDetailResponse from(CalendarEvent event) {
        return new CalendarEventDetailResponse(
                event.getId(),
                event.getTitle(),
                event.getCategory(),
                event.getStartDate(),
                event.getEndDate(),
                event.getIsGlobal(),
                event.getDescription(),
                event.getRelatedLink()
        );
    }
}