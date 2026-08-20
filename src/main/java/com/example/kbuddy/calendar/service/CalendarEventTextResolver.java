package com.example.kbuddy.calendar.service;

import com.example.kbuddy.calendar.entity.EventCategory;
import com.example.kbuddy.user.entity.Language;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TOPIK 캘린더 일정(title/description)을 사용자 {@link Language}에 맞춰 표시용 텍스트로 변환한다.
 * DB의 원본 한국어 문자열(회차/PBT·IBT/성적 발표일 등 실제 정보)은 그대로 두고, ENGLISH 사용자
 * 요청에 한해 매 응답 시점에 정규식으로 회차·시험형식을 파싱해 영어 문장으로 재구성한다.
 * 패턴이 맞지 않으면(향후 시드 문구가 달라지는 경우 등) 원본 한국어를 그대로 반환해 절대 깨지지 않게 한다.
 *
 * Calendar 도메인(CalendarEventService)과 TOPIK 알림 생성 경로(TriggerService.evaluateTopikTrigger)가
 * 이 클래스 하나를 공유해서, 같은 CalendarEvent의 title/description이 두 곳에서 서로 다른 방식으로
 * 번역되지 않도록 한다.
 */
@Component
public class CalendarEventTextResolver {

    private static final Pattern ROUND_FORMAT_PATTERN = Pattern.compile("제(\\d+)회.*?(PBT|IBT)");
    private static final Pattern SCORE_DATE_PATTERN = Pattern.compile("성적\\s*발표는\\s*(\\d{1,2})월\\s*(\\d{1,2})일");
    private static final String[] ENGLISH_MONTHS = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    };

    public String resolveTitle(Language language, EventCategory category, String koreanTitle) {
        if (language != Language.ENGLISH || koreanTitle == null) {
            return koreanTitle;
        }
        Matcher matcher = ROUND_FORMAT_PATTERN.matcher(koreanTitle);
        if (!matcher.find()) {
            return koreanTitle;
        }
        String round = matcher.group(1);
        String format = matcher.group(2);
        return switch (category) {
            case TOPIK_APPLICATION -> "TOPIK Round %s (%s) Application Period".formatted(round, format);
            case TOPIK_EXAM -> "TOPIK Round %s (%s) Exam Date".formatted(round, format);
            default -> koreanTitle;
        };
    }

    public String resolveDescription(Language language, EventCategory category, String koreanDescription) {
        if (language != Language.ENGLISH || koreanDescription == null) {
            return koreanDescription;
        }
        Matcher matcher = ROUND_FORMAT_PATTERN.matcher(koreanDescription);
        if (!matcher.find()) {
            return koreanDescription;
        }
        String round = matcher.group(1);
        String format = matcher.group(2);
        String base = switch (category) {
            case TOPIK_APPLICATION -> "This is the application period for TOPIK Round %s (%s).".formatted(round, format);
            case TOPIK_EXAM -> "This is the exam date for TOPIK Round %s (%s).".formatted(round, format);
            default -> null;
        };
        if (base == null) {
            return koreanDescription;
        }

        Matcher scoreDateMatcher = SCORE_DATE_PATTERN.matcher(koreanDescription);
        if (scoreDateMatcher.find()) {
            int month = Integer.parseInt(scoreDateMatcher.group(1));
            int day = Integer.parseInt(scoreDateMatcher.group(2));
            if (month >= 1 && month <= 12) {
                base += " Results are expected to be announced on %s %d.".formatted(ENGLISH_MONTHS[month - 1], day);
            }
        }
        return base;
    }
}
