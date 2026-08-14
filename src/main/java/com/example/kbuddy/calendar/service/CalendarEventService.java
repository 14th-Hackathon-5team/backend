package com.example.kbuddy.calendar.service;

import com.example.kbuddy.calendar.dto.CalendarEventDetailResponse;
import com.example.kbuddy.calendar.dto.CalendarEventResponse;
import com.example.kbuddy.calendar.entity.CalendarEvent;
import com.example.kbuddy.calendar.entity.EventCategory;
import com.example.kbuddy.calendar.repository.CalendarEventRepository;
import com.example.kbuddy.global.exception.BusinessException;
import com.example.kbuddy.global.exception.ErrorCode;
import com.example.kbuddy.user.entity.User;
import com.example.kbuddy.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CalendarEventService {

    private static final long VISA_EXPIRATION_REMINDER_DAYS_BEFORE = 30;
    private static final Long VISA_EXPIRATION_REMINDER_EVENT_ID = -1L;

    private final CalendarEventRepository calendarEventRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CalendarEventResponse> getMonthlyEvents(Long userId, int year, int month) {
        User user = findUserById(userId);
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        return getVisibleEvents(user, startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<CalendarEventResponse> getUpcomingEvents(Long userId) {
        User user = findUserById(userId);
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysLater = today.plusDays(7);

        return getVisibleEvents(user, today, sevenDaysLater);
    }

    @Transactional(readOnly = true)
    public CalendarEventDetailResponse getEventDetail(Long userId, Long eventId) {
        validateUserExists(userId);
        CalendarEvent event = calendarEventRepository.findVisibleEventById(userId, eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CALENDAR_EVENT_NOT_FOUND));

        return CalendarEventDetailResponse.from(event);
    }

    private List<CalendarEventResponse> getVisibleEvents(User user, LocalDate startDate, LocalDate endDate) {
        List<CalendarEventResponse> responses = new ArrayList<>(
                calendarEventRepository.findVisibleEventsBetween(user.getId(), startDate, endDate).stream()
                        .map(CalendarEventResponse::from)
                        .toList()
        );

        buildVisaExpirationReminder(user, startDate, endDate).ifPresent(responses::add);

        return responses.stream()
                .sorted(Comparator.comparing(CalendarEventResponse::startDate))
                .toList();
    }

    private Optional<CalendarEventResponse> buildVisaExpirationReminder(
            User user,
            LocalDate startDate,
            LocalDate endDate
    ) {
        LocalDate stayExpirationDate = user.getStayExpirationDate();
        if (stayExpirationDate == null) {
            return Optional.empty();
        }

        LocalDate reminderDate = stayExpirationDate.minusDays(VISA_EXPIRATION_REMINDER_DAYS_BEFORE);
        if (reminderDate.isBefore(startDate) || reminderDate.isAfter(endDate)) {
            return Optional.empty();
        }

        return Optional.of(new CalendarEventResponse(
                VISA_EXPIRATION_REMINDER_EVENT_ID,
                "체류기간 만료 30일 전 안내",
                EventCategory.VISA,
                reminderDate,
                null,
                false
        ));
    }

    private void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
