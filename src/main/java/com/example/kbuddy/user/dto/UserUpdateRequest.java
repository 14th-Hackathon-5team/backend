package com.example.kbuddy.user.dto;

import com.example.kbuddy.user.entity.HousingType;
import com.example.kbuddy.user.entity.PartTimeStatus;
import com.example.kbuddy.user.entity.TopikLevel;
import com.example.kbuddy.user.entity.UserStatus;
import com.example.kbuddy.user.entity.VisaType;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UserUpdateRequest(

        @Size(min = 1, max = 100)
        String name,

        @Size(min = 1, max = 100)
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

        TopikLevel currentTopikLevel,

        TopikLevel targetTopikLevel
) {
}
