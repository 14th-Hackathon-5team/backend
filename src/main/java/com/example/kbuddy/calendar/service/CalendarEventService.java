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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CalendarEventService {

    private static final long VISA_EXPIRATION_REMINDER_DAYS_BEFORE = 30;

    /**
     * 체류만료 알림은 실제 CalendarEvent row 없이 User.stayExpirationDate로부터 매번 계산되는
     * 합성(synthetic) 일정이라, 완료 체크/숨김 상태를 저장할 실제 event_id가 없다. 이 상수를
     * CalendarEventStatus.eventId로 재사용해 완료 체크 대상을 식별한다. TriggerService(알림 도메인)가
     * D-1 재알림에서 완료 여부를 물어볼 때도 이 상수를 그대로 참조하므로 public으로 노출한다.
     */
    public static final Long VISA_EXPIRATION_REMINDER_EVENT_ID = -1L;
    private static final String VISA_EXPIRATION_REMINDER_TITLE = "체류기간 만료 30일 전 안내";
    private static final String VISA_EXPIRATION_REMINDER_DESCRIPTION =
            "체류기간이 30일 후 만료됩니다. 체류기간 연장 등 필요한 절차를 미리 준비하세요.";
    private static final String VISA_EXPIRATION_REMINDER_RELATED_LINK = "https://www.hikorea.go.kr";

    private final CalendarEventRepository calendarEventRepository;
    private final CalendarEventStatusRepository calendarEventStatusRepository;
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
        if (VISA_EXPIRATION_REMINDER_EVENT_ID.equals(eventId)) {
            return getVisaExpirationReminderDetail(userId);
        }

        validateUserExists(userId);
        CalendarEvent event = calendarEventRepository.findVisibleEventById(userId, eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CALENDAR_EVENT_NOT_FOUND));

        return CalendarEventDetailResponse.from(event);
    }

    @Transactional
    public CalendarEventResponse toggleCompleted(Long userId, Long eventId) {
        User user = findUserById(userId);
        EventSnapshot snapshot = loadEventSnapshot(user, eventId);
        CalendarEventStatus status = findOrCreateStatus(user, eventId);
        status.toggleCompleted();

        return snapshot.toResponse(eventId, status.getCompleted());
    }

    @Transactional
    public void hide(Long userId, Long eventId) {
        User user = findUserById(userId);
        loadEventSnapshot(user, eventId);
        CalendarEventStatus status = findOrCreateStatus(user, eventId);
        status.hide();
    }

    @Transactional
    public void restore(Long userId, Long eventId) {
        User user = findUserById(userId);
        CalendarEventStatus status = calendarEventStatusRepository.findByUserIdAndEventId(user.getId(), eventId)
                .filter(CalendarEventStatus::isHidden)
                .orElseThrow(() -> new BusinessException(ErrorCode.CALENDAR_EVENT_NOT_FOUND));

        status.restore();
    }

    /**
     * User가 특정 eventId(실제 CalendarEvent.id 또는 {@link #VISA_EXPIRATION_REMINDER_EVENT_ID})를
     * 완료 체크했는지 조회한다. Notification/Trigger 도메인이 D-1 재알림에서 "이미 체크했으면 건너뛴다"
     * 정책을 판단할 때 사용한다. 상태 row가 없으면(한 번도 체크/숨김 조작을 안 한 경우) 미완료로 간주한다.
     */
    @Transactional(readOnly = true)
    public boolean isCompletedByUser(Long userId, Long eventId) {
        return calendarEventStatusRepository.findByUserIdAndEventId(userId, eventId)
                .map(CalendarEventStatus::getCompleted)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<CalendarEventResponse> getHiddenEvents(Long userId) {
        User user = findUserById(userId);
        List<CalendarEventStatus> hiddenStatuses = calendarEventStatusRepository
                .findByUserIdAndHiddenAtIsNotNull(user.getId());

        List<CalendarEventResponse> responses = new ArrayList<>();
        for (CalendarEventStatus status : hiddenStatuses) {
            loadEventSnapshotOrNull(user, status.getEventId())
                    .ifPresent(snapshot -> responses.add(snapshot.toResponse(status.getEventId(), status.getCompleted())));
        }
        return responses.stream()
                .sorted(Comparator.comparing(CalendarEventResponse::startDate))
                .toList();
    }

    private List<CalendarEventResponse> getVisibleEvents(User user, LocalDate startDate, LocalDate endDate) {
        List<CalendarEvent> events = calendarEventRepository.findVisibleEventsBetween(user.getId(), startDate, endDate);

        List<Long> eventIds = new ArrayList<>(events.stream().map(CalendarEvent::getId).toList());
        buildVisaExpirationReminder(user, startDate, endDate)
                .ifPresent(reminder -> eventIds.add(VISA_EXPIRATION_REMINDER_EVENT_ID));

        Map<Long, CalendarEventStatus> statusByEventId = loadStatusMap(user.getId(), eventIds);

        List<CalendarEventResponse> responses = new ArrayList<>();
        for (CalendarEvent event : events) {
            CalendarEventStatus status = statusByEventId.get(event.getId());
            if (status != null && status.isHidden()) {
                continue;
            }
            responses.add(CalendarEventResponse.from(event, status != null && status.getCompleted()));
        }

        buildVisaExpirationReminder(user, startDate, endDate).ifPresent(reminder -> {
            CalendarEventStatus status = statusByEventId.get(VISA_EXPIRATION_REMINDER_EVENT_ID);
            if (status != null && status.isHidden()) {
                return;
            }
            responses.add(reminder.toResponse(VISA_EXPIRATION_REMINDER_EVENT_ID, status != null && status.getCompleted()));
        });

        return responses.stream()
                .sorted(Comparator.comparing(CalendarEventResponse::startDate))
                .toList();
    }

    private Map<Long, CalendarEventStatus> loadStatusMap(Long userId, List<Long> eventIds) {
        Map<Long, CalendarEventStatus> statusByEventId = new HashMap<>();
        for (CalendarEventStatus status : calendarEventStatusRepository.findByUserIdAndEventIdIn(userId, eventIds)) {
            statusByEventId.put(status.getEventId(), status);
        }
        return statusByEventId;
    }

    private Optional<EventSnapshot> buildVisaExpirationReminder(User user, LocalDate startDate, LocalDate endDate) {
        LocalDate stayExpirationDate = user.getStayExpirationDate();
        if (stayExpirationDate == null) {
            return Optional.empty();
        }

        LocalDate reminderDate = stayExpirationDate.minusDays(VISA_EXPIRATION_REMINDER_DAYS_BEFORE);
        if (reminderDate.isBefore(startDate) || reminderDate.isAfter(endDate)) {
            return Optional.empty();
        }

        return Optional.of(new EventSnapshot(VISA_EXPIRATION_REMINDER_TITLE, EventCategory.VISA, reminderDate, null, false));
    }

    private CalendarEventDetailResponse getVisaExpirationReminderDetail(Long userId) {
        User user = findUserById(userId);
        LocalDate stayExpirationDate = user.getStayExpirationDate();
        if (stayExpirationDate == null) {
            throw new BusinessException(ErrorCode.CALENDAR_EVENT_NOT_FOUND);
        }

        LocalDate reminderDate = stayExpirationDate.minusDays(VISA_EXPIRATION_REMINDER_DAYS_BEFORE);

        return new CalendarEventDetailResponse(
                VISA_EXPIRATION_REMINDER_EVENT_ID,
                VISA_EXPIRATION_REMINDER_TITLE,
                EventCategory.VISA,
                reminderDate,
                null,
                false,
                VISA_EXPIRATION_REMINDER_DESCRIPTION,
                VISA_EXPIRATION_REMINDER_RELATED_LINK
        );
    }

    /**
     * 체크(완료)/숨김 대상 일정을 조회 범위(월별/임박) 제약 없이, ID만으로 조회한다.
     * 이미 지난 달의 일정이라도 완료 취소/복구를 할 수 있어야 하므로 날짜 범위를 걸지 않는다.
     */
    private EventSnapshot loadEventSnapshot(User user, Long eventId) {
        return loadEventSnapshotOrNull(user, eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CALENDAR_EVENT_NOT_FOUND));
    }

    private Optional<EventSnapshot> loadEventSnapshotOrNull(User user, Long eventId) {
        if (VISA_EXPIRATION_REMINDER_EVENT_ID.equals(eventId)) {
            LocalDate stayExpirationDate = user.getStayExpirationDate();
            if (stayExpirationDate == null) {
                return Optional.empty();
            }
            LocalDate reminderDate = stayExpirationDate.minusDays(VISA_EXPIRATION_REMINDER_DAYS_BEFORE);
            return Optional.of(new EventSnapshot(VISA_EXPIRATION_REMINDER_TITLE, EventCategory.VISA, reminderDate, null, false));
        }

        return calendarEventRepository.findVisibleEventById(user.getId(), eventId)
                .map(event -> new EventSnapshot(
                        event.getTitle(), event.getCategory(), event.getStartDate(), event.getEndDate(), event.getIsGlobal()));
    }

    private CalendarEventStatus findOrCreateStatus(User user, Long eventId) {
        return calendarEventStatusRepository.findByUserIdAndEventId(user.getId(), eventId)
                .orElseGet(() -> calendarEventStatusRepository.save(new CalendarEventStatus(user, eventId)));
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

    private record EventSnapshot(
            String title,
            EventCategory category,
            LocalDate startDate,
            LocalDate endDate,
            Boolean isGlobal
    ) {
        CalendarEventResponse toResponse(Long eventId, boolean completed) {
            return new CalendarEventResponse(eventId, title, category, startDate, endDate, isGlobal, completed);
        }
    }
}
