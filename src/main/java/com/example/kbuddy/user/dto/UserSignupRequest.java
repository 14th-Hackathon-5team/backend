package com.example.kbuddy.user.dto;

import com.example.kbuddy.user.entity.HousingType;
import com.example.kbuddy.user.entity.Language;
import com.example.kbuddy.user.entity.PartTimeStatus;
import com.example.kbuddy.user.entity.TopikLevel;
import com.example.kbuddy.user.entity.UserStatus;
import com.example.kbuddy.user.entity.VisaType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "회원가입 요청. SIGNUP_TOKEN에 담긴 provider/providerId/email 외의 프로필 정보를 입력합니다.")
public record UserSignupRequest(

        @Schema(description = "사용자 이름", example = "김철수", maxLength = 100, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 100)
        String name,

        @Schema(description = "국적 (ISO 3166-1 alpha-2 국가 코드)", example = "CN", pattern = "^[A-Z]{2}$", maxLength = 100, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = "^[A-Z]{2}$")
        String nationality,

        @Schema(description = "출생 연도", example = "2000", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Integer birthYear,

        @Schema(description = "사용자 상태", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        UserStatus userStatus,

        @Schema(description = "학교 이름 (선택)", example = "서울대학교")
        String schoolName,

        @Schema(description = "한국 입국일", example = "2022-03-01", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        LocalDate entryDate,

        @Schema(description = "비자 종류", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        VisaType visaType,

        @Schema(description = "외국인등록증 소지 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Boolean hasAlienRegistration,

        @Schema(description = "체류 만료일 (선택)", example = "2027-03-01")
        LocalDate stayExpirationDate,

        @Schema(description = "주거 형태 (선택)")
        HousingType housingType,

        @Schema(description = "부모님의 경제적 지원 여부 (선택)", example = "false")
        Boolean isParentSupported,

        @Schema(description = "아르바이트 현황 (선택)")
        PartTimeStatus partTimeStatus,

        @Schema(description = "현재 TOPIK 등급", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        TopikLevel currentTopikLevel,

        @Schema(description = "목표 TOPIK 등급", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        TopikLevel targetTopikLevel,

        @Schema(description = "사용 언어", example = "ENGLISH", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Language language
) {
}
