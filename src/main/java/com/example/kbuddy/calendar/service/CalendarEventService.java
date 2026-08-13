package com.example.kbuddy.calendar.service;

import com.example.kbuddy.calendar.dto.CalendarEventResponse;
import com.example.kbuddy.calendar.repository.CalendarEventRepository;
import com.example.kbuddy.global.exception.BusinessException;
import com.example.kbuddy.global.exception.ErrorCode;
import com.example.kbuddy.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CalendarEventService {

    private final CalendarEventRepository calendarEventRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CalendarEventResponse> getMonthlyEvents(Long userId, int year, int month) {
        validateUserExists(userId);
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        return calendarEventRepository.findVisibleEventsBetween(userId, startDate, endDate).stream()
                .map(CalendarEventResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CalendarEventResponse> getUpcomingEvents(Long userId) {
        validateUserExists(userId);
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysLater = today.plusDays(7);

        return calendarEventRepository.findVisibleEventsBetween(userId, today, sevenDaysLater).stream()
                .map(CalendarEventResponse::from)
                .toList();
    }

    private void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }
}