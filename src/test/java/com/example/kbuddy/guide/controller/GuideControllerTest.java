package com.example.kbuddy.guide.controller;

import com.example.kbuddy.global.exception.GlobalExceptionHandler;
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

    @TestConfiguration
    static class TestConfig {

        @Bean
        GuideService guideService() {
            return mock(GuideService.class);
        }
    }
}
