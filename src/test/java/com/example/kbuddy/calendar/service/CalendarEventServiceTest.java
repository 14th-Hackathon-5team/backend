package com.example.kbuddy.calendar.service;

import com.example.kbuddy.calendar.dto.CalendarEventDetailResponse;
import com.example.kbuddy.calendar.dto.CalendarEventResponse;
import com.example.kbuddy.calendar.entity.CalendarEvent;
import com.example.kbuddy.calendar.entity.CalendarEventStatus;
import com.example.kbuddy.calendar.entity.EventCategory;
import com.example.kbuddy.calendar.repository.CalendarEventRepository;
import com.example.kbuddy.calendar.repository.CalendarEventStatusRepository;
import com.example.kbuddy.global.exception.BusinessException;
import com.example.kbuddy.global.exception.ErrorCode;
import com.example.kbuddy.user.entity.User;
import com.example.kbuddy.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarEventServiceTest {

    @Mock
    private CalendarEventRepository calendarEventRepository;

    @Mock
    private CalendarEventStatusRepository calendarEventStatusRepository;

    @Mock
    private UserRepository userRepository;

    private CalendarEventService calendarEventService;

    @BeforeEach
    void setUp() {
        calendarEventService = new CalendarEventService(
                calendarEventRepository, calendarEventStatusRepository, userRepository);
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
        User user = user(1L, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
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
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> calendarEventService.getMonthlyEvents(999L, 2026, 9))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void 체류기간_만료_30일_전이면_비자_만료_알림_일정이_포함된다() {
        User user = user(1L, LocalDate.of(2026, 10, 1));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(calendarEventRepository.findVisibleEventsBetween(
                1L,
                YearMonth.of(2026, 9).atDay(1),
                YearMonth.of(2026, 9).atEndOfMonth()
        )).thenReturn(List.of());

        List<CalendarEventResponse> responses = calendarEventService.getMonthlyEvents(1L, 2026, 9);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).title()).isEqualTo("체류기간 만료 30일 전 안내");
        assertThat(responses.get(0).category()).isEqualTo(EventCategory.VISA);
        assertThat(responses.get(0).startDate()).isEqualTo(LocalDate.of(2026, 9, 1));
    }

    @Test
    void 체류기간_만료일이_조회_범위_밖이면_비자_만료_알림_일정이_포함되지_않는다() {
        User user = user(1L, LocalDate.of(2027, 1, 1));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(calendarEventRepository.findVisibleEventsBetween(
                1L,
                YearMonth.of(2026, 9).atDay(1),
                YearMonth.of(2026, 9).atEndOfMonth()
        )).thenReturn(List.of());

        List<CalendarEventResponse> responses = calendarEventService.getMonthlyEvents(1L, 2026, 9);

        assertThat(responses).isEmpty();
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
    void D_30_가상_일정_상세를_조회할_수_있다() {
        User user = mock(User.class);
        when(user.getStayExpirationDate()).thenReturn(LocalDate.of(2026, 10, 1));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        CalendarEventDetailResponse response = calendarEventService.getEventDetail(1L, -1L);

        assertThat(response.eventId()).isEqualTo(-1L);
        assertThat(response.title()).isEqualTo("체류기간 만료 30일 전 안내");
        assertThat(response.category()).isEqualTo(EventCategory.VISA);
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(response.endDate()).isNull();
        assertThat(response.isGlobal()).isFalse();
        assertThat(response.description()).isNotBlank();
        assertThat(response.relatedLink()).isNotBlank();
    }

    @Test
    void 체류기간_만료일이_없으면_D_30_가상_일정_상세_조회는_CALENDAR_EVENT_NOT_FOUND_예외가_발생한다() {
        User user = mock(User.class);
        when(user.getStayExpirationDate()).thenReturn(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> calendarEventService.getEventDetail(1L, -1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CALENDAR_EVENT_NOT_FOUND);
    }

    @Test
    void 임박_일정은_오늘부터_7일_뒤까지_조회한다() {
        User user = user(1L, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(calendarEventRepository.findVisibleEventsBetween(1L, LocalDate.now(), LocalDate.now().plusDays(7)))
                .thenReturn(List.of());

        List<CalendarEventResponse> responses = calendarEventService.getUpcomingEvents(1L);

        assertThat(responses).isEmpty();
        verify(calendarEventRepository).findVisibleEventsBetween(1L, LocalDate.now(), LocalDate.now().plusDays(7));
    }

    @Test
    void toggleCompleted는_기존_상태가_없으면_새로_만들어_완료로_전환한다() {
        User user = user(1L, null);
        CalendarEvent event = event(10L, "TOPIK 접수", EventCategory.TOPIK_APPLICATION,
                LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 9), true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(calendarEventRepository.findVisibleEventById(1L, 10L)).thenReturn(Optional.of(event));
        when(calendarEventStatusRepository.findByUserIdAndEventId(1L, 10L)).thenReturn(Optional.empty());
        when(calendarEventStatusRepository.save(any(CalendarEventStatus.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CalendarEventResponse response = calendarEventService.toggleCompleted(1L, 10L);

        assertThat(response.eventId()).isEqualTo(10L);
        assertThat(response.completed()).isTrue();
    }

    @Test
    void toggleCompleted는_이미_완료된_상태면_다시_미완료로_전환한다() {
        User user = user(1L, null);
        CalendarEvent event = event(10L, "TOPIK 접수", EventCategory.TOPIK_APPLICATION,
                LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 9), true);
        CalendarEventStatus status = new CalendarEventStatus(user, 10L);
        status.toggleCompleted();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(calendarEventRepository.findVisibleEventById(1L, 10L)).thenReturn(Optional.of(event));
        when(calendarEventStatusRepository.findByUserIdAndEventId(1L, 10L)).thenReturn(Optional.of(status));

        CalendarEventResponse response = calendarEventService.toggleCompleted(1L, 10L);

        assertThat(response.completed()).isFalse();
    }

    @Test
    void 존재하지_않는_일정을_완료_토글하면_CALENDAR_EVENT_NOT_FOUND_예외가_발생한다() {
        User user = user(1L, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(calendarEventRepository.findVisibleEventById(1L, 999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> calendarEventService.toggleCompleted(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CALENDAR_EVENT_NOT_FOUND);
    }

    @Test
    void hide는_기존_상태가_없으면_새로_만들어_숨김_처리한다() {
        User user = user(1L, null);
        CalendarEvent event = event(10L, "TOPIK 접수", EventCategory.TOPIK_APPLICATION,
                LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 9), true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(calendarEventRepository.findVisibleEventById(1L, 10L)).thenReturn(Optional.of(event));
        when(calendarEventStatusRepository.findByUserIdAndEventId(1L, 10L)).thenReturn(Optional.empty());
        when(calendarEventStatusRepository.save(any(CalendarEventStatus.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        calendarEventService.hide(1L, 10L);

        ArgumentCaptor<CalendarEventStatus> captor = ArgumentCaptor.forClass(CalendarEventStatus.class);
        verify(calendarEventStatusRepository).save(captor.capture());
        assertThat(captor.getValue().isHidden()).isTrue();
    }

    @Test
    void getMonthlyEvents는_숨겨진_일정을_제외한다() {
        User user = user(1L, null);
        CalendarEvent event = event(10L, "TOPIK 접수", EventCategory.TOPIK_APPLICATION,
                LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 9), true);
        CalendarEventStatus hiddenStatus = new CalendarEventStatus(user, 10L);
        hiddenStatus.hide();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(calendarEventRepository.findVisibleEventsBetween(
                1L, YearMonth.of(2026, 9).atDay(1), YearMonth.of(2026, 9).atEndOfMonth()
        )).thenReturn(List.of(event));
        when(calendarEventStatusRepository.findByUserIdAndEventIdIn(1L, List.of(10L)))
                .thenReturn(List.of(hiddenStatus));

        List<CalendarEventResponse> responses = calendarEventService.getMonthlyEvents(1L, 2026, 9);

        assertThat(responses).isEmpty();
    }

    @Test
    void restore는_숨김_상태가_아니면_CALENDAR_EVENT_NOT_FOUND_예외가_발생한다() {
        User user = user(1L, null);
        CalendarEventStatus status = new CalendarEventStatus(user, 10L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(calendarEventStatusRepository.findByUserIdAndEventId(1L, 10L)).thenReturn(Optional.of(status));

        assertThatThrownBy(() -> calendarEventService.restore(1L, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CALENDAR_EVENT_NOT_FOUND);
    }

    @Test
    void restore는_숨긴_일정을_다시_보이게_한다() {
        User user = user(1L, null);
        CalendarEventStatus status = new CalendarEventStatus(user, 10L);
        status.hide();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(calendarEventStatusRepository.findByUserIdAndEventId(1L, 10L)).thenReturn(Optional.of(status));

        calendarEventService.restore(1L, 10L);

        assertThat(status.isHidden()).isFalse();
    }

    @Test
    void getHiddenEvents는_숨긴_일정_목록을_조회한다() {
        User user = user(1L, null);
        CalendarEvent event = event(10L, "TOPIK 접수", EventCategory.TOPIK_APPLICATION,
                LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 9), true);
        CalendarEventStatus hiddenStatus = new CalendarEventStatus(user, 10L);
        hiddenStatus.hide();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(calendarEventStatusRepository.findByUserIdAndHiddenAtIsNotNull(1L)).thenReturn(List.of(hiddenStatus));
        when(calendarEventRepository.findVisibleEventById(1L, 10L)).thenReturn(Optional.of(event));

        List<CalendarEventResponse> responses = calendarEventService.getHiddenEvents(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).eventId()).isEqualTo(10L);
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
        lenient().when(event.getId()).thenReturn(id);
        lenient().when(event.getTitle()).thenReturn(title);
        lenient().when(event.getCategory()).thenReturn(category);
        lenient().when(event.getStartDate()).thenReturn(startDate);
        lenient().when(event.getEndDate()).thenReturn(endDate);
        lenient().when(event.getIsGlobal()).thenReturn(isGlobal);
        return event;
    }

    private User user(Long id, LocalDate stayExpirationDate) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        lenient().when(user.getStayExpirationDate()).thenReturn(stayExpirationDate);
        return user;
    }
}