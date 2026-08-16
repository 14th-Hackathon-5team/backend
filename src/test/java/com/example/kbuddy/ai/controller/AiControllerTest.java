package com.example.kbuddy.ai.controller;

import com.example.kbuddy.ai.client.AiClientException;
import com.example.kbuddy.ai.dto.AiChatRequest;
import com.example.kbuddy.ai.dto.AiChatResponse;
import com.example.kbuddy.ai.service.AiService;
import com.example.kbuddy.ai.service.AiUserMapper;
import com.example.kbuddy.global.exception.GlobalExceptionHandler;
import com.example.kbuddy.notification.entity.NotificationCategory;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AiController.class)
@Import({GlobalExceptionHandler.class, AiUserMapper.class, AiControllerTest.TestConfig.class})
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private AiService aiService;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(userService, aiService);
    }

    private UserResponse sampleUserResponse(Long id) {
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
                Language.KOREAN
        );
    }

    @Test
    void 인증된_User가_message를_보내면_200과_AI_응답을_반환한다() throws Exception {
        when(userService.getMyInfo(1L)).thenReturn(sampleUserResponse(1L));
        when(aiService.chat(any())).thenReturn(new AiChatResponse(
                "답변", NotificationCategory.PART_TIME, List.of("질문1"), List.of(Map.of("dataset", "law"))));

        mockMvc.perform(post("/ai/chat")
                        .with(jwt().jwt(b -> b.subject("1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "D-2 비자인데 아르바이트를 시작하려면 어떻게 해야 해?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("AI_CHAT_RESPONDED"))
                .andExpect(jsonPath("$.data.answer").value("답변"))
                .andExpect(jsonPath("$.data.category").value("PART_TIME"))
                .andExpect(jsonPath("$.data.suggestedQuestions[0]").value("질문1"))
                .andExpect(jsonPath("$.data.sources[0].dataset").value("law"));

        verify(userService).getMyInfo(1L);
    }

    @Test
    void User_정보가_AiUser로_정확히_매핑되고_nationality는_변환되지_않으며_message가_그대로_전달된다() throws Exception {
        when(userService.getMyInfo(1L)).thenReturn(sampleUserResponse(1L));
        when(aiService.chat(any())).thenReturn(new AiChatResponse("답변", NotificationCategory.SUPPORT, List.of(), List.of()));

        mockMvc.perform(post("/ai/chat")
                        .with(jwt().jwt(b -> b.subject("1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "질문 내용입니다"}
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<AiChatRequest> captor = ArgumentCaptor.forClass(AiChatRequest.class);
        verify(aiService).chat(captor.capture());
        AiChatRequest sent = captor.getValue();

        assertThat(sent.message()).isEqualTo("질문 내용입니다");
        assertThat(sent.user().userId()).isEqualTo(1L);
        assertThat(sent.user().nationality()).isEqualTo("VN");
        assertThat(sent.user().visaType()).isEqualTo(VisaType.D2);
        assertThat(sent.user().schoolName()).isEqualTo("경북대학교");
        assertThat(sent.user().currentTopikLevel()).isEqualTo(TopikLevel.LEVEL_3);
        assertThat(sent.user().targetTopikLevel()).isEqualTo(TopikLevel.LEVEL_5);
    }

    @Test
    void AiClientException이_발생하면_503과_AI_SERVICE_UNAVAILABLE을_반환한다() throws Exception {
        when(userService.getMyInfo(1L)).thenReturn(sampleUserResponse(1L));
        when(aiService.chat(any())).thenThrow(
                new AiClientException(AiClientException.Reason.SERVER_UNAVAILABLE, "fail", new RuntimeException()));

        mockMvc.perform(post("/ai/chat")
                        .with(jwt().jwt(b -> b.subject("1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "질문"}
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AI_SERVICE_UNAVAILABLE"));
    }

    @Test
    void message가_빈_문자열이면_400을_반환하고_AI를_호출하지_않는다() throws Exception {
        mockMvc.perform(post("/ai/chat")
                        .with(jwt().jwt(b -> b.subject("1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(userService, aiService);
    }

    @Test
    void message가_없으면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/ai/chat")
                        .with(jwt().jwt(b -> b.subject("1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        UserService userService() {
            return mock(UserService.class);
        }

        @Bean
        AiService aiService() {
            return mock(AiService.class);
        }
    }
}
