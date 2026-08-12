package com.example.kbuddy.user.controller;

import com.example.kbuddy.global.exception.BusinessException;
import com.example.kbuddy.global.exception.ErrorCode;
import com.example.kbuddy.global.exception.GlobalExceptionHandler;
import com.example.kbuddy.user.dto.UserResponse;
import com.example.kbuddy.user.dto.UserSignupRequest;
import com.example.kbuddy.user.dto.UserSignupResponse;
import com.example.kbuddy.user.entity.HousingType;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@Import({GlobalExceptionHandler.class, UserControllerTest.TestConfig.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @BeforeEach
    void resetUserServiceMock() {
        Mockito.reset(userService);
    }

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

    @Test
    void GET_요청이_정상이면_200과_UserResponse_데이터를_반환한다() throws Exception {
        UserResponse userResponse = new UserResponse(
                1L,
                "test@gmail.com",
                "김철수",
                "중국",
                2000,
                UserStatus.UNDERGRADUATE,
                "서울대학교",
                LocalDate.of(2022, 3, 1),
                VisaType.D2,
                true,
                LocalDate.of(2027, 3, 1),
                HousingType.DORMITORY,
                false,
                PartTimeStatus.SEARCHING,
                TopikLevel.LEVEL_3,
                TopikLevel.LEVEL_5
        );
        when(userService.getMyInfo(1L)).thenReturn(userResponse);

        mockMvc.perform(get("/api/users/me")
                        .with(jwt().jwt(builder -> builder.subject("1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.email").value("test@gmail.com"))
                .andExpect(jsonPath("$.data.name").value("김철수"));
    }

    @Test
    void GET_요청에서_USER_NOT_FOUND_예외가_발생하면_404를_반환한다() throws Exception {
        when(userService.getMyInfo(1L)).thenThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));

        mockMvc.perform(get("/api/users/me")
                        .with(jwt().jwt(builder -> builder.subject("1"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
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
