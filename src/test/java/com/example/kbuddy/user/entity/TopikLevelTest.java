package com.example.kbuddy.user.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TopikLevelTest {

    @Test
    void TopikLevel은_정의된_7개_상수를_가진다() {
        assertThat(TopikLevel.values())
                .extracting(Enum::name)
                .containsExactly(
                        "NONE",
                        "LEVEL_1",
                        "LEVEL_2",
                        "LEVEL_3",
                        "LEVEL_4",
                        "LEVEL_5",
                        "LEVEL_6"
                );
    }
}
