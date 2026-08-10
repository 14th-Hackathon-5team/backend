package com.example.kbuddy.user.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PartTimeStatusTest {

    @Test
    void PartTimeStatus는_정의된_3개_상수를_가진다() {
        assertThat(PartTimeStatus.values())
                .extracting(Enum::name)
                .containsExactly(
                        "WORKING",
                        "SEARCHING",
                        "NOT_PLANNED"
                );
    }
}
