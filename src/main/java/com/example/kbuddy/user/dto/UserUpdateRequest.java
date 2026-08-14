package com.example.kbuddy.user.dto;

import com.example.kbuddy.user.entity.HousingType;
import com.example.kbuddy.user.entity.Language;
import com.example.kbuddy.user.entity.PartTimeStatus;
import com.example.kbuddy.user.entity.TopikLevel;
import com.example.kbuddy.user.entity.UserStatus;
import com.example.kbuddy.user.entity.VisaType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(
        description = "내 정보 부분 수정 요청. 모든 필드는 선택 사항이며, 값을 전달한 필드만 수정됩니다. "
                + "요청에 포함하지 않거나 값이 null인 필드는 기존 값이 그대로 유지됩니다. "
                + "id, provider, providerId, email, createdAt, updatedAt은 이 요청으로 수정할 수 없으며 이 DTO에 존재하지도 않습니다.",
        example = """
                {
                  "name": "John",
                  "schoolName": "Kyung Hee University",
                  "currentTopikLevel": "LEVEL_3"
                }
                """
)
public record UserUpdateRequest(

        @Schema(description = "사용자 이름 (선택, 값을 보내면 빈 문자열 불가)", example = "John", maxLength = 100)
        @Size(min = 1, max = 100)
        String name,

        @Schema(description = "국적 (선택, ISO 3166-1 alpha-2 국가 코드)", example = "VN", pattern = "^[A-Z]{2}$", maxLength = 100)
        @Size(min = 1, max = 100)
        @Pattern(regexp = "^[A-Z]{2}$")
        String nationality,

        @Schema(description = "출생 연도 (선택)", example = "2000")
        Integer birthYear,

        @Schema(description = "사용자 상태 (선택)")
        UserStatus userStatus,

        @Schema(description = "학교 이름 (선택)", example = "Kyung Hee University")
        String schoolName,

        @Schema(description = "한국 입국일 (선택)", example = "2022-03-01")
        LocalDate entryDate,

        @Schema(description = "비자 종류 (선택)")
        VisaType visaType,

        @Schema(description = "외국인등록증 소지 여부 (선택)", example = "true")
        Boolean hasAlienRegistration,

        @Schema(description = "체류 만료일 (선택)", example = "2027-03-01")
        LocalDate stayExpirationDate,

        @Schema(description = "주거 형태 (선택)")
        HousingType housingType,

        @Schema(description = "부모님의 경제적 지원 여부 (선택)", example = "false")
        Boolean isParentSupported,

        @Schema(description = "아르바이트 현황 (선택)")
        PartTimeStatus partTimeStatus,

        @Schema(description = "현재 TOPIK 등급 (선택)", example = "LEVEL_3")
        TopikLevel currentTopikLevel,

        @Schema(description = "목표 TOPIK 등급 (선택)")
        TopikLevel targetTopikLevel,

        @Schema(description = "사용 언어 (선택)", example = "ENGLISH")
        Language language
) {
}
