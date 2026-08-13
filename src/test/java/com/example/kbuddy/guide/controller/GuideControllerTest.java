package com.example.kbuddy.guide.controller;

import com.example.kbuddy.global.exception.BusinessException;
import com.example.kbuddy.global.exception.ErrorCode;
import com.example.kbuddy.global.exception.GlobalExceptionHandler;
import com.example.kbuddy.guide.dto.GuideDetailResponse;
import com.example.kbuddy.guide.dto.GuideResponse;
import com.example.kbuddy.guide.entity.GuideCategory;
import com.example.kbuddy.guide.service.GuideService;
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GuideController.class)
@Import({GlobalExceptionHandler.class, GuideControllerTest.TestConfig.class})
class GuideControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GuideService guideService;

    @BeforeEach
    void resetGuideServiceMock() {
        Mockito.reset(guideService);
    }

    @Test
    void 카테고리별_가이드_목록_조회가_정상이면_200과_목록을_반환한다() throws Exception {
        when(guideService.getGuidesByCategory(GuideCategory.VISA))
                .thenReturn(List.of(new GuideResponse(1L, GuideCategory.VISA, "D-2 비자 연장 방법")));

        mockMvc.perform(get("/api/guides")
                        .param("category", "VISA")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("GUIDE_LIST_FETCHED"))
                .andExpect(jsonPath("$.data[0].guideId").value(1))
                .andExpect(jsonPath("$.data[0].category").value("VISA"));
    }

    @Test
    void 가이드_상세_조회가_정상이면_200과_상세_정보를_반환한다() throws Exception {
        when(guideService.getGuideDetail(1L))
                .thenReturn(new GuideDetailResponse(
                        1L,
                        GuideCategory.VISA,
                        "D-2 비자 연장 방법",
                        "D-2 비자는 학기 종료 전 연장 신청이 필요합니다.",
                        "https://www.hikorea.go.kr"
                ));

        mockMvc.perform(get("/api/guides/1")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("GUIDE_DETAIL_FETCHED"))
                .andExpect(jsonPath("$.data.guideId").value(1))
                .andExpect(jsonPath("$.data.content").value("D-2 비자는 학기 종료 전 연장 신청이 필요합니다."))
                .andExpect(jsonPath("$.data.referenceUrl").value("https://www.hikorea.go.kr"));
    }

    @Test
    void 가이드가_없으면_404를_반환한다() throws Exception {
        when(guideService.getGuideDetail(999L))
                .thenThrow(new BusinessException(ErrorCode.GUIDE_NOT_FOUND));

        mockMvc.perform(get("/api/guides/999")
                        .with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("GUIDE_NOT_FOUND"));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        GuideService guideService() {
            return mock(GuideService.class);
        }
    }
}
