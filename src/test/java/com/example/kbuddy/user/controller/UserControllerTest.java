package com.example.kbuddy.user.controller;

import com.example.kbuddy.global.exception.BusinessException;
import com.example.kbuddy.global.exception.ErrorCode;
import com.example.kbuddy.global.exception.GlobalExceptionHandler;
import com.example.kbuddy.user.dto.UserResponse;
import com.example.kbuddy.user.dto.UserSignupRequest;
import com.example.kbuddy.user.dto.UserSignupResponse;
import com.example.kbuddy.user.dto.UserUpdateRequest;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
    void 회원가입_요청에_language가_누락되면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/users/me")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJsonWithoutLanguage()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void 회원가입_요청에_nationality가_소문자이면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/users/me")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJsonWithNationality("cn")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void 회원가입_요청에_nationality가_3자리이면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/users/me")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJsonWithNationality("KOR")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void 회원가입_요청에_nationality가_한글_국가명이면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/users/me")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJsonWithNationality("중국")))
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
                "CN",
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
                TopikLevel.LEVEL_5,
                Language.KOREAN
        );
        when(userService.getMyInfo(1L)).thenReturn(userResponse);

        mockMvc.perform(get("/api/users/me")
                        .with(jwt().jwt(builder -> builder.subject("1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.email").value("test@gmail.com"))
                .andExpect(jsonPath("$.data.name").value("김철수"))
                .andExpect(jsonPath("$.data.language").value("KOREAN"));
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

    @Test
    void PATCH_요청이_정상이면_200과_수정된_UserResponse_데이터를_반환한다() throws Exception {
        UserResponse userResponse = new UserResponse(
                1L,
                "test@gmail.com",
                "김철수",
                "CN",
                2000,
                UserStatus.UNDERGRADUATE,
                "연세대학교",
                LocalDate.of(2022, 3, 1),
                VisaType.D2,
                true,
                LocalDate.of(2027, 3, 1),
                HousingType.DORMITORY,
                false,
                PartTimeStatus.SEARCHING,
                TopikLevel.LEVEL_3,
                TopikLevel.LEVEL_5,
                Language.KOREAN
        );
        when(userService.updateMyInfo(eq(1L), any(UserUpdateRequest.class))).thenReturn(userResponse);

        mockMvc.perform(patch("/api/users/me")
                        .with(jwt().jwt(builder -> builder.subject("1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "schoolName": "연세대학교"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("USER_INFO_UPDATED"))
                .andExpect(jsonPath("$.data.schoolName").value("연세대학교"))
                .andExpect(jsonPath("$.data.language").value("KOREAN"));
    }

    @Test
    void PATCH_요청에서_language만_전달해도_정상_처리된다() throws Exception {
        UserResponse userResponse = new UserResponse(
                1L,
                "test@gmail.com",
                "김철수",
                "CN",
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
                TopikLevel.LEVEL_5,
                Language.ENGLISH
        );
        when(userService.updateMyInfo(eq(1L), any(UserUpdateRequest.class))).thenReturn(userResponse);

        mockMvc.perform(patch("/api/users/me")
                        .with(jwt().jwt(builder -> builder.subject("1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "language": "ENGLISH"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.language").value("ENGLISH"));
    }

    @Test
    void PATCH_요청에서_USER_NOT_FOUND_예외가_발생하면_404를_반환한다() throws Exception {
        when(userService.updateMyInfo(eq(1L), any(UserUpdateRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));

        mockMvc.perform(patch("/api/users/me")
                        .with(jwt().jwt(builder -> builder.subject("1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "schoolName": "연세대학교"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void 빈_PATCH_요청이면_400을_반환한다() throws Exception {
        when(userService.updateMyInfo(eq(1L), any(UserUpdateRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.USER_UPDATE_EMPTY));

        mockMvc.perform(patch("/api/users/me")
                        .with(jwt().jwt(builder -> builder.subject("1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("USER_UPDATE_EMPTY"));
    }

    @Test
    void PATCH_validation_실패시_400을_반환한다() throws Exception {
        mockMvc.perform(patch("/api/users/me")
                        .with(jwt().jwt(builder -> builder.subject("1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void PATCH_요청에서_nationality가_소문자이면_400을_반환한다() throws Exception {
        mockMvc.perform(patch("/api/users/me")
                        .with(jwt().jwt(builder -> builder.subject("1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nationality": "cn"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void DELETE_요청이_정상이면_200을_반환한다() throws Exception {
        mockMvc.perform(delete("/api/users/me")
                        .with(jwt().jwt(builder -> builder.subject("1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("USER_DELETED"));

        verify(userService).deleteMyAccount(1L);
    }

    @Test
    void DELETE_요청에서_USER_NOT_FOUND_예외가_발생하면_404를_반환한다() throws Exception {
        doThrow(new BusinessException(ErrorCode.USER_NOT_FOUND))
                .when(userService).deleteMyAccount(1L);

        mockMvc.perform(delete("/api/users/me")
                        .with(jwt().jwt(builder -> builder.subject("1"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    private String validRequestJson() {
        return """
                {
                  "name": "김철수",
                  "nationality": "CN",
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
                  "targetTopikLevel": "LEVEL_5",
                  "language": "KOREAN"
                }
                """;
    }

    private String requestJsonWithoutLanguage() {
        return """
                {
                  "name": "김철수",
                  "nationality": "CN",
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

    private String requestJsonWithNationality(String nationality) {
        return """
                {
                  "name": "김철수",
                  "nationality": "%s",
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
                  "targetTopikLevel": "LEVEL_5",
                  "language": "KOREAN"
                }
                """.formatted(nationality);
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        UserService userService() {
            return mock(UserService.class);
        }
    }
}
