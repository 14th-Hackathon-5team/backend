package com.example.kbuddy.calendar.controller;

import com.example.kbuddy.calendar.dto.CalendarEventResponse;
import com.example.kbuddy.calendar.entity.EventCategory;
import com.example.kbuddy.calendar.service.CalendarEventService;
import com.example.kbuddy.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CalendarEventController.class)
@Import({GlobalExceptionHandler.class, CalendarEventControllerTest.TestConfig.class})
class CalendarEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CalendarEventService calendarEventService;

    @BeforeEach
    void resetCalendarEventServiceMock() {
        Mockito.reset(calendarEventService);
    }

    @Test
    void 월별_일정_조회가_정상이면_200과_일정_목록을_반환한다() throws Exception {
        when(calendarEventService.getMonthlyEvents(1L, 2026, 9))
                .thenReturn(List.of(new CalendarEventResponse(
                        1L,
                        "TOPIK 접수",
                        EventCategory.TOPIK,
                        LocalDate.of(2026, 9, 3),
                        LocalDate.of(2026, 9, 9),
                        true
                )));

        mockMvc.perform(get("/api/calendar/events")
                        .param("year", "2026")
                        .param("month", "9")
                        .with(jwt().jwt(builder -> builder.subject("1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("CALENDAR_MONTHLY_EVENTS_FETCHED"))
                .andExpect(jsonPath("$.data[0].eventId").value(1))
                .andExpect(jsonPath("$.data[0].category").value("TOPIK"));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        CalendarEventService calendarEventService() {
            return mock(CalendarEventService.class);
        }
    }
}