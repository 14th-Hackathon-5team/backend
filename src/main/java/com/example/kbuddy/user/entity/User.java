package com.example.kbuddy.user.entity;

import com.example.kbuddy.auth.oauth.AuthProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_provider_provider_id", columnNames = {"provider", "provider_id"})
        }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", length = 20, nullable = false)
    private AuthProvider provider;

    @Column(name = "provider_id", length = 100, nullable = false)
    private String providerId;

    @Column(name = "email", length = 255, nullable = false, unique = true)
    private String email;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "nationality", length = 100, nullable = false)
    private String nationality;

    @Column(name = "birth_year", columnDefinition = "YEAR", nullable = false)
    private Integer birthYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", length = 30, nullable = false)
    private UserStatus userStatus;

    @Column(name = "school_name", length = 100)
    private String schoolName;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "visa_type", length = 20, nullable = false)
    private VisaType visaType;

    @Column(name = "has_alien_registration", nullable = false)
    private Boolean hasAlienRegistration;

    @Column(name = "stay_expiration_date")
    private LocalDate stayExpirationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "housing_type", length = 20)
    private HousingType housingType;

    @Column(name = "is_parent_supported")
    private Boolean isParentSupported;

    @Enumerated(EnumType.STRING)
    @Column(name = "part_time_status", length = 20)
    private PartTimeStatus partTimeStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_topik_level", length = 20, nullable = false)
    private TopikLevel currentTopikLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_topik_level", length = 20, nullable = false)
    private TopikLevel targetTopikLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", length = 20, nullable = false)
    private Language language;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_language", length = 20, nullable = false)
    private AppLanguage preferredLanguage;

    @Enumerated(EnumType.STRING)
    @Column(name = "alarm_setting", length = 20, nullable = false)
    private AlarmSetting alarmSetting;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_state", length = 20, nullable = false)
    private AccountState accountState;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public User(
            AuthProvider provider,
            String providerId,
            String email,
            String name,
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
            TopikLevel targetTopikLevel,
            Language language
    ) {
        this.provider = provider;
        this.providerId = providerId;
        this.email = email;
        this.name = name;
        this.nationality = nationality;
        this.birthYear = birthYear;
        this.userStatus = userStatus;
        this.schoolName = schoolName;
        this.entryDate = entryDate;
        this.visaType = visaType;
        this.hasAlienRegistration = hasAlienRegistration;
        this.stayExpirationDate = stayExpirationDate;
        this.housingType = housingType;
        this.isParentSupported = isParentSupported;
        this.partTimeStatus = partTimeStatus;
        this.currentTopikLevel = currentTopikLevel;
        this.targetTopikLevel = targetTopikLevel;
        this.language = language;
        this.preferredLanguage = AppLanguage.valueOf(language.name());
        this.alarmSetting = AlarmSetting.ALL;
        this.accountState = AccountState.ACTIVE;
    }

    public void updateProfile(
            String name,
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
            TopikLevel targetTopikLevel,
            Language language
    ) {
        if (name != null) {
            this.name = name;
        }
        if (nationality != null) {
            this.nationality = nationality;
        }
        if (birthYear != null) {
            this.birthYear = birthYear;
        }
        if (userStatus != null) {
            this.userStatus = userStatus;
        }
        if (schoolName != null) {
            this.schoolName = schoolName;
        }
        if (entryDate != null) {
            this.entryDate = entryDate;
        }
        if (visaType != null) {
            this.visaType = visaType;
        }
        if (hasAlienRegistration != null) {
            this.hasAlienRegistration = hasAlienRegistration;
        }
        if (stayExpirationDate != null) {
            this.stayExpirationDate = stayExpirationDate;
        }
        if (housingType != null) {
            this.housingType = housingType;
        }
        if (isParentSupported != null) {
            this.isParentSupported = isParentSupported;
        }
        if (partTimeStatus != null) {
            this.partTimeStatus = partTimeStatus;
        }
        if (currentTopikLevel != null) {
            this.currentTopikLevel = currentTopikLevel;
        }
        if (targetTopikLevel != null) {
            this.targetTopikLevel = targetTopikLevel;
        }
        if (language != null) {
            this.language = language;
            this.preferredLanguage = AppLanguage.valueOf(language.name());
        }
    }

    public void updatePreferredLanguage(AppLanguage preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
        this.language = Language.valueOf(preferredLanguage.name());
    }

    public void updateAlarmSetting(AlarmSetting alarmSetting) {
        this.alarmSetting = alarmSetting;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.preferredLanguage == null) {
            this.preferredLanguage = AppLanguage.valueOf(this.language.name());
        }
        if (this.alarmSetting == null) {
            this.alarmSetting = AlarmSetting.ALL;
        }
        if (this.accountState == null) {
            this.accountState = AccountState.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
