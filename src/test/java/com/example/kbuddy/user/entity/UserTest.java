package com.example.kbuddy.user.entity;

import com.example.kbuddy.auth.oauth.AuthProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    private User createUser() {
        return new User(
                AuthProvider.KAKAO,
                "kakao-1234",
                "student@example.com",
                "홍길동",
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
    }

    @Test
    void 생성자로_User를_생성하면_전달한_값이_각_필드에_정상적으로_저장된다() {
        User user = createUser();

        assertThat(user.getProvider()).isEqualTo(AuthProvider.KAKAO);
        assertThat(user.getProviderId()).isEqualTo("kakao-1234");
        assertThat(user.getEmail()).isEqualTo("student@example.com");
        assertThat(user.getName()).isEqualTo("홍길동");
        assertThat(user.getNationality()).isEqualTo("CN");
        assertThat(user.getBirthYear()).isEqualTo(2000);
        assertThat(user.getUserStatus()).isEqualTo(UserStatus.UNDERGRADUATE);
        assertThat(user.getSchoolName()).isEqualTo("서울대학교");
        assertThat(user.getEntryDate()).isEqualTo(LocalDate.of(2022, 3, 1));
        assertThat(user.getVisaType()).isEqualTo(VisaType.D2);
        assertThat(user.getHasAlienRegistration()).isTrue();
        assertThat(user.getStayExpirationDate()).isEqualTo(LocalDate.of(2027, 3, 1));
        assertThat(user.getHousingType()).isEqualTo(HousingType.DORMITORY);
        assertThat(user.getIsParentSupported()).isFalse();
        assertThat(user.getPartTimeStatus()).isEqualTo(PartTimeStatus.SEARCHING);
        assertThat(user.getCurrentTopikLevel()).isEqualTo(TopikLevel.LEVEL_3);
        assertThat(user.getTargetTopikLevel()).isEqualTo(TopikLevel.LEVEL_5);
        assertThat(user.getLanguage()).isEqualTo(Language.KOREAN);
    }

    @Test
    void 조건부_nullable_필드는_null을_전달해도_예외_없이_생성된다() {
        User user = new User(
                AuthProvider.GOOGLE,
                "google-1",
                "before@example.com",
                "김철수",
                "VN",
                2003,
                UserStatus.BEFORE_ENTRY,
                null,
                LocalDate.of(2026, 9, 1),
                VisaType.D2,
                false,
                null,
                null,
                null,
                null,
                TopikLevel.NONE,
                TopikLevel.LEVEL_3,
                Language.ENGLISH
        );

        assertThat(user.getSchoolName()).isNull();
        assertThat(user.getStayExpirationDate()).isNull();
        assertThat(user.getHousingType()).isNull();
        assertThat(user.getIsParentSupported()).isNull();
        assertThat(user.getPartTimeStatus()).isNull();
    }

    @Test
    void 기본_생성자로_생성하면_필드가_모두_초기값인_상태로_생성된다() {
        User user = new User();

        assertThat(user.getId()).isNull();
        assertThat(user.getProvider()).isNull();
        assertThat(user.getCreatedAt()).isNull();
        assertThat(user.getUpdatedAt()).isNull();
    }

    @Test
    void User_클래스는_Entity이며_테이블명은_users이고_provider_providerId_복합_unique_제약을_가진다() {
        Entity entityAnnotation = User.class.getAnnotation(Entity.class);
        Table tableAnnotation = User.class.getAnnotation(Table.class);

        assertThat(entityAnnotation).isNotNull();
        assertThat(tableAnnotation).isNotNull();
        assertThat(tableAnnotation.name()).isEqualTo("users");

        UniqueConstraint[] uniqueConstraints = tableAnnotation.uniqueConstraints();
        assertThat(uniqueConstraints).hasSize(1);
        assertThat(uniqueConstraints[0].columnNames()).containsExactly("provider", "provider_id");
    }

    @Test
    void id_필드는_IDENTITY_전략의_기본키다() throws NoSuchFieldException {
        Field idField = User.class.getDeclaredField("id");

        assertThat(idField.getAnnotation(Id.class)).isNotNull();
        GeneratedValue generatedValue = idField.getAnnotation(GeneratedValue.class);
        assertThat(generatedValue).isNotNull();
        assertThat(generatedValue.strategy()).isEqualTo(GenerationType.IDENTITY);
    }

    @Test
    void email_필드는_unique_컬럼으로_매핑된다() throws NoSuchFieldException {
        Column column = User.class.getDeclaredField("email").getAnnotation(Column.class);

        assertThat(column.name()).isEqualTo("email");
        assertThat(column.unique()).isTrue();
        assertThat(column.nullable()).isFalse();
    }

    @Test
    void Enum_필드는_모두_EnumType_STRING으로_매핑된다() throws NoSuchFieldException {
        String[] enumFieldNames = {
                "provider", "userStatus", "visaType", "housingType",
                "partTimeStatus", "currentTopikLevel", "targetTopikLevel", "language"
        };

        for (String fieldName : enumFieldNames) {
            Field field = User.class.getDeclaredField(fieldName);
            Enumerated enumerated = field.getAnnotation(Enumerated.class);

            assertThat(enumerated)
                    .as("%s 필드에는 @Enumerated가 존재해야 한다", fieldName)
                    .isNotNull();
            assertThat(enumerated.value())
                    .as("%s 필드는 EnumType.STRING이어야 한다", fieldName)
                    .isEqualTo(EnumType.STRING);
        }
    }

    @Test
    void 조건부_nullable_필드에_해당하는_컬럼은_nullable_true로_매핑된다() throws NoSuchFieldException {
        String[] nullableFieldNames = {
                "schoolName", "stayExpirationDate", "housingType", "isParentSupported", "partTimeStatus"
        };

        for (String fieldName : nullableFieldNames) {
            Column column = User.class.getDeclaredField(fieldName).getAnnotation(Column.class);

            assertThat(column)
                    .as("%s 필드에는 @Column이 존재해야 한다", fieldName)
                    .isNotNull();
            assertThat(column.nullable())
                    .as("%s 컬럼은 조건부 nullable이므로 nullable=true여야 한다", fieldName)
                    .isTrue();
        }
    }

    @Test
    void DB_컬럼명이_필드명과_다른_주요_필드는_Column_name이_명시적으로_매핑된다() throws NoSuchFieldException {
        assertThat(User.class.getDeclaredField("providerId").getAnnotation(Column.class).name())
                .isEqualTo("provider_id");
        assertThat(User.class.getDeclaredField("birthYear").getAnnotation(Column.class).name())
                .isEqualTo("birth_year");
        assertThat(User.class.getDeclaredField("userStatus").getAnnotation(Column.class).name())
                .isEqualTo("user_status");
        assertThat(User.class.getDeclaredField("schoolName").getAnnotation(Column.class).name())
                .isEqualTo("school_name");
        assertThat(User.class.getDeclaredField("entryDate").getAnnotation(Column.class).name())
                .isEqualTo("entry_date");
        assertThat(User.class.getDeclaredField("visaType").getAnnotation(Column.class).name())
                .isEqualTo("visa_type");
        assertThat(User.class.getDeclaredField("hasAlienRegistration").getAnnotation(Column.class).name())
                .isEqualTo("has_alien_registration");
        assertThat(User.class.getDeclaredField("stayExpirationDate").getAnnotation(Column.class).name())
                .isEqualTo("stay_expiration_date");
        assertThat(User.class.getDeclaredField("housingType").getAnnotation(Column.class).name())
                .isEqualTo("housing_type");
        assertThat(User.class.getDeclaredField("isParentSupported").getAnnotation(Column.class).name())
                .isEqualTo("is_parent_supported");
        assertThat(User.class.getDeclaredField("partTimeStatus").getAnnotation(Column.class).name())
                .isEqualTo("part_time_status");
        assertThat(User.class.getDeclaredField("currentTopikLevel").getAnnotation(Column.class).name())
                .isEqualTo("current_topik_level");
        assertThat(User.class.getDeclaredField("targetTopikLevel").getAnnotation(Column.class).name())
                .isEqualTo("target_topik_level");
        assertThat(User.class.getDeclaredField("createdAt").getAnnotation(Column.class).name())
                .isEqualTo("created_at");
        assertThat(User.class.getDeclaredField("updatedAt").getAnnotation(Column.class).name())
                .isEqualTo("updated_at");
    }
}
