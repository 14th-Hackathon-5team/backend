package com.example.kbuddy.news.controller;

import com.example.kbuddy.global.exception.GlobalExceptionHandler;
import com.example.kbuddy.news.client.NewsClient;
import com.example.kbuddy.news.client.NewsClientException;
import com.example.kbuddy.news.dto.NewsItem;
import com.example.kbuddy.news.dto.NewsResponse;
import com.example.kbuddy.user.dto.UserResponse;
import com.example.kbuddy.user.entity.HousingType;
import com.example.kbuddy.user.entity.Language;
import com.example.kbuddy.user.entity.PartTimeStatus;
import com.example.kbuddy.user.entity.TopikLevel;
import com.example.kbuddy.user.entity.UserStatus;
import com.example.kbuddy.user.entity.VisaType;
import com.example.kbuddy.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NewsController.class)
@Import({GlobalExceptionHandler.class, NewsControllerTest.TestConfig.class})
class NewsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private NewsClient newsClient;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(userService, newsClient);
    }

    private UserResponse sampleUserResponse(Long id, Language language) {
        return new UserResponse(
                id,
                "test@gmail.com",
                "김철수",
                "VN",
                2003,
                UserStatus.UNDERGRADUATE,
                "경북대학교",
                LocalDate.of(2026, 2, 20),
                VisaType.D2,
                true,
                LocalDate.of(2027, 2, 19),
                HousingType.DORMITORY,
                true,
                PartTimeStatus.NOT_PLANNED,
                TopikLevel.LEVEL_3,
                TopikLevel.LEVEL_5,
                language
        );
    }

    @Test
    void KOREAN_사용자가_조회하면_AI_서버에_language_ko로_호출한다() throws Exception {
        when(userService.getMyInfo(1L)).thenReturn(sampleUserResponse(1L, Language.KOREAN));
        when(newsClient.getNews("ko")).thenReturn(new NewsResponse(List.of(
                new NewsItem("제목", List.of("1줄", "2줄", "3줄"), "상세 요약", "https://example.com/article"))));

        mockMvc.perform(get("/api/news").with(jwt().jwt(b -> b.subject("1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("NEWS_FETCHED"))
                .andExpect(jsonPath("$.data[0].title").value("제목"))
                .andExpect(jsonPath("$.data[0].threeLineSummary[0]").value("1줄"))
                .andExpect(jsonPath("$.data[0].link").value("https://example.com/article"));

        verify(newsClient).getNews("ko");
    }

    @Test
    void ENGLISH_사용자가_조회하면_AI_서버에_language_en으로_호출한다() throws Exception {
        when(userService.getMyInfo(1L)).thenReturn(sampleUserResponse(1L, Language.ENGLISH));
        when(newsClient.getNews("en")).thenReturn(new NewsResponse(List.of()));

        mockMvc.perform(get("/api/news").with(jwt().jwt(b -> b.subject("1"))))
                .andExpect(status().isOk());

        verify(newsClient).getNews("en");
    }

    @Test
    void 뉴스가_없으면_500이_아닌_200과_빈_배열을_반환한다() throws Exception {
        when(userService.getMyInfo(1L)).thenReturn(sampleUserResponse(1L, Language.KOREAN));
        when(newsClient.getNews("ko")).thenReturn(new NewsResponse(List.of()));

        mockMvc.perform(get("/api/news").with(jwt().jwt(b -> b.subject("1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void NewsClientException이_발생하면_503과_NEWS_SERVICE_UNAVAILABLE을_반환한다() throws Exception {
        when(userService.getMyInfo(1L)).thenReturn(sampleUserResponse(1L, Language.KOREAN));
        when(newsClient.getNews("ko")).thenThrow(
                new NewsClientException(NewsClientException.Reason.SERVER_UNAVAILABLE, "fail", new RuntimeException()));

        mockMvc.perform(get("/api/news").with(jwt().jwt(b -> b.subject("1"))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("NEWS_SERVICE_UNAVAILABLE"));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        UserService userService() {
            return mock(UserService.class);
        }

        @Bean
        NewsClient newsClient() {
            return mock(NewsClient.class);
        }

        // ClientRegistrationRepository를 직접 제공하지 않으면 Spring Boot의 OAuth2Client 자동설정이
        // application-local.yaml의 실제 client-id/secret으로 OAuth2ClientProperties를 바인딩하려다
        // 로컬 환경에 그 파일이 없을 때 실패한다(SecurityConfigTest.TestKeyConfig와 동일한 이유로 필요).
        // 이 테스트는 OAuth2 로그인 자체를 검증하지 않으므로 mock으로 충분하다.
        @Bean
        ClientRegistrationRepository clientRegistrationRepository() {
            return mock(ClientRegistrationRepository.class);
        }
    }
}
