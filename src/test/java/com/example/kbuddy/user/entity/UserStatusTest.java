package com.example.kbuddy.user.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserStatusTest {

    @Test
    void UserStatus는_정의된_7개_상수를_가진다() {
        assertThat(UserStatus.values())
                .extracting(Enum::name)
                .containsExactly(
                        "BEFORE_ENTRY",
                        "HIGH_SCHOOL",
                        "LANGUAGE_STUDENT",
                        "UNDERGRADUATE",
                        "GRADUATE",
                        "EXCHANGE_STUDENT",
                        "OTHER"
                );
    }
}
