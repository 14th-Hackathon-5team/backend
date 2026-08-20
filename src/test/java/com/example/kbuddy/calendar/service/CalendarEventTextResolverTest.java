package com.example.kbuddy.calendar.service;

import com.example.kbuddy.calendar.entity.EventCategory;
import com.example.kbuddy.user.entity.Language;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CalendarEventTextResolverTest {

    private final CalendarEventTextResolver resolver = new CalendarEventTextResolver();

    @Test
    void KOREAN_사용자는_title이_원본_그대로_반환된다() {
        String result = resolver.resolveTitle(Language.KOREAN, EventCategory.TOPIK_APPLICATION, "TOPIK 제109회 PBT 접수기간");

        assertThat(result).isEqualTo("TOPIK 제109회 PBT 접수기간");
    }

    @Test
    void ENGLISH_사용자는_TOPIK_APPLICATION_title이_영어로_변환된다() {
        String result = resolver.resolveTitle(Language.ENGLISH, EventCategory.TOPIK_APPLICATION, "TOPIK 제109회 PBT 접수기간");

        assertThat(result).isEqualTo("TOPIK Round 109 (PBT) Application Period");
    }

    @Test
    void ENGLISH_사용자는_TOPIK_EXAM_title이_영어로_변환된다() {
        String result = resolver.resolveTitle(Language.ENGLISH, EventCategory.TOPIK_EXAM, "TOPIK 제15회 IBT 시험일");

        assertThat(result).isEqualTo("TOPIK Round 15 (IBT) Exam Date");
    }

    @Test
    void ENGLISH_사용자여도_패턴이_맞지_않으면_원본을_그대로_반환한다() {
        String result = resolver.resolveTitle(Language.ENGLISH, EventCategory.TOPIK_EXAM, "알 수 없는 형식의 제목");

        assertThat(result).isEqualTo("알 수 없는 형식의 제목");
    }

    @Test
    void ENGLISH_사용자여도_VISA_카테고리는_원본을_그대로_반환한다() {
        String result = resolver.resolveTitle(Language.ENGLISH, EventCategory.VISA, "체류기간 만료");

        assertThat(result).isEqualTo("체류기간 만료");
    }

    @Test
    void null_language는_원본을_그대로_반환한다() {
        String result = resolver.resolveTitle(null, EventCategory.TOPIK_APPLICATION, "TOPIK 제109회 PBT 접수기간");

        assertThat(result).isEqualTo("TOPIK 제109회 PBT 접수기간");
    }

    @Test
    void ENGLISH_사용자는_접수기간_description이_영어로_변환된다() {
        String result = resolver.resolveDescription(
                Language.ENGLISH, EventCategory.TOPIK_APPLICATION, "제109회 TOPIK PBT 접수기간입니다.");

        assertThat(result).isEqualTo("This is the application period for TOPIK Round 109 (PBT).");
    }

    @Test
    void ENGLISH_사용자는_성적_발표일이_포함된_시험일_description이_영어로_변환된다() {
        String result = resolver.resolveDescription(
                Language.ENGLISH, EventCategory.TOPIK_EXAM, "제109회 TOPIK PBT 시험일입니다. 성적 발표는 12월 22일 예정입니다.");

        assertThat(result).isEqualTo("This is the exam date for TOPIK Round 109 (PBT). Results are expected to be announced on December 22.");
    }

    @Test
    void ENGLISH_사용자는_성적_발표일이_없는_시험일_description도_영어로_변환된다() {
        String result = resolver.resolveDescription(
                Language.ENGLISH, EventCategory.TOPIK_EXAM, "제15회 TOPIK IBT 시험일입니다.");

        assertThat(result).isEqualTo("This is the exam date for TOPIK Round 15 (IBT).");
    }

    @Test
    void description이_null이면_null을_그대로_반환한다() {
        String result = resolver.resolveDescription(Language.ENGLISH, EventCategory.TOPIK_EXAM, null);

        assertThat(result).isNull();
    }

    @Test
    void KOREAN_사용자는_description이_원본_그대로_반환된다() {
        String result = resolver.resolveDescription(
                Language.KOREAN, EventCategory.TOPIK_APPLICATION, "제109회 TOPIK PBT 접수기간입니다.");

        assertThat(result).isEqualTo("제109회 TOPIK PBT 접수기간입니다.");
    }
}
