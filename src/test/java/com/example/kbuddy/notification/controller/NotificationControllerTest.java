package com.example.kbuddy.notification.controller;

import com.example.kbuddy.global.exception.BusinessException;
import com.example.kbuddy.global.exception.ErrorCode;
import com.example.kbuddy.global.exception.GlobalExceptionHandler;
import com.example.kbuddy.notification.dto.NotificationResponse;
import com.example.kbuddy.notification.entity.NotificationCategory;
import com.example.kbuddy.notification.entity.NotificationTriggerType;
import com.example.kbuddy.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NotificationController.class)
@Import({GlobalExceptionHandler.class, NotificationControllerTest.TestConfig.class})
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationService notificationService;

    @BeforeEach
    void resetMock() {
        Mockito.reset(notificationService);
    }

    private NotificationResponse response(Long id, Map<String, Object> source, boolean isRead, LocalDateTime readAt) {
        return new NotificationResponse(
                id,
                NotificationCategory.VISA,
                "체류기간 만료 30일 전입니다",
                "현재 비자가 D-2입니다.",
                "요약",
                Map.of("deadline", "2026-09-20"),
                source,
                5,
                NotificationTriggerType.VISA_EXPIRATION,
                LocalDate.of(2026, 8, 17),
                isRead,
                readAt,
                LocalDateTime.of(2026, 8, 17, 9, 0)
        );
    }

    @Test
    void GET_notifications가_정상이면_200과_목록을_반환한다() throws Exception {
        when(notificationService.getMyNotifications(1L)).thenReturn(List.of(
                response(1L, Map.of("dataset", "law"), false, null)));

        mockMvc.perform(get("/notifications").with(jwt().jwt(b -> b.subject("1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("NOTIFICATION_LIST_FETCHED"))
                .andExpect(jsonPath("$.data[0].notificationId").value(1))
                .andExpect(jsonPath("$.data[0].category").value("VISA"))
                .andExpect(jsonPath("$.data[0].priority").value(5))
                .andExpect(jsonPath("$.data[0].details.deadline").value("2026-09-20"))
                .andExpect(jsonPath("$.data[0].source.dataset").value("law"))
                .andExpect(jsonPath("$.data[0].isRead").value(false))
                .andExpect(jsonPath("$.data[0].readAt").value(nullValue()));
    }

    @Test
    void GET_notifications는_JWT의_subject를_userId로_사용해_조회한다() throws Exception {
        when(notificationService.getMyNotifications(42L)).thenReturn(List.of());

        mockMvc.perform(get("/notifications").with(jwt().jwt(b -> b.subject("42"))))
                .andExpect(status().isOk());

        verify(notificationService).getMyNotifications(42L);
    }

    @Test
    void source가_null이면_응답에서도_null로_반환된다() throws Exception {
        when(notificationService.getMyNotifications(1L)).thenReturn(List.of(
                response(1L, null, true, LocalDateTime.of(2026, 8, 17, 10, 0))));

        mockMvc.perform(get("/notifications").with(jwt().jwt(b -> b.subject("1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].source").value(nullValue()))
                .andExpect(jsonPath("$.data[0].isRead").value(true))
                .andExpect(jsonPath("$.data[0].readAt").value("2026-08-17T10:00:00"));
    }

    @Test
    void PATCH_notifications_read가_정상이면_200과_읽음_처리된_응답을_반환한다() throws Exception {
        NotificationResponse readResponse = response(10L, null, true, LocalDateTime.of(2026, 8, 17, 10, 0));
        when(notificationService.markAsRead(1L, 10L)).thenReturn(readResponse);

        mockMvc.perform(patch("/notifications/10/read").with(jwt().jwt(b -> b.subject("1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("NOTIFICATION_READ"))
                .andExpect(jsonPath("$.data.notificationId").value(10))
                .andExpect(jsonPath("$.data.isRead").value(true))
                .andExpect(jsonPath("$.data.readAt").value("2026-08-17T10:00:00"));
    }

    @Test
    void PATCH는_JWT의_userId와_경로의_notificationId를_함께_Service에_전달한다() throws Exception {
        when(notificationService.markAsRead(7L, 10L)).thenReturn(response(10L, null, true, LocalDateTime.now()));

        mockMvc.perform(patch("/notifications/10/read").with(jwt().jwt(b -> b.subject("7"))))
                .andExpect(status().isOk());

        verify(notificationService).markAsRead(7L, 10L);
    }

    @Test
    void 존재하지_않는_Notification을_읽음_처리하면_404와_NOTIFICATION_NOT_FOUND를_반환한다() throws Exception {
        when(notificationService.markAsRead(1L, 999L))
                .thenThrow(new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        mockMvc.perform(patch("/notifications/999/read").with(jwt().jwt(b -> b.subject("1"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"));
    }

    @Test
    void 다른_User_소유의_Notification을_읽음_처리하려_하면_404와_NOTIFICATION_NOT_FOUND를_반환한다() throws Exception {
        // User A(userId=1)가 User B 소유 notificationId=2를 요청 → Service가 ownership 불일치로 동일하게 NOTIFICATION_NOT_FOUND 처리
        when(notificationService.markAsRead(1L, 2L))
                .thenThrow(new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        mockMvc.perform(patch("/notifications/2/read").with(jwt().jwt(b -> b.subject("1"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        NotificationService notificationService() {
            return mock(NotificationService.class);
        }
    }
}
