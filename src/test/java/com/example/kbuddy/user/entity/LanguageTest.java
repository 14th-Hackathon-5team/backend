package com.example.kbuddy.user.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LanguageTest {

    @Test
    void Language는_정의된_2개_상수를_가진다() {
        assertThat(Language.values())
                .extracting(Enum::name)
                .containsExactly("KOREAN", "ENGLISH");
    }
}
