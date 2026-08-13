package com.example.kbuddy.calendar.controller;

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
import org.springframework.web.bind.annotation.GetMapping;
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
}
