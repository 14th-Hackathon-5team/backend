package com.example.kbuddy.calendar.controller;

import com.example.kbuddy.calendar.dto.CalendarEventDetailResponse;
import com.example.kbuddy.calendar.dto.CalendarEventResponse;
import com.example.kbuddy.calendar.entity.EventCategory;
import com.example.kbuddy.calendar.service.CalendarEventService;
import com.example.kbuddy.global.exception.BusinessException;
import com.example.kbuddy.global.exception.ErrorCode;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
                        EventCategory.TOPIK_APPLICATION,
                        LocalDate.of(2026, 9, 3),
                        LocalDate.of(2026, 9, 9),
                        true,
                        false
                )));

        mockMvc.perform(get("/api/calendar/events")
                        .param("year", "2026")
                        .param("month", "9")
                        .with(jwt().jwt(builder -> builder.subject("1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("CALENDAR_MONTHLY_EVENTS_FETCHED"))
                .andExpect(jsonPath("$.data[0].eventId").value(1))
                .andExpect(jsonPath("$.data[0].category").value("TOPIK_APPLICATION"));
    }

    @Test
    void 임박_일정_조회가_정상이면_200과_일정_목록을_반환한다() throws Exception {
        when(calendarEventService.getUpcomingEvents(1L))
                .thenReturn(List.of(new CalendarEventResponse(
                        2L,
                        "비자 만료 알림",
                        EventCategory.VISA,
                        LocalDate.of(2026, 9, 10),
                        null,
                        false,
                        false
                )));

        mockMvc.perform(get("/api/calendar/events/upcoming")
                        .with(jwt().jwt(builder -> builder.subject("1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("CALENDAR_UPCOMING_EVENTS_FETCHED"))
                .andExpect(jsonPath("$.data[0].eventId").value(2))
                .andExpect(jsonPath("$.data[0].isGlobal").value(false));
    }

    @Test
    void 일정_상세_조회가_정상이면_200과_상세_정보를_반환한다() throws Exception {
        when(calendarEventService.getEventDetail(1L, 10L))
                .thenReturn(new CalendarEventDetailResponse(
                        10L,
                        "전입신고 안내",
                        EventCategory.LEGAL,
                        LocalDate.of(2026, 9, 15),
                        null,
                        true,
                        "거주지 변경 시 전입신고가 필요합니다.",
                        "https://www.gov.kr"
                ));

        mockMvc.perform(get("/api/calendar/events/10")
                        .with(jwt().jwt(builder -> builder.subject("1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("CALENDAR_EVENT_DETAIL_FETCHED"))
                .andExpect(jsonPath("$.data.eventId").value(10))
                .andExpect(jsonPath("$.data.description").value("거주지 변경 시 전입신고가 필요합니다."))
                .andExpect(jsonPath("$.data.relatedLink").value("https://www.gov.kr"));
    }

    @Test
    void 일정이_없으면_404를_반환한다() throws Exception {
        when(calendarEventService.getEventDetail(1L, 999L))
                .thenThrow(new BusinessException(ErrorCode.CALENDAR_EVENT_NOT_FOUND));

        mockMvc.perform(get("/api/calendar/events/999")
                        .with(jwt().jwt(builder -> builder.subject("1"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("CALENDAR_EVENT_NOT_FOUND"));
    }

    @Test
    void 일정_완료_토글이_정상이면_200과_변경된_일정을_반환한다() throws Exception {
        when(calendarEventService.toggleCompleted(1L, 2L))
                .thenReturn(new CalendarEventResponse(
                        2L,
                        "비자 만료 알림",
                        EventCategory.VISA,
                        LocalDate.of(2026, 9, 10),
                        null,
                        false,
                        true
                ));

        mockMvc.perform(patch("/api/calendar/events/2/complete")
                        .with(jwt().jwt(builder -> builder.subject("1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("CALENDAR_EVENT_COMPLETION_TOGGLED"))
                .andExpect(jsonPath("$.data.completed").value(true));
    }

    @Test
    void 일정_숨기기가_정상이면_200을_반환한다() throws Exception {
        mockMvc.perform(delete("/api/calendar/events/2")
                        .with(jwt().jwt(builder -> builder.subject("1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("CALENDAR_EVENT_HIDDEN"));
    }

    @Test
    void 일정_복구가_정상이면_200을_반환한다() throws Exception {
        mockMvc.perform(post("/api/calendar/events/2/restore")
                        .with(jwt().jwt(builder -> builder.subject("1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("CALENDAR_EVENT_RESTORED"));
    }

    @Test
    void 숨긴_일정_목록_조회가_정상이면_200과_목록을_반환한다() throws Exception {
        when(calendarEventService.getHiddenEvents(1L))
                .thenReturn(List.of(new CalendarEventResponse(
                        3L,
                        "TOPIK 접수",
                        EventCategory.TOPIK_APPLICATION,
                        LocalDate.of(2026, 9, 3),
                        LocalDate.of(2026, 9, 9),
                        true,
                        false
                )));

        mockMvc.perform(get("/api/calendar/events/hidden")
                        .with(jwt().jwt(builder -> builder.subject("1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("CALENDAR_HIDDEN_EVENTS_FETCHED"))
                .andExpect(jsonPath("$.data[0].eventId").value(3));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        CalendarEventService calendarEventService() {
            return mock(CalendarEventService.class);
        }
    }
}