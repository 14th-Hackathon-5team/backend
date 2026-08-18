package com.example.kbuddy.calendar.controller;

import com.example.kbuddy.calendar.dto.CalendarEventDetailResponse;
import com.example.kbuddy.calendar.dto.CalendarEventResponse;
import com.example.kbuddy.calendar.service.CalendarEventService;
import com.example.kbuddy.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "캘린더", description = "공통 일정과 개인 맞춤 일정을 조회하는 API")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/calendar/events")
public class CalendarEventController {

    private final CalendarEventService calendarEventService;

    @Operation(
            summary = "월별 일정 조회",
            description = "선택한 월의 공통 일정과 현재 로그인한 사용자의 개인 일정을 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공 (code: CALENDAR_MONTHLY_EVENTS_FETCHED)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 사용자 (code: USER_NOT_FOUND)"
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CalendarEventResponse>>> getMonthlyEvents(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam @Min(1900) @Max(2100) int year,
            @RequestParam @Min(1) @Max(12) int month
    ) {
        Long userId = Long.valueOf(jwt.getSubject());
        List<CalendarEventResponse> response = calendarEventService.getMonthlyEvents(userId, year, month);
        return ResponseEntity.ok(
                ApiResponse.success("CALENDAR_MONTHLY_EVENTS_FETCHED", "월별 일정을 조회했습니다.", response)
        );
    }

    @Operation(
            summary = "임박 일정 조회",
            description = "오늘부터 7일 이내의 공통 일정과 현재 로그인한 사용자의 개인 일정을 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공 (code: CALENDAR_UPCOMING_EVENTS_FETCHED)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 사용자 (code: USER_NOT_FOUND)"
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<List<CalendarEventResponse>>> getUpcomingEvents(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.valueOf(jwt.getSubject());
        List<CalendarEventResponse> response = calendarEventService.getUpcomingEvents(userId);
        return ResponseEntity.ok(
                ApiResponse.success("CALENDAR_UPCOMING_EVENTS_FETCHED", "임박 일정을 조회했습니다.", response)
        );
    }

    @Operation(
            summary = "숨긴 일정 목록 조회",
            description = "현재 로그인한 사용자가 삭제(숨김)한 일정 목록을 조회합니다. 복구 대상 확인용입니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공 (code: CALENDAR_HIDDEN_EVENTS_FETCHED)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 사용자 (code: USER_NOT_FOUND)"
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/hidden")
    public ResponseEntity<ApiResponse<List<CalendarEventResponse>>> getHiddenEvents(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.valueOf(jwt.getSubject());
        List<CalendarEventResponse> response = calendarEventService.getHiddenEvents(userId);
        return ResponseEntity.ok(
                ApiResponse.success("CALENDAR_HIDDEN_EVENTS_FETCHED", "숨긴 일정 목록을 조회했습니다.", response)
        );
    }

    @Operation(
            summary = "일정 상세 조회",
            description = "선택한 공통 일정 또는 현재 로그인한 사용자의 개인 일정 상세 설명과 관련 링크를 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공 (code: CALENDAR_EVENT_DETAIL_FETCHED)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 사용자(code: USER_NOT_FOUND) 또는 일정 없음(code: CALENDAR_EVENT_NOT_FOUND)"
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{eventId}")
    public ResponseEntity<ApiResponse<CalendarEventDetailResponse>> getEventDetail(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long eventId
    ) {
        Long userId = Long.valueOf(jwt.getSubject());
        CalendarEventDetailResponse response = calendarEventService.getEventDetail(userId, eventId);
        return ResponseEntity.ok(
                ApiResponse.success("CALENDAR_EVENT_DETAIL_FETCHED", "일정 상세 정보를 조회했습니다.", response)
        );
    }

    @Operation(
            summary = "일정 완료 체크 토글",
            description = "선택한 일정의 완료 여부를 토글합니다. 미완료 상태면 완료로, 완료 상태면 다시 미완료로 전환됩니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "처리 성공 (code: CALENDAR_EVENT_COMPLETION_TOGGLED)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 사용자(code: USER_NOT_FOUND) 또는 일정 없음(code: CALENDAR_EVENT_NOT_FOUND)"
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{eventId}/complete")
    public ResponseEntity<ApiResponse<CalendarEventResponse>> toggleCompleted(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long eventId
    ) {
        Long userId = Long.valueOf(jwt.getSubject());
        CalendarEventResponse response = calendarEventService.toggleCompleted(userId, eventId);
        return ResponseEntity.ok(
                ApiResponse.success("CALENDAR_EVENT_COMPLETION_TOGGLED", "일정 완료 상태를 변경했습니다.", response)
        );
    }

    @Operation(
            summary = "일정 숨기기(삭제)",
            description = "선택한 일정을 체크리스트 목록에서 숨깁니다. 실제로 삭제되지 않으며 복구 API로 되돌릴 수 있습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "처리 성공 (code: CALENDAR_EVENT_HIDDEN)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 사용자(code: USER_NOT_FOUND) 또는 일정 없음(code: CALENDAR_EVENT_NOT_FOUND)"
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{eventId}")
    public ResponseEntity<ApiResponse<Void>> hide(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long eventId
    ) {
        Long userId = Long.valueOf(jwt.getSubject());
        calendarEventService.hide(userId, eventId);
        return ResponseEntity.ok(
                ApiResponse.success("CALENDAR_EVENT_HIDDEN", "일정을 숨겼습니다.")
        );
    }

    @Operation(
            summary = "숨긴 일정 복구",
            description = "숨김(삭제) 처리했던 일정을 다시 체크리스트 목록에 복구합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "처리 성공 (code: CALENDAR_EVENT_RESTORED)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 사용자(code: USER_NOT_FOUND) 또는 숨겨진 일정 없음(code: CALENDAR_EVENT_NOT_FOUND)"
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{eventId}/restore")
    public ResponseEntity<ApiResponse<Void>> restore(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long eventId
    ) {
        Long userId = Long.valueOf(jwt.getSubject());
        calendarEventService.restore(userId, eventId);
        return ResponseEntity.ok(
                ApiResponse.success("CALENDAR_EVENT_RESTORED", "일정을 복구했습니다.")
        );
    }
}
