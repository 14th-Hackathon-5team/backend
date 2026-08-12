package com.example.kbuddy.user.controller;

import com.example.kbuddy.global.exception.BusinessException;
import com.example.kbuddy.global.exception.ErrorCode;
import com.example.kbuddy.global.exception.GlobalExceptionHandler;
import com.example.kbuddy.user.dto.UserSignupRequest;
import com.example.kbuddy.user.dto.UserSignupResponse;
import com.example.kbuddy.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, UserControllerTest.TestConfig.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Test
    void 정상_요청이면_200과_ACCESS_TOKEN을_반환한다() throws Exception {
        when(userService.signup(any(), any(UserSignupRequest.class)))
                .thenReturn(new UserSignupResponse("access-token-value"));

        mockMvc.perform(post("/api/users/me")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("USER_CREATED"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token-value"));
    }

    @Test
    void validation_실패시_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/users/me")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void BusinessException_발생시_정의한_ApiResponse_fail_형태로_응답한다() throws Exception {
        when(userService.signup(any(), any(UserSignupRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.USER_ALREADY_EXISTS));

        mockMvc.perform(post("/api/users/me")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("USER_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("이미 회원가입이 완료된 사용자입니다."));
    }

    private String validRequestJson() {
        return """
                {
                  "name": "김철수",
                  "nationality": "중국",
                  "birthYear": 2000,
                  "userStatus": "UNDERGRADUATE",
                  "schoolName": "서울대학교",
                  "entryDate": "2022-03-01",
                  "visaType": "D2",
                  "hasAlienRegistration": true,
                  "stayExpirationDate": "2027-03-01",
                  "housingType": "DORMITORY",
                  "isParentSupported": false,
                  "partTimeStatus": "SEARCHING",
                  "currentTopikLevel": "LEVEL_3",
                  "targetTopikLevel": "LEVEL_5"
                }
                """;
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        UserService userService() {
            return mock(UserService.class);
        }
    }
}
