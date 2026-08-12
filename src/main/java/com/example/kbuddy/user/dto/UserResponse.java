package com.example.kbuddy.user.dto;

import com.example.kbuddy.user.entity.HousingType;
import com.example.kbuddy.user.entity.PartTimeStatus;
import com.example.kbuddy.user.entity.TopikLevel;
import com.example.kbuddy.user.entity.UserStatus;
import com.example.kbuddy.user.entity.VisaType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "사용자 프로필 정보 응답")
public record UserResponse(
        @Schema(description = "사용자 ID", example = "1")
        Long id,

        @Schema(description = "이메일", example = "test@gmail.com")
        String email,

        @Schema(description = "사용자 이름", example = "김철수")
        String name,

        @Schema(description = "국적", example = "중국")
        String nationality,

        @Schema(description = "출생 연도", example = "2000")
        Integer birthYear,

        @Schema(description = "사용자 상태")
        UserStatus userStatus,

        @Schema(description = "학교 이름", example = "서울대학교")
        String schoolName,

        @Schema(description = "한국 입국일", example = "2022-03-01")
        LocalDate entryDate,

        @Schema(description = "비자 종류")
        VisaType visaType,

        @Schema(description = "외국인등록증 소지 여부", example = "true")
        Boolean hasAlienRegistration,

        @Schema(description = "체류 만료일", example = "2027-03-01")
        LocalDate stayExpirationDate,

        @Schema(description = "주거 형태")
        HousingType housingType,

        @Schema(description = "부모님의 경제적 지원 여부", example = "false")
        Boolean isParentSupported,

        @Schema(description = "아르바이트 현황")
        PartTimeStatus partTimeStatus,

        @Schema(description = "현재 TOPIK 등급")
        TopikLevel currentTopikLevel,

        @Schema(description = "목표 TOPIK 등급")
        TopikLevel targetTopikLevel
) {
}
