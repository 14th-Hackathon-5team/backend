package com.example.kbuddy.user.dto;

import com.example.kbuddy.user.entity.HousingType;
import com.example.kbuddy.user.entity.PartTimeStatus;
import com.example.kbuddy.user.entity.TopikLevel;
import com.example.kbuddy.user.entity.UserStatus;
import com.example.kbuddy.user.entity.VisaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UserSignupRequest(

        @NotBlank
        @Size(max = 100)
        String name,

        @NotBlank
        @Size(max = 100)
        String nationality,

        @NotNull
        Integer birthYear,

        @NotNull
        UserStatus userStatus,

        String schoolName,

        @NotNull
        LocalDate entryDate,

        @NotNull
        VisaType visaType,

        @NotNull
        Boolean hasAlienRegistration,

        LocalDate stayExpirationDate,

        HousingType housingType,

        Boolean isParentSupported,

        PartTimeStatus partTimeStatus,

        @NotNull
        TopikLevel currentTopikLevel,

        @NotNull
        TopikLevel targetTopikLevel
) {
}
