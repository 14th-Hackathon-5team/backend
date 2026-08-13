package com.example.kbuddy.calendar.service;

import com.example.kbuddy.calendar.dto.CalendarEventDetailResponse;
import com.example.kbuddy.calendar.dto.CalendarEventResponse;
import com.example.kbuddy.calendar.entity.CalendarEvent;
import com.example.kbuddy.calendar.entity.EventCategory;
import com.example.kbuddy.calendar.repository.CalendarEventRepository;
import com.example.kbuddy.global.exception.BusinessException;
import com.example.kbuddy.global.exception.ErrorCode;
import com.example.kbuddy.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarEventServiceTest {

    @Mock
    private CalendarEventRepository calendarEventRepository;

    @Mock
    private UserRepository userRepository;

    private CalendarEventService calendarEventService;

    @BeforeEach
    void setUp() {
        calendarEventService = new CalendarEventService(calendarEventRepository, userRepository);
    }

    @Test
    void 월별_일정은_선택한_월의_시작일과_종료일로_조회한다() {
        CalendarEvent event = event(
                1L,
                "TOPIK 접수",
                EventCategory.TOPIK_APPLICATION,
                LocalDate.of(2026, 9, 3),
                LocalDate.of(2026, 9, 9),
                true
        );
        when(userRepository.existsById(1L)).thenReturn(true);
        when(calendarEventRepository.findVisibleEventsBetween(
                1L,
                YearMonth.of(2026, 9).atDay(1),
                YearMonth.of(2026, 9).atEndOfMonth()
        )).thenReturn(List.of(event));

        List<CalendarEventResponse> responses = calendarEventService.getMonthlyEvents(1L, 2026, 9);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).eventId()).isEqualTo(1L);
        assertThat(responses.get(0).title()).isEqualTo("TOPIK 접수");
        assertThat(responses.get(0).category()).isEqualTo(EventCategory.TOPIK_APPLICATION);
    }

    @Test
    void 존재하지_않는_사용자의_월별_일정_조회는_USER_NOT_FOUND_예외가_발생한다() {
        when(userRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> calendarEventService.getMonthlyEvents(999L, 2026, 9))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void 일정_상세를_조회할_수_있다() {
        CalendarEvent event = event(
                10L,
                "비자 만료 알림",
                EventCategory.VISA,
                LocalDate.of(2026, 10, 1),
                null,
                false
        );
        when(event.getDescription()).thenReturn("체류 기간 만료 전에 연장 신청이 필요합니다.");
        when(event.getRelatedLink()).thenReturn("https://www.hikorea.go.kr");
        when(userRepository.existsById(1L)).thenReturn(true);
        when(calendarEventRepository.findVisibleEventById(1L, 10L)).thenReturn(Optional.of(event));

        CalendarEventDetailResponse response = calendarEventService.getEventDetail(1L, 10L);

        assertThat(response.eventId()).isEqualTo(10L);
        assertThat(response.category()).isEqualTo(EventCategory.VISA);
        assertThat(response.description()).isEqualTo("체류 기간 만료 전에 연장 신청이 필요합니다.");
        assertThat(response.relatedLink()).isEqualTo("https://www.hikorea.go.kr");
    }

    @Test
    void 조회할_수_없는_일정이면_CALENDAR_EVENT_NOT_FOUND_예외가_발생한다() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(calendarEventRepository.findVisibleEventById(1L, 999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> calendarEventService.getEventDetail(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CALENDAR_EVENT_NOT_FOUND);
    }

    @Test
    void 임박_일정은_오늘부터_7일_뒤까지_조회한다() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(calendarEventRepository.findVisibleEventsBetween(1L, LocalDate.now(), LocalDate.now().plusDays(7)))
                .thenReturn(List.of());

        List<CalendarEventResponse> responses = calendarEventService.getUpcomingEvents(1L);

        assertThat(responses).isEmpty();
        verify(calendarEventRepository).findVisibleEventsBetween(1L, LocalDate.now(), LocalDate.now().plusDays(7));
    }

    private CalendarEvent event(
            Long id,
            String title,
            EventCategory category,
            LocalDate startDate,
            LocalDate endDate,
            Boolean isGlobal
    ) {
        CalendarEvent event = mock(CalendarEvent.class);
        when(event.getId()).thenReturn(id);
        when(event.getTitle()).thenReturn(title);
        when(event.getCategory()).thenReturn(category);
        when(event.getStartDate()).thenReturn(startDate);
        when(event.getEndDate()).thenReturn(endDate);
        when(event.getIsGlobal()).thenReturn(isGlobal);
        return event;
    }
}