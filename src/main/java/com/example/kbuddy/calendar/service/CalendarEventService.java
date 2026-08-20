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
import com.example.kbuddy.user.entity.Language;
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
    private static final String VISA_EXPIRATION_REMINDER_TITLE_EN = "Stay Expiration Notice (30 Days Left)";
    private static final String VISA_EXPIRATION_REMINDER_DESCRIPTION =
            "체류기간이 30일 후 만료됩니다. 체류기간 연장 등 필요한 절차를 미리 준비하세요.";
    private static final String VISA_EXPIRATION_REMINDER_DESCRIPTION_EN =
            "Your stay period will expire in 30 days. Please prepare necessary procedures such as a stay extension in advance.";
    private static final String VISA_EXPIRATION_REMINDER_RELATED_LINK = "https://www.hikorea.go.kr";

    /**
     * 만료 "당일" 알림도 위와 같은 이유로 실제 CalendarEvent row 없이 합성 일정으로 취급한다.
     * FE→BE 요청(체류기간 만료 체크리스트 항목을 실제 이벤트로)에서는 실제 DB row 자동 생성이나
     * 전용 완료 엔드포인트를 대안으로 제시했지만, 완료 체크는 CalendarEventStatus.eventId가
     * CalendarEvent에 대한 FK가 아니라 임의의 식별자를 그대로 저장/조회하므로 이미 정상 동작한다
     * (회귀 테스트로 확인). 그래서 회원가입/정보수정 시점 동기화나 새 엔드포인트 없이,
     * 기존 -1(D-30 안내) 패턴을 그대로 확장하는 것으로 충분하다.
     */
    private static final Long VISA_EXPIRATION_DAY_EVENT_ID = -2L;
    private static final String VISA_EXPIRATION_DAY_TITLE = "체류기간 만료";
    private static final String VISA_EXPIRATION_DAY_TITLE_EN = "Stay Expiration Today";
    private static final String VISA_EXPIRATION_DAY_DESCRIPTION =
            "오늘 체류기간이 만료됩니다. 체류기간 연장 등 필요한 절차를 아직 안 하셨다면 서둘러 확인하세요.";
    private static final String VISA_EXPIRATION_DAY_DESCRIPTION_EN =
            "Your stay period expires today. If you haven't completed necessary procedures such as a stay extension yet, please check immediately.";

    private final CalendarEventRepository calendarEventRepository;
    private final CalendarEventStatusRepository calendarEventStatusRepository;
    private final UserRepository userRepository;
    private final CalendarEventTextResolver textResolver;

    /**
     * 체류만료 합성 title/description은 실제 CalendarEvent row가 없어 {@link CalendarEventTextResolver}가
     * 다루는 "DB 원본 한국어를 파싱해 영어로 재구성"하는 방식을 적용할 수 없다. 두 언어 버전을 상수로
     * 미리 준비해두고 language로 고르기만 한다.
     */
    private String visaReminderTitle(boolean isDayOf, Language language) {
        if (language == Language.ENGLISH) {
            return isDayOf ? VISA_EXPIRATION_DAY_TITLE_EN : VISA_EXPIRATION_REMINDER_TITLE_EN;
        }
        return isDayOf ? VISA_EXPIRATION_DAY_TITLE : VISA_EXPIRATION_REMINDER_TITLE;
    }

    private String visaReminderDescription(boolean isDayOf, Language language) {
        if (language == Language.ENGLISH) {
            return isDayOf ? VISA_EXPIRATION_DAY_DESCRIPTION_EN : VISA_EXPIRATION_REMINDER_DESCRIPTION_EN;
        }
        return isDayOf ? VISA_EXPIRATION_DAY_DESCRIPTION : VISA_EXPIRATION_REMINDER_DESCRIPTION;
    }

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
        if (isVisaReminderId(eventId)) {
            return getVisaReminderDetail(userId, eventId);
        }

        User user = findUserById(userId);
        CalendarEvent event = calendarEventRepository.findVisibleEventById(userId, eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CALENDAR_EVENT_NOT_FOUND));

        return new CalendarEventDetailResponse(
                event.getId(),
                textResolver.resolveTitle(user.getLanguage(), event.getCategory(), event.getTitle()),
                event.getCategory(),
                event.getStartDate(),
                event.getEndDate(),
                event.getIsGlobal(),
                textResolver.resolveDescription(user.getLanguage(), event.getCategory(), event.getDescription()),
                event.getRelatedLink()
        );
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
        List<VisaReminder> visaReminders = buildVisaReminders(user, startDate, endDate);

        List<Long> eventIds = new ArrayList<>(events.stream().map(CalendarEvent::getId).toList());
        visaReminders.forEach(reminder -> eventIds.add(reminder.eventId()));

        Map<Long, CalendarEventStatus> statusByEventId = loadStatusMap(user.getId(), eventIds);

        List<CalendarEventResponse> responses = new ArrayList<>();
        for (CalendarEvent event : events) {
            CalendarEventStatus status = statusByEventId.get(event.getId());
            if (status != null && status.isHidden()) {
                continue;
            }
            String displayTitle = textResolver.resolveTitle(user.getLanguage(), event.getCategory(), event.getTitle());
            responses.add(new CalendarEventResponse(
                    event.getId(), displayTitle, event.getCategory(), event.getStartDate(), event.getEndDate(),
                    event.getIsGlobal(), status != null && status.getCompleted()));
        }

        for (VisaReminder reminder : visaReminders) {
            CalendarEventStatus status = statusByEventId.get(reminder.eventId());
            if (status != null && status.isHidden()) {
                continue;
            }
            responses.add(reminder.snapshot().toResponse(reminder.eventId(), status != null && status.getCompleted()));
        }

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

    private boolean isVisaReminderId(Long eventId) {
        return VISA_EXPIRATION_REMINDER_EVENT_ID.equals(eventId) || VISA_EXPIRATION_DAY_EVENT_ID.equals(eventId);
    }

    /**
     * stayExpirationDate 하나로부터 D-30 안내, 만료 당일 두 합성 일정을 모두 만든다.
     * startDate/endDate 범위를 벗어나는 것은 결과에서 제외한다(월별/임박 조회 범위 필터링용).
     */
    private List<VisaReminder> buildVisaReminders(User user, LocalDate startDate, LocalDate endDate) {
        LocalDate stayExpirationDate = user.getStayExpirationDate();
        if (stayExpirationDate == null) {
            return List.of();
        }

        Language language = user.getLanguage();
        List<VisaReminder> reminders = new ArrayList<>();
        LocalDate thirtyDaysBefore = stayExpirationDate.minusDays(VISA_EXPIRATION_REMINDER_DAYS_BEFORE);
        if (!thirtyDaysBefore.isBefore(startDate) && !thirtyDaysBefore.isAfter(endDate)) {
            reminders.add(new VisaReminder(VISA_EXPIRATION_REMINDER_EVENT_ID,
                    new EventSnapshot(visaReminderTitle(false, language), EventCategory.VISA, thirtyDaysBefore, null, false)));
        }
        if (!stayExpirationDate.isBefore(startDate) && !stayExpirationDate.isAfter(endDate)) {
            reminders.add(new VisaReminder(VISA_EXPIRATION_DAY_EVENT_ID,
                    new EventSnapshot(visaReminderTitle(true, language), EventCategory.VISA, stayExpirationDate, null, false)));
        }
        return reminders;
    }

    private CalendarEventDetailResponse getVisaReminderDetail(Long userId, Long eventId) {
        User user = findUserById(userId);
        LocalDate stayExpirationDate = user.getStayExpirationDate();
        if (stayExpirationDate == null) {
            throw new BusinessException(ErrorCode.CALENDAR_EVENT_NOT_FOUND);
        }

        boolean isDayOf = VISA_EXPIRATION_DAY_EVENT_ID.equals(eventId);
        LocalDate date = isDayOf ? stayExpirationDate : stayExpirationDate.minusDays(VISA_EXPIRATION_REMINDER_DAYS_BEFORE);

        return new CalendarEventDetailResponse(
                eventId,
                visaReminderTitle(isDayOf, user.getLanguage()),
                EventCategory.VISA,
                date,
                null,
                false,
                visaReminderDescription(isDayOf, user.getLanguage()),
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
        if (isVisaReminderId(eventId)) {
            LocalDate stayExpirationDate = user.getStayExpirationDate();
            if (stayExpirationDate == null) {
                return Optional.empty();
            }
            boolean isDayOf = VISA_EXPIRATION_DAY_EVENT_ID.equals(eventId);
            LocalDate date = isDayOf ? stayExpirationDate : stayExpirationDate.minusDays(VISA_EXPIRATION_REMINDER_DAYS_BEFORE);
            String title = visaReminderTitle(isDayOf, user.getLanguage());
            return Optional.of(new EventSnapshot(title, EventCategory.VISA, date, null, false));
        }

        return calendarEventRepository.findVisibleEventById(user.getId(), eventId)
                .map(event -> new EventSnapshot(
                        textResolver.resolveTitle(user.getLanguage(), event.getCategory(), event.getTitle()),
                        event.getCategory(), event.getStartDate(), event.getEndDate(), event.getIsGlobal()));
    }

    private CalendarEventStatus findOrCreateStatus(User user, Long eventId) {
        return calendarEventStatusRepository.findByUserIdAndEventId(user.getId(), eventId)
                .orElseGet(() -> calendarEventStatusRepository.save(new CalendarEventStatus(user, eventId)));
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

    private record VisaReminder(Long eventId, EventSnapshot snapshot) {
    }
}
