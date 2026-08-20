package com.example.kbuddy.ai.dto;

import com.example.kbuddy.user.entity.HousingType;
import com.example.kbuddy.user.entity.Language;
import com.example.kbuddy.user.entity.PartTimeStatus;
import com.example.kbuddy.user.entity.TopikLevel;
import com.example.kbuddy.user.entity.UserStatus;
import com.example.kbuddy.user.entity.VisaType;

import java.time.LocalDate;

public record AiUser(
        Long userId,
        String nationality,
        Integer birthYear,
        UserStatus userStatus,
        String schoolName,
        LocalDate entryDate,
        VisaType visaType,
        Boolean hasAlienRegistration,
        LocalDate stayExpirationDate,
        HousingType housingType,
        Boolean isParentSupported,
        PartTimeStatus partTimeStatus,
        LocalDate partTimeStartDate,
        Boolean hasPartTimePermit,
        TopikLevel currentTopikLevel,
        TopikLevel targetTopikLevel,
        String language
) {

    /**
     * 내부 {@link Language} enum을 AI 서버와 합의된 언어 코드로 변환한다.
     * AI 서버는 값이 없거나 알 수 없으면 자체적으로 한국어로 기본 처리하므로,
     * null이 들어오면 그대로 null을 반환해 AI 서버의 기본 처리에 맡긴다.
     */
    public static String languageCode(Language language) {
        if (language == null) {
            return null;
        }
        return switch (language) {
            case KOREAN -> "ko";
            case ENGLISH -> "en";
        };
    }
}
