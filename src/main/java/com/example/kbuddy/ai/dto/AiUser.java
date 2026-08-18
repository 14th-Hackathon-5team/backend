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
        Language language
) {
}
