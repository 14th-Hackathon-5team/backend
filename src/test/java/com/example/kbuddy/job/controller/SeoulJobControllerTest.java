package com.example.kbuddy.job.controller;

import com.example.kbuddy.global.exception.GlobalExceptionHandler;
import com.example.kbuddy.job.client.SeoulJobClientException;
import com.example.kbuddy.job.dto.SeoulJobResponse;
import com.example.kbuddy.job.service.SeoulJobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SeoulJobController.class)
@Import({GlobalExceptionHandler.class, SeoulJobControllerTest.TestConfig.class})
class SeoulJobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SeoulJobService seoulJobService;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(seoulJobService);
    }

    @Test
    void 인증된_User가_조회하면_200과_채용공고_목록을_반환한다() throws Exception {
        when(seoulJobService.search(1, 20)).thenReturn(List.of(
                new SeoulJobResponse("카페 아르바이트", "설명", "홍길동", "한국어", "2026-08-01", "2026-08-02")));

        mockMvc.perform(get("/api/external/seoul-jobs")
                        .param("startIndex", "1")
                        .param("endIndex", "20")
                        .with(jwt().jwt(b -> b.subject("1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SEOUL_JOB_LIST_FETCHED"))
                .andExpect(jsonPath("$.data[0].title").value("카페 아르바이트"))
                .andExpect(jsonPath("$.data[0].writerName").value("홍길동"));

        verify(seoulJobService).search(1, 20);
    }

    @Test
    void startIndex_endIndex를_생략하면_기본값_1과_20으로_조회한다() throws Exception {
        when(seoulJobService.search(1, 20)).thenReturn(List.of());

        mockMvc.perform(get("/api/external/seoul-jobs")
                        .with(jwt().jwt(b -> b.subject("1"))))
                .andExpect(status().isOk());

        verify(seoulJobService).search(eq(1), eq(20));
    }

    @Test
    void SeoulJobClientException이_발생하면_503과_SEOUL_JOB_SERVICE_UNAVAILABLE을_반환한다() throws Exception {
        when(seoulJobService.search(1, 20)).thenThrow(
                new SeoulJobClientException(SeoulJobClientException.Reason.SERVER_UNAVAILABLE, "fail", new RuntimeException()));

        mockMvc.perform(get("/api/external/seoul-jobs")
                        .with(jwt().jwt(b -> b.subject("1"))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("SEOUL_JOB_SERVICE_UNAVAILABLE"));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        SeoulJobService seoulJobService() {
            return mock(SeoulJobService.class);
        }
    }
}
