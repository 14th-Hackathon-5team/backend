package com.example.kbuddy.calendar.service;

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
                EventCategory.TOPIK,
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
        assertThat(responses.get(0).category()).isEqualTo(EventCategory.TOPIK);
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